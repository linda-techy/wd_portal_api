package com.wd.api.service;

import com.wd.api.dto.LabourPaymentDTO;
import com.wd.api.dto.ProjectInvoiceDTO;
import com.wd.api.dto.PurchaseInvoiceDTO;
import com.wd.api.model.*;
import com.wd.api.model.enums.InvoiceStatus;
import com.wd.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Thin facade that delegates to focused sub-services:
 * - {@link ProjectInvoiceService} — invoice generation and queries
 * - {@link LabourPaymentService}  — labour wage processing and wage sheet validation
 *
 * This class retains purchase invoices, milestone billing, receipt recording,
 * and payment reconciliation.
 */
@Service
@RequiredArgsConstructor
public class FinanceService {

        private final ProjectInvoiceService projectInvoiceService;
        private final LabourPaymentService labourPaymentService;

        private final ProjectInvoiceRepository projectInvoiceRepository;
        private final PurchaseInvoiceRepository purchaseInvoiceRepository;
        private final CustomerProjectRepository projectRepository;
        private final VendorRepository vendorRepository;
        private final PurchaseOrderRepository poRepository;
        private final GoodsReceivedNoteRepository grnRepository;
        private final ProjectMilestoneRepository milestoneRepository;
        private final ReceiptRepository receiptRepository;
        private final PaymentTransactionRepository paymentTransactionRepository;
        private final CustomerNotificationFacade customerNotificationFacade;

        // ── Delegated: Project Invoices ───────────────────────────────────────────

        @Transactional
        public ProjectInvoiceDTO createProjectInvoice(ProjectInvoiceDTO dto) {
                return projectInvoiceService.createProjectInvoice(dto);
        }

        public List<ProjectInvoiceDTO> getInvoicesByProject(Long projectId) {
                return projectInvoiceService.getInvoicesByProject(projectId);
        }

        @Transactional
        public ProjectInvoiceDTO generateInvoiceForMilestone(Long milestoneId) {
                return projectInvoiceService.generateInvoiceForMilestone(milestoneId);
        }

        // ── Delegated: Labour Payments ────────────────────────────────────────────

        @Transactional
        public LabourPaymentDTO recordLabourPayment(LabourPaymentDTO dto) {
                return labourPaymentService.recordLabourPayment(dto);
        }

        // ── Purchase Invoices ─────────────────────────────────────────────────────

        @Transactional
        public PurchaseInvoiceDTO recordPurchaseInvoice(PurchaseInvoiceDTO dto) {
                Long vendorId = java.util.Objects.requireNonNull(dto.getVendorId());
                Vendor vendor = vendorRepository.findById(vendorId)
                                .orElseThrow(() -> new RuntimeException("Vendor not found"));
                Long projectId = java.util.Objects.requireNonNull(dto.getProjectId());
                CustomerProject project = projectRepository.findById(projectId)
                                .orElseThrow(() -> new RuntimeException("Project not found"));

                PurchaseOrder po = null;
                if (dto.getPoId() != null) {
                        po = poRepository.findById(dto.getPoId()).orElse(null);
                }

                GoodsReceivedNote grn = null;
                if (dto.getGrnId() != null) {
                        grn = grnRepository.findById(dto.getGrnId()).orElse(null);
                }

                PurchaseInvoice invoice = PurchaseInvoice.builder()
                                .vendor(vendor)
                                .project(project)
                                .purchaseOrder(po)
                                .grn(grn)
                                .vendorInvoiceNumber(dto.getVendorInvoiceNumber())
                                .invoiceDate(dto.getInvoiceDate())
                                .amount(dto.getAmount())
                                .status("PENDING")
                                .build();

                return mapToPurchaseInvoiceDTO(purchaseInvoiceRepository.save(invoice));
        }

        // ── Milestone Billing ─────────────────────────────────────────────────────

        @Transactional
        public ProjectMilestone createMilestone(ProjectMilestone milestone) {
                return milestoneRepository.save(milestone);
        }

