package com.wd.api.service;

import com.wd.api.dto.ApprovalRequestDTO;
import com.wd.api.dto.ApprovalSearchFilter;
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
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApprovalService {

        private final ApprovalRequestRepository approvalRepository;
        private final PortalUserRepository userRepository;
        private final PurchaseOrderRepository poRepository;
        private final ProjectInvoiceRepository invoiceRepository;
        private final PaymentChallanRepository challanRepository;

        @Transactional(readOnly = true)
        public Page<ApprovalRequest> searchApprovals(ApprovalSearchFilter filter) {
                Specification<ApprovalRequest> spec = buildSpecification(filter);
                return approvalRepository.findAll(spec, Objects.requireNonNull(filter.toPageable()));
        }

        private Specification<ApprovalRequest> buildSpecification(ApprovalSearchFilter filter) {
                return (root, query, cb) -> {
                        List<Predicate> predicates = new ArrayList<>();

                        // Search across requester name, approver name
                        if (filter.getSearch() != null && !filter.getSearch().isEmpty()) {
                                String searchPattern = "%" + filter.getSearch().toLowerCase() + "%";
                                predicates.add(cb.or(
                                                cb.like(cb.lower(root.join("requestedBy").get("firstName")),
                                                                searchPattern),
                                                cb.like(cb.lower(root.join("requestedBy").get("lastName")),
                                                                searchPattern),
                                                cb.like(cb.lower(root.join("approver").get("firstName")),
                                                                searchPattern),
                                                cb.like(cb.lower(root.join("approver").get("lastName")), searchPattern),
                                                cb.like(cb.lower(root.get("targetType")), searchPattern)));
                        }

                        // Filter by moduleType (targetType)
                        if (filter.getModuleType() != null && !filter.getModuleType().isEmpty()) {
                                predicates.add(cb.equal(root.get("targetType"), filter.getModuleType()));
                        }

                        // Filter by referenceId (targetId)
                        if (filter.getReferenceId() != null) {
                                predicates.add(cb.equal(root.get("targetId"), filter.getReferenceId()));
                        }

                        // Filter by approverId
                        if (filter.getApproverId() != null) {
                                predicates.add(cb.equal(root.get("approver").get("id"), filter.getApproverId()));
                        }

                        // Filter by status
                        if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
                                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
                        }

                        // Date range filter
                        if (filter.getStartDate() != null) {
                                predicates.add(cb.greaterThanOrEqualTo(root.get("requestedAt"),
                                                filter.getStartDate().atStartOfDay()));
                        }
                        if (filter.getEndDate() != null) {
                                predicates.add(cb.lessThanOrEqualTo(root.get("requestedAt"),
                                                filter.getEndDate().atTime(23, 59, 59)));
                        }

                        return cb.and(predicates.toArray(new Predicate[0]));
                };
        }

        @Transactional
        public ApprovalRequestDTO createRequest(ApprovalRequestDTO dto) {
                PortalUser requester = userRepository
                                .findById(Objects.requireNonNull(dto.getRequestedById(), "Requester ID is required"))
                                .orElseThrow(() -> new RuntimeException("Requester not found"));

                PortalUser approver = dto.getApproverId() != null
                                ? userRepository.findById(Objects.requireNonNull(dto.getApproverId())).orElse(null)
                                : null;

                ApprovalRequest request = ApprovalRequest.builder()
                                .targetType(dto.getTargetType())
                                .targetId(dto.getTargetId())
                                .requestedBy(requester)
                                .approver(approver)
                                .status("PENDING")
                                .comments(dto.getComments())
                                .build();

                request = approvalRepository.save(Objects.requireNonNull(request));
                return mapToDTO(request);
        }

        @Transactional
        public ApprovalRequestDTO processRequest(Long requestId, String status, String comments, Long approverId) {
                ApprovalRequest request = approvalRepository
                                .findById(Objects.requireNonNull(requestId, "Request ID is required"))
                                .orElseThrow(() -> new RuntimeException("Request not found"));

                PortalUser approver = userRepository
                                .findById(Objects.requireNonNull(approverId, "Approver ID is required"))
                                .orElseThrow(() -> new RuntimeException("Approver not found"));

                request.setStatus(status);
                request.setComments(comments);
                request.setApprover(approver);
                request.setDecidedAt(LocalDateTime.now());

                request = approvalRepository.save(request);

                // Update the target object status
                Long targetId = Objects.requireNonNull(request.getTargetId(), "Target ID is required");
                if ("PO".equals(request.getTargetType())) {
                        PurchaseOrder po = poRepository.findById(targetId).orElse(null);
                        if (po != null) {
                                if ("APPROVED".equals(status)) {
                                        po.setStatus(com.wd.api.model.enums.PurchaseOrderStatus.ISSUED);
                                } else if ("REJECTED".equals(status)) {
                                        po.setStatus(com.wd.api.model.enums.PurchaseOrderStatus.DRAFT);
                                }
                                poRepository.save(po);
                        }
                } else if ("INVOICE".equals(request.getTargetType())) {
                        ProjectInvoice inv = invoiceRepository.findById(targetId).orElse(null);
                        if (inv != null) {
                                inv.setStatus(
                                                "APPROVED".equals(status) ? "ISSUED"
                                                                : ("REJECTED".equals(status) ? "DRAFT"
                                                                                : inv.getStatus()));
                                invoiceRepository.save(inv);
                        }
                } else if ("CHALLAN".equals(request.getTargetType())) {
                        PaymentChallan challan = challanRepository.findById(targetId).orElse(null);
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
