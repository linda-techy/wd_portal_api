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
     * Generate a unique, race-safe quotation number using the
     * {@code lead_quotation_number_seq} Postgres sequence (V72).
     *
     * <p>Format: {@code QUO-{yyyyMMdd}-{NNNN}} with the suffix sourced from
     * the sequence, not from a row count. Replaces the previous
     * {@code count(*) + 1} approach which was racy under concurrent creates
     * and reused numbers after a hard delete.
     */
    private String generateQuotationNumber() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String datePart = LocalDateTime.now().format(formatter);
        long seq = quotationRepository.nextQuotationSequenceValue();
        return String.format("QUO-%s-%04d", datePart, seq);
    }

    /**
     * Compute and write the totals on a quotation from its line items.
     *
     * <p>Order of operations (Indian GST convention):
     * <ol>
     *   <li>{@code subtotal = SUM(item.totalPrice)}</li>
     *   <li>{@code discountedBase = subtotal − discount} (validated: discount ≤ subtotal)</li>
     *   <li>If {@code taxRatePercent} is set on the quotation, {@code taxAmount}
     *       is computed as {@code discountedBase × rate / 100} (rounded HALF_UP
     *       to two decimals) and overwrites whatever was on the entity. When
     *       the rate is {@code null}, the staff-entered manual {@code taxAmount}
     *       is honored unchanged.</li>
     *   <li>{@code finalAmount = discountedBase + taxAmount}</li>
     * </ol>
     *
     * <p>Package-private so unit tests can drive the math directly without
     * having to mock the repositories.
     *
     * @throws IllegalArgumentException if {@code discountAmount} exceeds the
     *         line-item subtotal (negative finalAmount → broken accounting).
     */
    void calculateTotals(LeadQuotation quotation) {
        BigDecimal subtotal = quotation.getItems().stream()
                .map(LeadQuotationItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

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
        // discussed residential metric.
        BigDecimal area = lead != null ? lead.getProjectSqftArea() : null;
        if (area != null && area.signum() > 0) {
            BigDecimal perSqft = finalAmount.divide(area, 0, java.math.RoundingMode.HALF_UP);
            context.setVariable("areaDisplay", area.stripTrailingZeros().toPlainString());
            context.setVariable("perSqftDisplay", DpcRenderService.formatINR(perSqft));
        } else {
            context.setVariable("areaDisplay", null);
            context.setVariable("perSqftDisplay", null);
        }

        // Resolve the standard payment milestones to rupee amounts so customers
        // can budget cash flow concretely instead of doing percent math.
        List<Map<String, String>> milestones = new ArrayList<>();
        milestones.add(milestoneRow("Advance on acceptance",      "30%", finalAmount, new BigDecimal("0.30")));
        milestones.add(milestoneRow("Foundation completion",      "40%", finalAmount, new BigDecimal("0.40")));
        milestones.add(milestoneRow("Structure completion",       "25%", finalAmount, new BigDecimal("0.25")));
        milestones.add(milestoneRow("Final handover",             "5%",  finalAmount, new BigDecimal("0.05")));
        context.setVariable("milestones", milestones);

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

        // Process Thymeleaf template
        String html = templateEngine.process("quotation-template", context);

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
