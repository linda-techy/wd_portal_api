package com.wd.api.service;

import com.wd.api.dto.ProjectInvoiceDTO;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.ProjectInvoice;
import com.wd.api.model.ProjectMilestone;
import com.wd.api.model.enums.InvoiceStatus;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.ProjectInvoiceRepository;
import com.wd.api.repository.ProjectMilestoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles project invoice generation and queries (split from FinanceService god class).
 */
@Service
@RequiredArgsConstructor
public class ProjectInvoiceService {

    private final ProjectInvoiceRepository projectInvoiceRepository;
    private final CustomerProjectRepository projectRepository;
    private final ProjectMilestoneRepository milestoneRepository;

    // ── Invoice generation ────────────────────────────────────────────────────

    @Transactional
    public ProjectInvoiceDTO createProjectInvoice(ProjectInvoiceDTO dto) {
        Long projectId = java.util.Objects.requireNonNull(dto.getProjectId());
        CustomerProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        BigDecimal gstRate = dto.getGstPercentage() != null ? dto.getGstPercentage() : new BigDecimal("18.00");
        BigDecimal gstAmount = dto.getSubTotal().multiply(gstRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = dto.getSubTotal().add(gstAmount);

        ProjectInvoice invoice = ProjectInvoice.builder()
                .project(project)
                .invoiceNumber(nextInvoiceNumber(project.getCode()))
                .invoiceDate(dto.getInvoiceDate() != null ? dto.getInvoiceDate() : java.time.LocalDate.now())
                .dueDate(dto.getDueDate())
                .subTotal(dto.getSubTotal())
                .gstPercentage(gstRate)
                .gstAmount(gstAmount)
                .totalAmount(totalAmount)
                .status(InvoiceStatus.ISSUED)
                .notes(dto.getNotes())
                .build();

        return mapToDTO(projectInvoiceRepository.save(invoice));
    }

    @Transactional
    public ProjectInvoiceDTO generateInvoiceForMilestone(Long milestoneId) {
        ProjectMilestone milestone = milestoneRepository.findById(java.util.Objects.requireNonNull(milestoneId))
                .orElseThrow(() -> new RuntimeException("Milestone not found"));

        if (!"COMPLETED".equals(milestone.getStatus())) {
            throw new IllegalStateException("Invoice can only be generated for a COMPLETED milestone");
        }
        if (milestone.getInvoice() != null) {
            throw new IllegalStateException("Invoice already exists for this milestone");
        }

        CustomerProject project = milestone.getProject();
        BigDecimal amount = milestone.getAmount();
        BigDecimal gstRate = new BigDecimal("18.00");
        BigDecimal gstAmount = amount.multiply(gstRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = amount.add(gstAmount);

        ProjectInvoice invoice = ProjectInvoice.builder()
                .project(project)
                .invoiceNumber(nextInvoiceNumber(project.getCode()))
                .invoiceDate(java.time.LocalDate.now())
                .dueDate(java.time.LocalDate.now().plusDays(15))
                .subTotal(amount)
                .gstPercentage(gstRate)
                .gstAmount(gstAmount)
                .totalAmount(totalAmount)
                .status(InvoiceStatus.ISSUED)
                .notes("Invoice for milestone: " + milestone.getName())
                .build();

        ProjectInvoice savedInvoice = projectInvoiceRepository.save(invoice);

        milestone.setInvoice(savedInvoice);
        milestone.setStatus("INVOICED");
        milestoneRepository.save(milestone);

        return mapToDTO(savedInvoice);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public List<ProjectInvoiceDTO> getInvoicesByProject(Long projectId) {
        return projectInvoiceRepository.findByProjectId(projectId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private String nextInvoiceNumber(String projectCode) {
        Long seq = projectInvoiceRepository.getNextInvoiceNumber();
        String code = (projectCode != null && !projectCode.isBlank()) ? projectCode : "GEN";
        return "INV-" + code + "-" + seq;
    }

    public ProjectInvoiceDTO mapToDTO(ProjectInvoice inv) {
        return ProjectInvoiceDTO.builder()
                .id(inv.getId())
                .projectId(inv.getProject().getId())
                .projectName(inv.getProject().getName())
                .invoiceNumber(inv.getInvoiceNumber())
                .invoiceDate(inv.getInvoiceDate())
                .dueDate(inv.getDueDate())
                .subTotal(inv.getSubTotal())
                .gstPercentage(inv.getGstPercentage())
                .gstAmount(inv.getGstAmount())
                .totalAmount(inv.getTotalAmount())
                .status(inv.getStatus() != null ? inv.getStatus().name() : null)
                .notes(inv.getNotes())
                .build();
    }
}
