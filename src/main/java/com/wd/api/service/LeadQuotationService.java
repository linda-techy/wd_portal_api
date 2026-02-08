package com.wd.api.service;

import com.wd.api.config.CompanyInfoConfig;
import com.wd.api.dto.LeadQuotationSearchFilter;
import com.wd.api.model.Lead;
import com.wd.api.model.LeadQuotation;
import com.wd.api.model.LeadQuotationItem;
import com.wd.api.repository.LeadQuotationRepository;
import com.wd.api.repository.LeadRepository;
import com.wd.api.util.NumberToWords;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.persistence.criteria.Predicate;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service for managing lead quotations and proposals
 */
@Service
public class LeadQuotationService {

    @Autowired
    private LeadQuotationRepository quotationRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private CompanyInfoConfig companyInfoConfig;

    @Transactional(readOnly = true)
    public Page<LeadQuotation> searchLeadQuotations(LeadQuotationSearchFilter filter) {
        Specification<LeadQuotation> spec = buildSpecification(filter);
        return quotationRepository.findAll(spec, Objects.requireNonNull(filter.toPageable()));
    }

    private Specification<LeadQuotation> buildSpecification(LeadQuotationSearchFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search across quotationNumber, notes, status
            if (filter.getSearch() != null && !filter.getSearch().isEmpty()) {
                String searchPattern = "%" + filter.getSearch().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("quotationNumber")), searchPattern),
                        cb.like(cb.lower(root.get("notes")), searchPattern),
                        cb.like(cb.lower(root.get("status")), searchPattern)));
            }

            // Filter by leadId
            if (filter.getLeadId() != null) {
                predicates.add(cb.equal(root.get("leadId"), filter.getLeadId()));
            }

            // Filter by quotationNumber
            if (filter.getQuotationNumber() != null && !filter.getQuotationNumber().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("quotationNumber")),
                        "%" + filter.getQuotationNumber().toLowerCase() + "%"));
            }

            // Filter by preparedById (createdById)
            if (filter.getPreparedById() != null) {
                predicates.add(cb.equal(root.get("createdById"), filter.getPreparedById()));
            }

            // Filter by validityStatus (status)
            if (filter.getValidityStatus() != null && !filter.getValidityStatus().isEmpty()) {
                predicates.add(cb.equal(root.get("status"), filter.getValidityStatus()));
            }

            // Filter by status (from base class)
            if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            // Amount range filter
            if (filter.getMinAmount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("totalAmount"), filter.getMinAmount()));
            }
            if (filter.getMaxAmount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("totalAmount"), filter.getMaxAmount()));
            }

            // Date range filter (on createdAt)
            if (filter.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getStartDate().atStartOfDay()));
            }
            if (filter.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getEndDate().atTime(23, 59, 59)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Get quotation by ID with items eagerly loaded
     */
    @Transactional(readOnly = true)
    public LeadQuotation getQuotationById(Long id) {
        LeadQuotation quotation = quotationRepository.findById(Objects.requireNonNull(id, "Quotation ID is required"))
                .orElseThrow(() -> new RuntimeException("Quotation not found with id: " + id));

        // Force eager loading of items by accessing the collection
        quotation.getItems().size(); // This triggers lazy loading

        return quotation;
    }

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
        LeadQuotation existing = quotationRepository.findById(Objects.requireNonNull(id, "Quotation ID is required"))
                .orElseThrow(() -> new RuntimeException("Quotation not found"));

        // Only allow updates if not accepted or rejected
        if ("ACCEPTED".equals(existing.getStatus()) || "REJECTED".equals(existing.getStatus())) {
            throw new IllegalStateException("Cannot update quotation with status: " + existing.getStatus());
        }

        existing.setTitle(updatedQuotation.getTitle());
        existing.setDescription(updatedQuotation.getDescription());
        existing.setTaxAmount(updatedQuotation.getTaxAmount());
        existing.setDiscountAmount(updatedQuotation.getDiscountAmount());
        existing.setValidityDays(updatedQuotation.getValidityDays());
        existing.setNotes(updatedQuotation.getNotes());

        // Update items if provided
        if (updatedQuotation.getItems() != null && !updatedQuotation.getItems().isEmpty()) {
            // Clear existing items and add new ones
            existing.getItems().clear();
            for (LeadQuotationItem item : updatedQuotation.getItems()) {
                item.setQuotation(existing);
                existing.getItems().add(item);
            }
            // Recalculate totals from items
            calculateTotals(existing);
        } else {
            // If items not provided, use amounts from request (manual override)
            existing.setTotalAmount(updatedQuotation.getTotalAmount());
            existing.setFinalAmount(updatedQuotation.getFinalAmount());
        }

        return quotationRepository.save(existing);
    }

    /**
     * Send quotation to lead
     */
    @Transactional
    public LeadQuotation sendQuotation(Long id) {
        LeadQuotation quotation = quotationRepository.findById(Objects.requireNonNull(id, "Quotation ID is required"))
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
        LeadQuotation quotation = quotationRepository.findById(Objects.requireNonNull(id, "Quotation ID is required"))
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
    @SuppressWarnings("null")
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
    @SuppressWarnings("null")
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
    @SuppressWarnings("null")
    public void deleteQuotation(Long id) {
        LeadQuotation quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quotation not found"));

        // Only allow deletion of DRAFT quotations
        if (!"DRAFT".equals(quotation.getStatus())) {
            throw new IllegalStateException("Only DRAFT quotations can be deleted");
        }

        quotationRepository.delete(quotation);
    }

    /**
     * Generate quotation PDF for client presentation
     * Enterprise-grade PDF generation with company branding and professional
     * formatting
     */
    @SuppressWarnings("null")
    public byte[] generateQuotationPdf(Long quotationId) {
        LeadQuotation quotation = getQuotationById(quotationId);

        // Load lead information for client details
        Optional<Lead> leadOpt = leadRepository.findById(quotation.getLeadId());
        Lead lead = leadOpt.orElse(null);

        // Prepare template context
        Context context = new Context();
        context.setVariable("quotation", quotation);
        context.setVariable("lead", lead);
        context.setVariable("items", quotation.getItems());

        // Company information from configuration
        context.setVariable("companyName", companyInfoConfig.getName());
        context.setVariable("companyAddress", companyInfoConfig.getAddress());
        context.setVariable("companyPhone", companyInfoConfig.getPhone());
        context.setVariable("companyEmail", companyInfoConfig.getEmail());
        context.setVariable("companyWebsite", companyInfoConfig.getWebsite());
        context.setVariable("companyGst", companyInfoConfig.getGst());

        // Calculate amount in words
        BigDecimal finalAmount = quotation.getFinalAmount() != null ? quotation.getFinalAmount()
                : (quotation.getTotalAmount() != null ? quotation.getTotalAmount() : BigDecimal.ZERO);
        String amountInWords = NumberToWords.convert(finalAmount);
        context.setVariable("amountInWords", amountInWords);

        // Process Thymeleaf template
        String html = templateEngine.process("quotation-template", context);

        // Generate PDF using openhtmltopdf
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, "/");
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating quotation PDF", e);
        }
    }
}
