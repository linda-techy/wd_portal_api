package com.wd.api.service;

import com.wd.api.model.LeadQuotation;
import com.wd.api.model.LeadQuotationItem;
import com.wd.api.repository.LeadQuotationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for managing lead quotations and proposals
 */
@Service
public class LeadQuotationService {

    @Autowired
    private LeadQuotationRepository quotationRepository;

    /**
     * Get all quotations for a specific lead
     */
    public List<LeadQuotation> getQuotationsByLeadId(Long leadId) {
        return quotationRepository.findByLeadIdOrderByVersionDesc(leadId);
    }

    /**
     * Get quotations by status
     */
    public List<LeadQuotation> getQuotationsByStatus(String status) {
        return quotationRepository.findByStatus(status);
    }

    /**
     * Create a new quotation
     */
    @Transactional
    public LeadQuotation createQuotation(LeadQuotation quotation, Long createdById) {
        // Generate quotation number if not provided
        if (quotation.getQuotationNumber() == null || quotation.getQuotationNumber().isEmpty()) {
            quotation.setQuotationNumber(generateQuotationNumber());
        }

        quotation.setCreatedById(createdById);
        quotation.setStatus("DRAFT");

        // Calculate totals from items if present
        if (quotation.getItems() != null && !quotation.getItems().isEmpty()) {
            calculateTotals(quotation);
        }

        return quotationRepository.save(quotation);
    }

    /**
     * Update an existing quotation
     */
    @Transactional
    public LeadQuotation updateQuotation(Long id, LeadQuotation updatedQuotation) {
        LeadQuotation existing = quotationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quotation not found"));

        // Only allow updates if not accepted or rejected
        if ("ACCEPTED".equals(existing.getStatus()) || "REJECTED".equals(existing.getStatus())) {
            throw new IllegalStateException("Cannot update quotation with status: " + existing.getStatus());
        }

        existing.setTitle(updatedQuotation.getTitle());
        existing.setDescription(updatedQuotation.getDescription());
        existing.setTotalAmount(updatedQuotation.getTotalAmount());
        existing.setTaxAmount(updatedQuotation.getTaxAmount());
        existing.setDiscountAmount(updatedQuotation.getDiscountAmount());
        existing.setFinalAmount(updatedQuotation.getFinalAmount());
        existing.setValidityDays(updatedQuotation.getValidityDays());
        existing.setNotes(updatedQuotation.getNotes());

        return quotationRepository.save(existing);
    }

    /**
     * Send quotation to lead
     */
    @Transactional
    public LeadQuotation sendQuotation(Long id) {
        LeadQuotation quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quotation not found"));

        if (!"DRAFT".equals(quotation.getStatus())) {
            throw new IllegalStateException("Only DRAFT quotations can be sent");
        }

        quotation.setStatus("SENT");
        quotation.setSentAt(LocalDateTime.now());

        return quotationRepository.save(quotation);
    }

    /**
     * Mark quotation as viewed by lead
     */
    @Transactional
    public LeadQuotation markAsViewed(Long id) {
        LeadQuotation quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quotation not found"));

        if (quotation.getViewedAt() == null) {
            quotation.setViewedAt(LocalDateTime.now());
            if ("SENT".equals(quotation.getStatus())) {
                quotation.setStatus("VIEWED");
            }
        }

        return quotationRepository.save(quotation);
    }

    /**
     * Accept quotation
     */
    @Transactional
    public LeadQuotation acceptQuotation(Long id) {
        LeadQuotation quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quotation not found"));

        quotation.setStatus("ACCEPTED");
        quotation.setRespondedAt(LocalDateTime.now());

        return quotationRepository.save(quotation);
    }

    /**
     * Reject quotation
     */
    @Transactional
    public LeadQuotation rejectQuotation(Long id, String reason) {
        LeadQuotation quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quotation not found"));

        quotation.setStatus("REJECTED");
        quotation.setRespondedAt(LocalDateTime.now());
        if (reason != null) {
            quotation.setNotes(quotation.getNotes() != null ? quotation.getNotes() + "\nRejection Reason: " + reason
                    : "Rejection Reason: " + reason);
        }

        return quotationRepository.save(quotation);
    }

    /**
     * Generate unique quotation number
     */
    private String generateQuotationNumber() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String datePart = LocalDateTime.now().format(formatter);
        long count = quotationRepository.count() + 1;
        return String.format("QUO-%s-%04d", datePart, count);
    }

    /**
     * Calculate totals based on line items
     */
    private void calculateTotals(LeadQuotation quotation) {
        BigDecimal total = quotation.getItems().stream()
                .map(LeadQuotationItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        quotation.setTotalAmount(total);

        // Calculate final amount with tax and discount
        BigDecimal finalAmount = total;
        if (quotation.getTaxAmount() != null) {
            finalAmount = finalAmount.add(quotation.getTaxAmount());
        }
        if (quotation.getDiscountAmount() != null) {
            finalAmount = finalAmount.subtract(quotation.getDiscountAmount());
        }

        quotation.setFinalAmount(finalAmount);
    }

    /**
     * Delete quotation
     */
    @Transactional
    public void deleteQuotation(Long id) {
        LeadQuotation quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quotation not found"));

        // Only allow deletion of DRAFT quotations
        if (!"DRAFT".equals(quotation.getStatus())) {
            throw new IllegalStateException("Only DRAFT quotations can be deleted");
        }

        quotationRepository.delete(quotation);
    }
}
