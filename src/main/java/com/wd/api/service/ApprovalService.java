package com.wd.api.service;

import com.wd.api.dto.ApprovalRequestDTO;
import com.wd.api.model.ApprovalRequest;
import com.wd.api.model.PortalUser;
import com.wd.api.repository.ApprovalRequestRepository;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.repository.PurchaseOrderRepository;
import com.wd.api.repository.ProjectInvoiceRepository;
import com.wd.api.model.PurchaseOrder;
import com.wd.api.model.ProjectInvoice;
import com.wd.api.model.PaymentChallan;
import com.wd.api.repository.PaymentChallanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApprovalService {

        private final ApprovalRequestRepository approvalRepository;
        private final PortalUserRepository userRepository;
        private final PurchaseOrderRepository poRepository;
        private final ProjectInvoiceRepository invoiceRepository;
        private final PaymentChallanRepository challanRepository;

        @Transactional
        public ApprovalRequestDTO createRequest(ApprovalRequestDTO dto) {
                PortalUser requester = userRepository.findById(dto.getRequestedById())
                                .orElseThrow(() -> new RuntimeException("Requester not found"));

                PortalUser approver = dto.getApproverId() != null
                                ? userRepository.findById(dto.getApproverId()).orElse(null)
                                : null;

                ApprovalRequest request = ApprovalRequest.builder()
                                .targetType(dto.getTargetType())
                                .targetId(dto.getTargetId())
                                .requestedBy(requester)
                                .approver(approver)
                                .status("PENDING")
                                .comments(dto.getComments())
                                .build();

                request = approvalRepository.save(request);
                return mapToDTO(request);
        }

        @Transactional
        public ApprovalRequestDTO processRequest(Long requestId, String status, String comments, Long approverId) {
                ApprovalRequest request = approvalRepository.findById(requestId)
                                .orElseThrow(() -> new RuntimeException("Request not found"));

                PortalUser approver = userRepository.findById(approverId)
                                .orElseThrow(() -> new RuntimeException("Approver not found"));

                request.setStatus(status);
                request.setComments(comments);
                request.setApprover(approver);
                request.setDecidedAt(LocalDateTime.now());

                request = approvalRepository.save(request);

                // Update the target object status
                if ("PO".equals(request.getTargetType())) {
                        PurchaseOrder po = poRepository.findById(request.getTargetId()).orElse(null);
                        if (po != null) {
                                if ("APPROVED".equals(status)) {
                                        po.setStatus(com.wd.api.model.enums.PurchaseOrderStatus.ISSUED);
                                } else if ("REJECTED".equals(status)) {
                                        po.setStatus(com.wd.api.model.enums.PurchaseOrderStatus.DRAFT);
                                }
                                poRepository.save(po);
                        }
                } else if ("INVOICE".equals(request.getTargetType())) {
                        ProjectInvoice inv = invoiceRepository.findById(request.getTargetId()).orElse(null);
                        if (inv != null) {
                                inv.setStatus(
                                                "APPROVED".equals(status) ? "ISSUED"
                                                                : ("REJECTED".equals(status) ? "DRAFT"
                                                                                : inv.getStatus()));
                                invoiceRepository.save(inv);
                        }
                } else if ("CHALLAN".equals(request.getTargetType())) {
                        PaymentChallan challan = challanRepository.findById(request.getTargetId()).orElse(null);
                        if (challan != null) {
                                challan.setStatus(
                                                "APPROVED".equals(status) ? "APPROVED"
                                                                : ("REJECTED".equals(status) ? "REJECTED"
                                                                                : challan.getStatus()));
                                challanRepository.save(challan);
                        }
                }

                return mapToDTO(request);
        }

        public List<ApprovalRequestDTO> getPendingApprovals(Long approverId) {
                return approvalRepository.findByApproverIdAndStatus(approverId, "PENDING").stream()
                                .map(this::mapToDTO)
                                .collect(Collectors.toList());
        }

        private ApprovalRequestDTO mapToDTO(ApprovalRequest req) {
                return ApprovalRequestDTO.builder()
                                .id(req.getId())
                                .targetType(req.getTargetType())
                                .targetId(req.getTargetId())
                                .requestedById(req.getRequestedBy().getId())
                                .requestedByName(req.getRequestedBy().getFirstName() + " "
                                                + req.getRequestedBy().getLastName())
                                .approverId(req.getApprover() != null ? req.getApprover().getId() : null)
                                .approverName(req.getApprover() != null
                                                ? req.getApprover().getFirstName() + " "
                                                                + req.getApprover().getLastName()
                                                : null)
                                .status(req.getStatus())
                                .comments(req.getComments())
                                .requestedAt(req.getRequestedAt())
                                .decidedAt(req.getDecidedAt())
                                .build();
        }
}
