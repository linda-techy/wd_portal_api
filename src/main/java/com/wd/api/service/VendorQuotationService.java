package com.wd.api.service;

import com.wd.api.model.*;
import com.wd.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorQuotationService {

        private final VendorQuotationRepository quotationRepository;
        private final MaterialIndentRepository indentRepository;
        private final VendorRepository vendorRepository;
        private final ProcurementService procurementService;

        @Transactional
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