        @Transactional
        public ProjectMilestone updateMilestone(Long id, ProjectMilestone details) {
                ProjectMilestone milestone = milestoneRepository.findById(java.util.Objects.requireNonNull(id))
                                .orElseThrow(() -> new RuntimeException("Milestone not found"));

                milestone.setName(details.getName());
                milestone.setDescription(details.getDescription());
                milestone.setMilestonePercentage(details.getMilestonePercentage());
                milestone.setAmount(details.getAmount());
                milestone.setDueDate(details.getDueDate());

                boolean wasCompleted = "COMPLETED".equals(milestone.getStatus());
                if (details.getStatus() != null) {
                        milestone.setStatus(details.getStatus());
                        if ("COMPLETED".equals(details.getStatus()) && milestone.getCompletedDate() == null) {
                                milestone.setCompletedDate(java.time.LocalDate.now());
                        }
                }

                ProjectMilestone saved = milestoneRepository.save(milestone);

                if (!wasCompleted && "COMPLETED".equals(saved.getStatus()) && saved.getProject() != null) {
                        String milestoneName = saved.getName() != null ? saved.getName() : "A milestone";
                        customerNotificationFacade.notifyAll(
                                saved.getProject().getId(),
                                "Milestone Completed",
                                milestoneName + " has been completed.",
                                "MILESTONE",
                                saved.getId()
                        );
                }

                return saved;
        }

        public List<ProjectMilestone> getMilestonesByProject(Long projectId) {
                return milestoneRepository.findByProjectId(projectId);
        }

        // ── Receipt Recording ─────────────────────────────────────────────────────

        @Transactional
        public Receipt recordReceipt(Receipt receipt) {
                if (receipt.getProject() == null && receipt.getInvoice() != null) {
                        receipt.setProject(receipt.getInvoice().getProject());
                }

                if (receipt.getProject() == null) {
                        throw new RuntimeException("Project is required for receipt");
                }

                if (receipt.getInvoice() != null) {
                        BigDecimal invoiceTotal = receipt.getInvoice().getTotalAmount();
                        BigDecimal priorReceipts = receiptRepository.sumAmountByInvoiceId(receipt.getInvoice().getId());
                        if (priorReceipts == null) priorReceipts = BigDecimal.ZERO;
                        BigDecimal newTotal = priorReceipts.add(receipt.getAmount());
                        if (newTotal.compareTo(invoiceTotal) > 0) {
                                throw new IllegalArgumentException(
                                                "Total receipts (" + newTotal + ") would exceed invoice amount (" + invoiceTotal + ")");
                        }
                }

                return receiptRepository.save(receipt);
        }

        public List<Receipt> getReceiptsByProject(Long projectId) {
                return receiptRepository.findByProjectId(projectId);
        }

        // ── Payment Reconciliation ────────────────────────────────────────────────

        @Transactional
        public ProjectInvoiceDTO reconcilePayment(Long paymentTransactionId, Long projectInvoiceId) {
                PaymentTransaction transaction = paymentTransactionRepository.findById(paymentTransactionId)
                                .orElseThrow(() -> new RuntimeException("Payment transaction not found: " + paymentTransactionId));

                ProjectInvoice invoice = projectInvoiceRepository.findById(projectInvoiceId)
                                .orElseThrow(() -> new RuntimeException("Invoice not found: " + projectInvoiceId));

                if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
                        throw new IllegalStateException("Cannot reconcile payment against a CANCELLED invoice");
                }
                if (invoice.getStatus() == InvoiceStatus.PAID) {
                        throw new IllegalStateException("Invoice is already PAID");
                }

                transaction.setProjectInvoice(invoice);
                paymentTransactionRepository.save(transaction);

                BigDecimal totalPaid = paymentTransactionRepository.sumAmountByInvoiceId(projectInvoiceId);
                if (totalPaid.compareTo(invoice.getTotalAmount()) >= 0) {
                        invoice.setStatus(InvoiceStatus.PAID);
                        projectInvoiceRepository.save(invoice);
                }

                return projectInvoiceService.mapToDTO(invoice);
        }

        // ── Mappers ───────────────────────────────────────────────────────────────

        private PurchaseInvoiceDTO mapToPurchaseInvoiceDTO(PurchaseInvoice inv) {
                return PurchaseInvoiceDTO.builder()
                                .id(inv.getId())
                                .vendorId(inv.getVendor().getId())
                                .vendorName(inv.getVendor().getName())
                                .projectId(inv.getProject().getId())
                                .projectName(inv.getProject().getName())
                                .poId(inv.getPurchaseOrder() != null ? inv.getPurchaseOrder().getId() : null)
                                .grnId(inv.getGrn() != null ? inv.getGrn().getId() : null)
                                .vendorInvoiceNumber(inv.getVendorInvoiceNumber())
                                .invoiceDate(inv.getInvoiceDate())
                                .amount(inv.getAmount())
                                .status(inv.getStatus())
                                .build();
        }
}
