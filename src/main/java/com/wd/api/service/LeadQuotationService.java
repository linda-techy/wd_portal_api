package com.wd.api.service;

import com.wd.api.config.CompanyInfoConfig;
import com.wd.api.dto.LeadQuotationSearchFilter;
import com.wd.api.dto.quotation.AddItemFromCatalogRequest;
import com.wd.api.model.Lead;
import com.wd.api.model.LeadQuotation;
import com.wd.api.model.LeadQuotationItem;
import com.wd.api.model.QuotationCatalogItem;
import com.wd.api.repository.LeadQuotationItemRepository;
import com.wd.api.repository.LeadQuotationRepository;
import com.wd.api.repository.LeadRepository;
import com.wd.api.repository.QuotationCatalogItemRepository;
import com.wd.api.service.dpc.DpcRenderService;
import com.wd.api.util.NumberToWords;
import com.openhtmltopdf.extend.FSSupplier;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.persistence.criteria.Predicate;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    @Autowired
    private QuotationCatalogItemRepository catalogItemRepository;

    @Autowired
    private LeadQuotationItemRepository quotationItemRepository;

    @Transactional(readOnly = true)
    public Page<LeadQuotation> searchLeadQuotations(LeadQuotationSearchFilter filter) {
        Specification<LeadQuotation> spec = buildSpecification(filter);
        Page<LeadQuotation> page = quotationRepository.findAll(spec, Objects.requireNonNull(filter.toPageable()));
        enrichWithLeadNames(page.getContent());
        return page;
    }

    /**
     * Batch-populate {@code leadName} on each quotation in the list. Single
     * {@code IN (...)} lookup against the lead table avoids N+1, and the
     * field is {@code @Transient} so it costs nothing on persist. Quietly
     * tolerates missing leads — leadName stays null for orphan quotations.
     */
    private void enrichWithLeadNames(List<LeadQuotation> quotations) {
        if (quotations == null || quotations.isEmpty()) return;
        java.util.Set<Long> leadIds = new java.util.HashSet<>();
        for (LeadQuotation q : quotations) {
            if (q.getLeadId() != null) leadIds.add(q.getLeadId());
        }
        if (leadIds.isEmpty()) return;
        java.util.Map<Long, String> nameByLeadId = new java.util.HashMap<>();
        for (Lead lead : leadRepository.findAllById(leadIds)) {
            nameByLeadId.put(lead.getId(), lead.getName());
        }
        for (LeadQuotation q : quotations) {
            if (q.getLeadId() != null) {
                q.setLeadName(nameByLeadId.get(q.getLeadId()));
            }
        }
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
     * Get quotation by ID with items eagerly loaded via JOIN FETCH (single SQL).
     */
    @Transactional(readOnly = true)
    public LeadQuotation getQuotationById(Long id) {
        return quotationRepository.findByIdWithItems(Objects.requireNonNull(id, "Quotation ID is required"))
                .orElseThrow(() -> new RuntimeException("Quotation not found with id: " + id));
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
     * Add a new line item to a quotation, sourced from the master catalog.
     *
     * <p>Recomputes quotation totals from the items collection and increments
     * the catalog row's {@code timesUsed} counter as a side effect.
     *
     * @throws IllegalArgumentException if the quotation or catalog item is not found
     */
    @Transactional
    public LeadQuotationItem addItemFromCatalog(Long quotationId, AddItemFromCatalogRequest req) {
        LeadQuotation quotation = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new IllegalArgumentException("Quotation not found: " + quotationId));

        // Lock down items once a quotation has been sent to the lead — otherwise
        // we silently mutate something the customer already saw.
        if (!"DRAFT".equalsIgnoreCase(quotation.getStatus())) {
            throw new IllegalStateException(
                    "Quotation " + quotationId + " is in status " + quotation.getStatus()
                            + " — only DRAFT quotations can have items added");
        }

        QuotationCatalogItem catalogItem = catalogItemRepository.findById(req.catalogItemId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Catalog item not found: " + req.catalogItemId()));

        // Build description: name (+ " - " + truncated description if present).
        String description = catalogItem.getName();
        if (catalogItem.getDescription() != null && !catalogItem.getDescription().isBlank()) {
            String desc = catalogItem.getDescription();
            if (desc.length() > 500) {
                desc = desc.substring(0, 500);
            }
            description = description + " - " + desc;
        }

        BigDecimal quantity = req.quantity() != null ? req.quantity() : BigDecimal.ONE;
        BigDecimal unitPrice = req.unitPriceOverride() != null
                ? req.unitPriceOverride()
                : catalogItem.getDefaultUnitPrice();
        BigDecimal totalPrice = quantity.multiply(unitPrice);

        // Compute next item number — max existing + 1, or 1 if no items yet.
        int nextItemNumber = quotation.getItems().stream()
                .map(LeadQuotationItem::getItemNumber)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0) + 1;

        LeadQuotationItem item = new LeadQuotationItem();
        item.setQuotation(quotation);
        item.setItemNumber(nextItemNumber);
        item.setDescription(description);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setTotalPrice(totalPrice);
        item.setNotes(null);
        item.setCatalogItem(catalogItem);

        // Add to the quotation's collection so totals recompute against the new item.
        quotation.addItem(item);
        LeadQuotationItem saved = quotationItemRepository.save(item);

        // Recompute totals via existing logic and persist the quotation.
        calculateTotals(quotation);
        quotationRepository.save(quotation);

        // Bump usage count on the catalog row.
        catalogItemRepository.incrementTimesUsed(catalogItem.getId());

        return saved;
    }

    /**
     * Aggregate summary stats for the pipeline hero card. Open = DRAFT /
     * SENT / VIEWED (still actionable). Accepted/rejected counts and win
     * rate are computed over a 90-day rolling window so the dashboard
     * reflects the current quarter rather than the full archive.
     */
    @Transactional(readOnly = true)
    public com.wd.api.dto.quotation.PipelineSummaryResponse getPipelineSummary() {
        java.time.LocalDateTime since = java.time.LocalDateTime.now().minusDays(90);
        List<Object[]> rows = quotationRepository.pipelineRowsSince(since);

        long openCount = 0;
        BigDecimal openValue = BigDecimal.ZERO;
        long acceptedCount = 0;
        BigDecimal acceptedValue = BigDecimal.ZERO;
        long rejectedCount = 0;
        long totalCloseDays = 0;
        int closeSamples = 0;

        for (Object[] row : rows) {
            String status = (String) row[0];
            BigDecimal finalAmount = (BigDecimal) row[1];
            java.time.LocalDateTime sentAt = (java.time.LocalDateTime) row[2];
            java.time.LocalDateTime respondedAt = (java.time.LocalDateTime) row[3];

            if ("DRAFT".equals(status) || "SENT".equals(status) || "VIEWED".equals(status)) {
                openCount++;
                if (finalAmount != null) openValue = openValue.add(finalAmount);
            } else if ("ACCEPTED".equals(status)) {
                acceptedCount++;
                if (finalAmount != null) acceptedValue = acceptedValue.add(finalAmount);
                if (sentAt != null && respondedAt != null) {
                    totalCloseDays += java.time.temporal.ChronoUnit.DAYS.between(sentAt, respondedAt);
                    closeSamples++;
                }
            } else if ("REJECTED".equals(status)) {
                rejectedCount++;
            }
        }

        long closedTotal = acceptedCount + rejectedCount;
        double winRate = closedTotal > 0 ? (acceptedCount * 100.0) / closedTotal : 0.0;
        Double avgCloseDays = closeSamples > 0 ? (double) totalCloseDays / closeSamples : null;

        return new com.wd.api.dto.quotation.PipelineSummaryResponse(
                openCount, openValue, acceptedCount, acceptedValue, winRate, avgCloseDays);
    }

    /**
     * Duplicate an existing quotation as a fresh DRAFT — copies header,
     * pricing knobs, and items (including catalog FKs); resets identifiers,
     * status, and lifecycle timestamps. The most-requested missing CRM
     * action: re-quoting a similar villa for a new lead, or revising after
     * a customer-led scope change.
     *
     * <p>Quotation number is generated fresh; title gets a " (Copy)" suffix
     * so the user notices the duplicate in the list before they edit it.
     */
    @Transactional
    public LeadQuotation duplicateQuotation(Long sourceId, Long currentUserId) {
        LeadQuotation src = quotationRepository.findByIdWithItems(sourceId)
                .orElseThrow(() -> new RuntimeException("Quotation not found: " + sourceId));

        LeadQuotation copy = new LeadQuotation();
        copy.setLeadId(src.getLeadId());
        copy.setQuotationNumber(generateQuotationNumber());
        copy.setVersion(1);
        copy.setTitle((src.getTitle() != null ? src.getTitle() : "Quotation") + " (Copy)");
        copy.setDescription(src.getDescription());
        copy.setValidityDays(src.getValidityDays());
        copy.setTaxRatePercent(src.getTaxRatePercent());
        copy.setDiscountAmount(src.getDiscountAmount());
        copy.setNotes(src.getNotes());
        copy.setStatus("DRAFT");
        copy.setCreatedById(currentUserId);

        // Items — fresh rows, but preserve catalog-FK and content. Catalog
        // promotion source-of-truth stays the original quotation; the copy
        // is a fresh consumer of the same catalog rows.
        for (LeadQuotationItem srcItem : src.getItems()) {
            LeadQuotationItem newItem = new LeadQuotationItem();
            newItem.setItemNumber(srcItem.getItemNumber());
            newItem.setDescription(srcItem.getDescription());
            newItem.setQuantity(srcItem.getQuantity());
            newItem.setUnitPrice(srcItem.getUnitPrice());
            newItem.setTotalPrice(srcItem.getTotalPrice());
            newItem.setNotes(srcItem.getNotes());
            newItem.setCatalogItem(srcItem.getCatalogItem());
            copy.addItem(newItem);
        }

        calculateTotals(copy);
        return quotationRepository.save(copy);
    }

    /**
     * Resolve the subtotal source for a quotation based on its pricing mode.
     *
     * <ul>
     *   <li>{@code LINE_ITEM} — sum of {@code item.totalPrice} (legacy).</li>
     *   <li>{@code SQFT_RATE} — {@code lead.projectSqftArea × quotation.ratePerSqft}.
     *       Subtotal is {@code 0} (not an error) when either input is missing —
     *       a partially-typed quotation in the Flutter form should still be
     *       saveable as a draft.</li>
     * </ul>
     *
     * <p>Package-private so the unit test can drive each branch directly.
     */
    BigDecimal computeSubtotal(LeadQuotation quotation) {
        if ("SQFT_RATE".equals(quotation.getPricingMode())) {
            BigDecimal rate = quotation.getRatePerSqft();
            if (rate == null || rate.signum() <= 0) return BigDecimal.ZERO;
            BigDecimal sqft = resolveLeadSqft(quotation.getLeadId());
            if (sqft == null || sqft.signum() <= 0) return BigDecimal.ZERO;
            return sqft.multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP);
        }
        // LINE_ITEM (legacy default for migrated rows).
        return quotation.getItems().stream()
                .map(LeadQuotationItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Resolve {@code Lead.projectSqftArea} for a quotation. Tolerates
     * unknown / orphan leads with {@code null} so {@link #computeSubtotal}
     * can collapse to zero rather than throwing during a draft save.
     */
    private BigDecimal resolveLeadSqft(Long leadId) {
        if (leadId == null) return null;
        return leadRepository.findById(leadId)
                .map(Lead::getProjectSqftArea)
                .orElse(null);
    }

    /**
     * Generate a unique, race-safe quotation number using the
     * {@code lead_quotation_number_seq} Postgres sequence (V72).
     *
     * <p>Format: {@code YYYY/MM/DD/A{seq}} — matches Walldot's actual
     * paper reference (e.g. {@code 2026/02/04/A6} on Mr Clinton's
     * quotation). The "A" prefix is the company-internal series code;
     * sequence is a single global counter so collisions are impossible.
     *
     * <p>The slashes in the format are URL-unsafe but the existing
     * download path already escapes them
     * ({@code quotationNumber.replace("/", "_")} in the controller's
     * {@code Content-Disposition}), so they don't break PDF downloads.
     */
    private String generateQuotationNumber() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        String datePart = LocalDateTime.now().format(formatter);
        long seq = quotationRepository.nextQuotationSequenceValue();
        return String.format("%s/A%d", datePart, seq);
    }

    /**
     * Compute and write the totals on a quotation.
     *
     * <p>Subtotal source depends on {@link LeadQuotation#getPricingMode()}:
     * <ul>
     *   <li>{@code LINE_ITEM} — {@code subtotal = SUM(item.totalPrice)} (legacy).</li>
     *   <li>{@code SQFT_RATE} — {@code subtotal = lead.projectSqftArea × ratePerSqft}
     *       (Walldot customer-facing quotation). Items are scope specs, not
     *       priced rows; their prices are ignored. When the lead has no sqft
     *       on file or {@code ratePerSqft} is unset, subtotal collapses to 0.</li>
     * </ul>
     *
     * <p>Discount + tax then apply uniformly:
     * <ol>
     *   <li>{@code discountedBase = subtotal − discount} (validated: discount ≤ subtotal)</li>
     *   <li>If {@code taxRatePercent} is set, {@code taxAmount = discountedBase × rate / 100}
     *       (rounded HALF_UP, overwrites manual entry). When rate is {@code null},
     *       the staff-entered manual {@code taxAmount} is honored unchanged.</li>
     *   <li>{@code finalAmount = discountedBase + taxAmount}</li>
     * </ol>
     *
     * <p>Package-private so unit tests can drive the math directly without
     * having to mock the repositories.
     *
     * @throws IllegalArgumentException if {@code discountAmount} exceeds the
     *         subtotal (negative finalAmount → broken accounting), or
     *         {@code discountAmount} is negative.
     */
    void calculateTotals(LeadQuotation quotation) {
        BigDecimal subtotal = computeSubtotal(quotation);
        quotation.setTotalAmount(subtotal);

        BigDecimal discount = quotation.getDiscountAmount() != null
                ? quotation.getDiscountAmount() : BigDecimal.ZERO;
        if (discount.signum() < 0) {
            throw new IllegalArgumentException("discount amount cannot be negative");
        }
        if (discount.compareTo(subtotal) > 0) {
            throw new IllegalArgumentException(
                    "discount amount " + discount + " exceeds subtotal " + subtotal
                            + " — quotation final cannot be negative");
        }

        BigDecimal discountedBase = subtotal.subtract(discount);

        BigDecimal taxAmount;
        if (quotation.getTaxRatePercent() != null) {
            // Auto-compute against the discounted base — this is the canonical
            // Indian-GST tax base. Round to 2dp HALF_UP to match the column.
            taxAmount = discountedBase
                    .multiply(quotation.getTaxRatePercent())
                    .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            quotation.setTaxAmount(taxAmount);
        } else {
            taxAmount = quotation.getTaxAmount() != null
                    ? quotation.getTaxAmount() : BigDecimal.ZERO;
        }

        quotation.setFinalAmount(discountedBase.add(taxAmount));
    }

    /**
     * Soft-delete a quotation. Backed by the entity's {@code @SQLDelete}
     * directive — the row gets a {@code deleted_at} tombstone rather than
     * a destructive DELETE. The {@code @SQLRestriction} filter then hides
     * it from every normal query.
     *
     * <p>Restricted to DRAFT status because sending/accepting a quote is a
     * commitment to the customer; deleting one in those states would leave
     * the relationship in an undefined place. Restoration via
     * {@link #restoreQuotation(Long)} is available for the Undo window.
     */
    @Transactional
    public void deleteQuotation(Long id) {
        LeadQuotation quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quotation not found"));

        if (!"DRAFT".equals(quotation.getStatus())) {
            throw new IllegalStateException("Only DRAFT quotations can be deleted");
        }

        quotationRepository.delete(quotation);
    }

    /**
     * Restore a soft-deleted quotation. Triggered by the Flutter "Undo"
     * snackbar within a 5-second window after a delete. Returns the
     * restored quotation; throws when the row is not currently tombstoned
     * (already-restored, never-deleted, or unknown id).
     */
    @Transactional
    public LeadQuotation restoreQuotation(Long id) {
        int updated = quotationRepository.restoreById(id);
        if (updated == 0) {
            throw new RuntimeException(
                    "Cannot restore quotation " + id + " — not found or not deleted");
        }
        return quotationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Restored quotation " + id + " disappeared from query — "
                                + "investigate concurrent delete"));
    }

    /**
     * Generate quotation PDF for client presentation
     * Enterprise-grade PDF generation with company branding and professional
     * formatting.
     *
     * <p>{@code @Transactional(readOnly = true)} keeps the Hibernate session
     * open for the duration of the render so the lazy-loaded
     * {@code quotation.items} collection can be iterated without throwing
     * {@code LazyInitializationException}.
     */
    @Transactional(readOnly = true)
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

        // Pre-compute validity expiry date in Java — Thymeleaf SpEL can't reliably
        // call LocalDate.plusDays(Integer) chained with createDateTime/createLocalTime.
        java.time.LocalDate validUntil = null;
        Long daysUntilExpiry = null;
        if (quotation.getCreatedAt() != null && quotation.getValidityDays() != null) {
            validUntil = quotation.getCreatedAt().toLocalDate().plusDays(quotation.getValidityDays().longValue());
            // Drive the urgency-pill copy ("X days left") and red-state styling.
            // Negative values are clamped to zero — an EXPIRED quote still says
            // "0 days left" rather than a confusing minus-number.
            long days = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), validUntil);
            daysUntilExpiry = Math.max(0L, days);
        }
        context.setVariable("validUntil", validUntil);
        context.setVariable("daysUntilExpiry", daysUntilExpiry);

        // Pre-format every monetary value using Indian-locale grouping (e.g.
        // 4728200 -> "47,28,200") so the template doesn't need to call
        // `#numbers.formatDecimal` (which produces Western 4,728,200) and so
        // each cell can render the embedded ₹ glyph cleanly.
        List<Map<String, Object>> formattedItems = new ArrayList<>();
        for (LeadQuotationItem item : quotation.getItems()) {
            Map<String, Object> row = new HashMap<>();
            row.put("itemNumber", item.getItemNumber());
            row.put("description", item.getDescription());
            row.put("notes", item.getNotes());
            row.put("quantity", item.getQuantity() != null
                    ? item.getQuantity().stripTrailingZeros().toPlainString()
                    : "1");
            row.put("unitPriceDisplay", DpcRenderService.formatINR(item.getUnitPrice()));
            row.put("totalPriceDisplay", DpcRenderService.formatINR(item.getTotalPrice()));
            formattedItems.add(row);
        }
        context.setVariable("formattedItems", formattedItems);

        BigDecimal subtotal = quotation.getTotalAmount() != null ? quotation.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal tax = quotation.getTaxAmount() != null ? quotation.getTaxAmount() : BigDecimal.ZERO;
        BigDecimal discount = quotation.getDiscountAmount() != null ? quotation.getDiscountAmount() : BigDecimal.ZERO;

        context.setVariable("subtotalDisplay", DpcRenderService.formatINR(subtotal));
        context.setVariable("taxDisplay", tax.signum() > 0 ? DpcRenderService.formatINR(tax) : null);
        context.setVariable("discountDisplay", discount.signum() > 0 ? DpcRenderService.formatINR(discount) : null);
        context.setVariable("finalDisplay", DpcRenderService.formatINR(finalAmount));
        // Suppress duplicate "Subtotal == Final" row when no tax/discount applies.
        context.setVariable("hasAdjustments", tax.signum() > 0 || discount.signum() > 0);

        // Tax label and GST notice — driven by the quotation's actual
        // taxRatePercent so the PDF reflects the configured rate (no more
        // hardcoded "CGST 9% + SGST 9%"). When rate is null the quotation is
        // in legacy manual-tax mode; the label collapses to a generic "Tax".
        BigDecimal taxRate = quotation.getTaxRatePercent();
        String rateLabel;          // "GST (18%)" or "Tax"
        String gstNoticeRateLabel; // "GST extra at 18%" or null when rate unset
        BigDecimal gstAtRate;
        if (taxRate != null) {
            String rateStr = taxRate.stripTrailingZeros().toPlainString();
            rateLabel = "GST (" + rateStr + "%)";
            gstNoticeRateLabel = "GST extra at " + rateStr + "%";
            BigDecimal discountedBase = subtotal.subtract(discount).max(BigDecimal.ZERO);
            gstAtRate = discountedBase.multiply(taxRate)
                    .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
        } else {
            rateLabel = "Tax";
            gstNoticeRateLabel = null;
            gstAtRate = BigDecimal.ZERO;
        }
        context.setVariable("taxRateLabel", rateLabel);
        context.setVariable("gstNoticeRateLabel", gstNoticeRateLabel);
        context.setVariable("gstNoticeDisplay", DpcRenderService.formatINR(gstAtRate));

        // Optional company logo — rendered above the company name in the
        // PDF header. Resolved to a data URI so openhtmltopdf can embed it
        // without needing a network or filesystem lookup at render time.
        // Absent / unreadable file → null → template skips the <img>.
        context.setVariable("companyLogoDataUri", resolveCompanyLogoDataUri());

        // Per-sqft rate when the lead carries a built-up area — Kerala's most
        // discussed residential metric. In SQFT_RATE mode this is the
        // headline number rather than a derived figure.
        BigDecimal area = lead != null ? lead.getProjectSqftArea() : null;
        if (area != null && area.signum() > 0) {
            BigDecimal perSqft = finalAmount.divide(area, 0, java.math.RoundingMode.HALF_UP);
            context.setVariable("areaDisplay", area.stripTrailingZeros().toPlainString());
            context.setVariable("perSqftDisplay", DpcRenderService.formatINR(perSqft));
        } else {
            context.setVariable("areaDisplay", null);
            context.setVariable("perSqftDisplay", null);
        }

        // SQFT_RATE-specific render hints. The template branches on these
        // to render the Walldot scope-table layout (description-only items)
        // and the "Rs. X/- per square feet" headline.
        boolean isSqftRate = "SQFT_RATE".equals(quotation.getPricingMode());
        context.setVariable("isSqftRate", isSqftRate);
        if (isSqftRate && quotation.getRatePerSqft() != null) {
            context.setVariable("ratePerSqftDisplay",
                    DpcRenderService.formatINR(quotation.getRatePerSqft()));
        } else {
            context.setVariable("ratePerSqftDisplay", null);
        }

        // Signatory + city for the SQFT_RATE "With Regards" footer block —
        // mirrors Walldot's paper-format signature. Defaults come from
        // CompanyInfoConfig so a deployment can swap MD/branch without code.
        context.setVariable("companySignatoryName", companyInfoConfig.getSignatoryName());
        context.setVariable("companyCity", companyInfoConfig.getCity());

        // Resolve the payment milestones to rupee amounts. Schedule shape
        // varies by project type — a ₹40L interior fitout shouldn't ride on
        // the same 30/40/25/5 cashflow as a ₹3 cr villa. Earlier audit
        // flagged the hardcoded waterfall as a trust killer; the per-type
        // map below resolves that, with a sensible default for unknown
        // types so we never crash on legacy or unmapped leads.
        String projectType = (lead != null && lead.getProjectType() != null)
                ? lead.getProjectType().toUpperCase(Locale.ROOT)
                : "DEFAULT";
        context.setVariable("milestones",
                resolveMilestonesForProjectType(projectType, finalAmount));

        // Company registration + bank details (driven by application.yml /
        // CompanyInfoConfig — empty values are safely hidden by the template).
        context.setVariable("companyGst",   companyInfoConfig.getGst());
        context.setVariable("companyPan",   companyInfoConfig.getPan());
        context.setVariable("companyLlpin", companyInfoConfig.getLlpin());
        context.setVariable("bankAccountName",   companyInfoConfig.getBankAccountName());
        context.setVariable("bankAccountNumber", companyInfoConfig.getBankAccountNumber());
        context.setVariable("bankIfsc",          companyInfoConfig.getBankIfsc());
        context.setVariable("bankName",          companyInfoConfig.getBankName());
        context.setVariable("bankBranch",        companyInfoConfig.getBankBranch());

        // Pre-format an absolute valid_until date for the budgetary template.
        // Falls back to the legacy "createdAt + validityDays" derivation when
        // the V76 valid_until column hasn't been set on the row yet.
        String validUntilDisplay = null;
        java.time.LocalDate effectiveValidUntil = quotation.getValidUntil() != null
                ? quotation.getValidUntil()
                : validUntil;
        if (effectiveValidUntil != null) {
            validUntilDisplay = effectiveValidUntil.format(
                    java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"));
        }
        context.setVariable("validUntilDisplay", validUntilDisplay);

        // V76: pick the template by quotation_type. BUDGETARY uses the new
        // tier-card / no-totals layout; everything else falls through to the
        // legacy template until those stages are split out.
        String templateName = "BUDGETARY".equals(quotation.getQuotationType())
                ? "quotation-budgetary"
                : "quotation-template";
        String html = templateEngine.process(templateName, context);

        // Generate PDF using openhtmltopdf with Arial embedded so the rupee
        // glyph (U+20B9) renders — without an embedded font that supports it
        // openhtmltopdf substitutes "#" which is exactly what users were seeing.
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            FSSupplier<InputStream> arial     = () -> openFontStream("fonts/Arial.ttf");
            FSSupplier<InputStream> arialBold = () -> openFontStream("fonts/Arial-Bold.ttf");
            builder.useFont(arial,     "Arial", 400, FontStyle.NORMAL, true);
            builder.useFont(arialBold, "Arial", 700, FontStyle.NORMAL, true);
            builder.withHtmlContent(html, "/");
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating quotation PDF", e);
        }
    }

    /** Build one milestone row with label, percentage and resolved rupee amount. */
    private static Map<String, String> milestoneRow(String label, String pctLabel,
                                                    BigDecimal finalAmount, BigDecimal pct) {
        Map<String, String> row = new HashMap<>();
        row.put("label", label);
        row.put("pct", pctLabel);
        BigDecimal amount = finalAmount.multiply(pct).setScale(0, java.math.RoundingMode.HALF_UP);
        row.put("amountDisplay", DpcRenderService.formatINR(amount));
        return row;
    }

    /**
     * One milestone in a payment schedule template. Pure value; values are
     * stable so the schedule resolves deterministically from project type.
     */
    private static final class MilestoneSpec {
        final String label;
        final String pctLabel;
        final BigDecimal pct;
        MilestoneSpec(String label, String pctLabel, BigDecimal pct) {
            this.label = label;
            this.pctLabel = pctLabel;
            this.pct = pct;
        }
    }

    /**
     * Payment-schedule templates keyed by {@code Lead.projectType} (upper-cased).
     * Each template's percentages must sum to 1.00 — the unit test asserts this.
     *
     * <ul>
     *   <li>RESIDENTIAL — softer 20% advance for Kerala small-residential
     *       (less aggressive than the old 30%).</li>
     *   <li>VILLA       — same as residential but with a heavier finishing
     *       weight, since villas spend more on joinery / wall finishes.</li>
     *   <li>COMMERCIAL  — closer to the old waterfall; commercial customers
     *       expect a 30% mobilisation advance.</li>
     *   <li>INTERIOR    — front-loaded; interior fitouts spend most on
     *       material procurement before site work.</li>
     *   <li>TURNKEY     — balanced, matches typical turnkey contracts.</li>
     *   <li>DEFAULT     — preserved 30/40/25/5 so existing demo data stays
     *       arithmetically consistent with V28 seed.</li>
     * </ul>
     */
    private static final Map<String, List<MilestoneSpec>> MILESTONE_TEMPLATES = Map.of(
            "RESIDENTIAL", List.of(
                    new MilestoneSpec("Advance on acceptance",  "20%", new BigDecimal("0.20")),
                    new MilestoneSpec("Foundation completion",  "40%", new BigDecimal("0.40")),
                    new MilestoneSpec("Structure completion",   "30%", new BigDecimal("0.30")),
                    new MilestoneSpec("Final handover",         "10%", new BigDecimal("0.10"))),
            "VILLA", List.of(
                    new MilestoneSpec("Advance on acceptance",  "20%", new BigDecimal("0.20")),
                    new MilestoneSpec("Foundation completion",  "30%", new BigDecimal("0.30")),
                    new MilestoneSpec("Structure completion",   "30%", new BigDecimal("0.30")),
                    new MilestoneSpec("Finishing & handover",   "20%", new BigDecimal("0.20"))),
            "COMMERCIAL", List.of(
                    new MilestoneSpec("Advance on acceptance",  "30%", new BigDecimal("0.30")),
                    new MilestoneSpec("Foundation completion",  "40%", new BigDecimal("0.40")),
                    new MilestoneSpec("Structure completion",   "25%", new BigDecimal("0.25")),
                    new MilestoneSpec("Final handover",         "5%",  new BigDecimal("0.05"))),
            "INTERIOR", List.of(
                    new MilestoneSpec("Material procurement",   "40%", new BigDecimal("0.40")),
                    new MilestoneSpec("Carcass installation",   "40%", new BigDecimal("0.40")),
                    new MilestoneSpec("Snag-list & handover",   "20%", new BigDecimal("0.20"))),
            "TURNKEY", List.of(
                    new MilestoneSpec("Advance on acceptance",  "25%", new BigDecimal("0.25")),
                    new MilestoneSpec("Foundation completion",  "35%", new BigDecimal("0.35")),
                    new MilestoneSpec("Structure completion",   "30%", new BigDecimal("0.30")),
                    new MilestoneSpec("Final handover",         "10%", new BigDecimal("0.10"))),
            "DEFAULT", List.of(
                    new MilestoneSpec("Advance on acceptance",  "30%", new BigDecimal("0.30")),
                    new MilestoneSpec("Foundation completion",  "40%", new BigDecimal("0.40")),
                    new MilestoneSpec("Structure completion",   "25%", new BigDecimal("0.25")),
                    new MilestoneSpec("Final handover",         "5%",  new BigDecimal("0.05")))
    );

    /**
     * Resolve and render the payment-milestone rows for a given project type.
     * Falls back to {@code DEFAULT} when the type is missing or unmapped.
     * Package-private so unit tests can drive the lookup directly.
     */
    static List<Map<String, String>> resolveMilestonesForProjectType(
            String projectType, BigDecimal finalAmount) {
        List<MilestoneSpec> specs = MILESTONE_TEMPLATES.getOrDefault(
                projectType != null ? projectType.toUpperCase(Locale.ROOT) : "DEFAULT",
                MILESTONE_TEMPLATES.get("DEFAULT"));
        List<Map<String, String>> out = new ArrayList<>(specs.size());
        for (MilestoneSpec s : specs) {
            out.add(milestoneRow(s.label, s.pctLabel, finalAmount, s.pct));
        }
        return out;
    }

    /** Open a classpath font resource as an InputStream — wraps the checked
     *  IOException so it can be used inside an FSSupplier lambda. */
    private static InputStream openFontStream(String classPathResource) {
        try {
            return new ClassPathResource(classPathResource).getInputStream();
        } catch (IOException e) {
            throw new RuntimeException("Quotation font missing on classpath: " + classPathResource, e);
        }
    }

    /**
     * Load the configured company logo and return it as a data URI suitable
     * for an HTML {@code <img src=...>}. Returns {@code null} if the path is
     * unset or the file cannot be read — the template treats absence as
     * "render header without logo" rather than failing the PDF.
     *
     * <p>Accepts either {@code classpath:branding/foo.png} (resource bundled
     * with the JAR) or an absolute filesystem path.
     */
    private String resolveCompanyLogoDataUri() {
        String configured = companyInfoConfig.getLogoPath();
        if (configured == null || configured.isBlank()) return null;
        try {
            byte[] bytes;
            String mimeSource;
            if (configured.startsWith("classpath:")) {
                String resource = configured.substring("classpath:".length());
                try (InputStream is = new ClassPathResource(resource).getInputStream()) {
                    bytes = is.readAllBytes();
                }
                mimeSource = resource;
            } else {
                bytes = Files.readAllBytes(Path.of(configured));
                mimeSource = configured;
            }
            return "data:" + guessImageMime(mimeSource) + ";base64,"
                    + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            // Logo is optional. Don't fail the PDF if the file is missing —
            // typical state during dev before a brand asset has been dropped in.
            return null;
        }
    }

    /** Map a file extension to its MIME type for data-URI rendering. */
    private static String guessImageMime(String pathOrName) {
        String lower = pathOrName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".gif")) return "image/gif";
        return "application/octet-stream";
    }
}
