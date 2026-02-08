package com.wd.api.service;

import com.wd.api.dto.VendorQuotationSearchFilter;
import com.wd.api.model.*;
import com.wd.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorQuotationService {

        private final VendorQuotationRepository quotationRepository;
        private final MaterialIndentRepository indentRepository;
        private final VendorRepository vendorRepository;
        private final ProcurementService procurementService;

        @Transactional(readOnly = true)
        @SuppressWarnings("null")
        public Page<VendorQuotation> searchVendorQuotations(VendorQuotationSearchFilter filter) {
                Specification<VendorQuotation> spec = buildSpecification(filter);
                return quotationRepository.findAll(spec, filter.toPageable());
        }

        private Specification<VendorQuotation> buildSpecification(VendorQuotationSearchFilter filter) {
                return (root, query, cb) -> {
                        List<Predicate> predicates = new ArrayList<>();

                        // Search across quotationNumber, vendor name
                        if (filter.getSearch() != null && !filter.getSearch().isEmpty()) {
                                String searchPattern = "%" + filter.getSearch().toLowerCase() + "%";
                                predicates.add(cb.or(
                                                cb.like(cb.lower(root.get("quotationNumber")), searchPattern),
                                                cb.like(cb.lower(root.join("vendor").get("name")), searchPattern)));
                        }

                        // Filter by vendorId
                        if (filter.getVendorId() != null) {
                                predicates.add(cb.equal(root.get("vendor").get("id"), filter.getVendorId()));
                        }

                        // Filter by projectId (through indent)
                        if (filter.getProjectId() != null) {
                                predicates.add(cb.equal(root.join("indent").join("project").get("id"),
                                                filter.getProjectId()));
                        }

                        // Filter by quotationNumber
                        if (filter.getQuotationNumber() != null && !filter.getQuotationNumber().isEmpty()) {
                                predicates.add(cb.like(cb.lower(root.get("quotationNumber")),
                                                "%" + filter.getQuotationNumber().toLowerCase() + "%"));
                        }

                        // Filter by status
                        if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
                                predicates.add(cb.equal(root.get("status"),
                                                VendorQuotation.QuotationStatus.valueOf(filter.getStatus())));
                        }

                        // Amount range filter
                        if (filter.getMinAmount() != null) {
                                predicates.add(cb.greaterThanOrEqualTo(root.get("quotedAmount"),
                                                filter.getMinAmount()));
                        }
                        if (filter.getMaxAmount() != null) {
                                predicates.add(cb.lessThanOrEqualTo(root.get("quotedAmount"), filter.getMaxAmount()));
                        }

                        // Date range filter
                        if (filter.getStartDate() != null) {
                                predicates.add(cb.greaterThanOrEqualTo(root.get("quotationDate"),
                                                filter.getStartDate().atStartOfDay()));
                        }
                        if (filter.getEndDate() != null) {
                                predicates.add(cb.lessThanOrEqualTo(root.get("quotationDate"),
                                                filter.getEndDate().atTime(23, 59, 59)));
                        }

                        return cb.and(predicates.toArray(new Predicate[0]));
                };
        }

        @Transactional
        @SuppressWarnings("null")
        public VendorQuotation createQuotation(Long indentId, Long vendorId, VendorQuotation quotation) {
                MaterialIndent indent = indentRepository.findById(indentId)
                                .orElseThrow(() -> new RuntimeException("Indent not found"));

                Vendor vendor = vendorRepository.findById(vendorId)
                                .orElseThrow(() -> new RuntimeException("Vendor not found"));

                quotation.setIndent(indent);
                quotation.setVendor(vendor);
                quotation.setStatus(VendorQuotation.QuotationStatus.PENDING);

                return quotationRepository.save(quotation);
        }

        @Transactional(readOnly = true)
        public List<VendorQuotation> getQuotationsForIndent(Long indentId) {
                return quotationRepository.findByIndentId(indentId);
        }

        @Transactional
        @SuppressWarnings("null")
        public VendorQuotation approveQuotation(Long quotationId) {
                VendorQuotation quotation = quotationRepository.findById(quotationId)
                                .orElseThrow(() -> new RuntimeException("Quotation not found"));

                // Update status
                quotation.setStatus(VendorQuotation.QuotationStatus.SELECTED);
                quotation.setSelectedAt(LocalDateTime.now());
                quotation = quotationRepository.save(quotation);

                // Update Indent Status
                MaterialIndent indent = quotation.getIndent();
                indent.setStatus(MaterialIndent.IndentStatus.PO_CREATED);
                indentRepository.save(indent);

                // Create Draft Purchase Order
                createDraftPOFromQuotation(quotation);

                return quotation;
        }

        private void createDraftPOFromQuotation(VendorQuotation quotation) {
                MaterialIndent indent = quotation.getIndent();

                List<com.wd.api.dto.PurchaseOrderItemDTO> poItems = indent.getItems().stream()
                                .map(indentItem -> com.wd.api.dto.PurchaseOrderItemDTO.builder()
                                                .description("From Indent: " + indentItem.getMaterial().getName())
                                                .materialId(indentItem.getMaterial().getId())
                                                .quantity(indentItem.getQuantityRequested())
                                                .unit(com.wd.api.model.enums.MaterialUnit.valueOf(indentItem.getUnit())) // Convert
                                                                                                                         // String
                                                                                                                         // to
                                                                                                                         // Enum
                                                .rate(java.math.BigDecimal.ZERO) // Rate unknown at item level, only
                                                                                 // total known
                                                .amount(java.math.BigDecimal.ZERO)
                                                .gstPercentage(java.math.BigDecimal.ZERO)
                                                .build())
                                .toList();

                com.wd.api.dto.PurchaseOrderDTO poDTO = com.wd.api.dto.PurchaseOrderDTO.builder()
                                .projectId(indent.getProject().getId())
                                .vendorId(quotation.getVendor().getId())
                                .vendorName(quotation.getVendor().getName())
                                .indentId(indent.getId())
                                .quotationId(quotation.getId())
                                .poDate(java.time.LocalDate.now())
                                .expectedDeliveryDate(quotation.getExpectedDeliveryDate()) // Using delivery date from
                                                                                           // quote
                                .totalAmount(quotation.getQuotedAmount())
                                .gstAmount(java.math.BigDecimal.ZERO) // Simplified for now
                                .netAmount(quotation.getQuotedAmount())
                                .status("DRAFT")
                                .notes("Auto-generated from Quotation #" + quotation.getId() + ". Please update rates.")
                                .items(poItems)
                                .build();

                procurementService.createPurchaseOrder(poDTO);
        }
}
