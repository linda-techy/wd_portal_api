package com.wd.api.service.dpc;

import com.openhtmltopdf.extend.FSSupplier;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.wd.api.config.CompanyInfoConfig;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import com.wd.api.dto.dpc.DpcCostRollupDto;
import com.wd.api.dto.dpc.DpcCustomizationLineDto;
import com.wd.api.dto.dpc.DpcDocumentDto;
import com.wd.api.dto.dpc.DpcDocumentScopeDto;
import com.wd.api.dto.dpc.DpcMasterCostSummaryDto;
import com.wd.api.dto.dpc.DpcPaymentMilestoneDto;
import com.wd.api.dto.dpc.DpcScopeOptionDto;
import com.wd.api.repository.CustomerProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Renders a DPC (Detailed Project Costing) document to a PDF byte array.
 *
 * <p>Pulls the assembled {@link DpcDocumentDto} from {@link DpcDocumentService},
 * builds a Thymeleaf {@link Context} populated with pre-formatted strings (so
 * the template doesn't have to fight Thymeleaf's number-formatting limits),
 * and runs the resulting HTML through openhtmltopdf — same pipeline as
 * {@code LeadQuotationService.generateQuotationPdf}.
 *
 * <p>All numeric formatting follows the Indian locale convention (e.g.
 * {@code 47,28,200}) — see {@link #formatINR(BigDecimal)}.
 */
@Service
@Transactional(readOnly = true)
public class DpcRenderService {

    private static final Logger log = LoggerFactory.getLogger(DpcRenderService.class);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private final DpcDocumentService dpcDocumentService;
    private final TemplateEngine templateEngine;
    private final CompanyInfoConfig companyInfoConfig;
    private final CustomerProjectRepository customerProjectRepository;

    public DpcRenderService(DpcDocumentService dpcDocumentService,
                            TemplateEngine templateEngine,
                            CompanyInfoConfig companyInfoConfig,
                            CustomerProjectRepository customerProjectRepository) {
        this.dpcDocumentService = dpcDocumentService;
        this.templateEngine = templateEngine;
        this.companyInfoConfig = companyInfoConfig;
        this.customerProjectRepository = customerProjectRepository;
    }

    /**
     * Render the DPC document with the given id to a PDF byte array.
     *
     * @param dpcDocumentId the DPC document primary key
     * @return PDF bytes (never null, never empty if rendering succeeds)
     * @throws RuntimeException if openhtmltopdf fails to render the produced HTML
     */
    public byte[] renderPdf(Long dpcDocumentId) {
        DpcDocumentDto dto = dpcDocumentService.getById(dpcDocumentId);
        // Touch the project header through the repo so the renderer has a
        // single source of truth even if the dto is later regenerated; we
        // currently rely entirely on the dto's projectName/Location/sqfeet.
        if (dto.projectId() != null) {
            customerProjectRepository.findById(dto.projectId()).ifPresent(p ->
                    log.debug("Rendering DPC {} for project {} ({})", dpcDocumentId, p.getId(), p.getName()));
        }

        Context context = new Context(Locale.ENGLISH);
        context.setVariable("dpc", dto);
        context.setVariable("company", companyInfoConfig);
        context.setVariable("currentDate", LocalDate.now().format(DATE_FMT));

        Map<String, String> fmt = buildFormattedNumbers(dto);
        context.setVariable("fmt", fmt);
        // Convenience: pre-formatted master totals.
        DpcMasterCostSummaryDto summary = dto.masterCostSummary();
        context.setVariable("formattedTotal",
                summary != null ? "INR " + formatINR(summary.totalCustomized()) : "INR 0");

        // Hand-rolled view-models so the template can iterate without
        // touching BigDecimal formatting.
        context.setVariable("scopesView", buildScopesView(dto));
        context.setVariable("masterView", buildMasterView(dto));
        context.setVariable("milestonesView", buildMilestonesView(dto));
        context.setVariable("customizationsView", buildCustomizationsView(dto));
        context.setVariable("totalPages", 16);
        context.setVariable("revisionLabel",
                dto.revisionNumber() != null ? String.format("%02d", dto.revisionNumber()) : "01");

        String html = templateEngine.process("dpc-template", context);

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            // Embed Arial regular + bold so Indian rupee glyph (U+20B9) renders.
            // openhtmltopdf bundles only the 14 PDF type-1 standard fonts which
            // lack ₹ — the cost cells need this.
            FSSupplier<InputStream> arial     = () -> openFontStream("fonts/Arial.ttf");
            FSSupplier<InputStream> arialBold = () -> openFontStream("fonts/Arial-Bold.ttf");
            builder.useFont(arial,     "Arial", 400, FontStyle.NORMAL, true);
            builder.useFont(arialBold, "Arial", 700, FontStyle.NORMAL, true);
            builder.withHtmlContent(html, "/");
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error rendering DPC PDF for document " + dpcDocumentId, e);
        }
    }

    /** Open a classpath font resource as an InputStream — wraps the checked
     *  IOException so it can be used inside an FSSupplier lambda. */
    private static InputStream openFontStream(String classPathResource) {
        try {
            return new ClassPathResource(classPathResource).getInputStream();
        } catch (IOException e) {
            throw new RuntimeException("DPC font missing on classpath: " + classPathResource, e);
        }
    }

    // ---- Formatting helpers ----

    /**
     * Format a non-null BigDecimal using Indian-locale grouping (e.g.
     * 4728200 -> {@code "47,28,200"}).  Null and zero collapse to {@code "0"}.
     *
     * <p>Implementation note: {@link java.text.DecimalFormat} with the pattern
     * {@code "##,##,###"} produces the right grouping for whole numbers
     * regardless of magnitude.  For decimals we round to whole rupees first —
     * the design never shows paise for DPC cost totals.
     */
    public static String formatINR(BigDecimal value) {
        if (value == null) return "0";
        BigDecimal rounded = value.setScale(0, RoundingMode.HALF_UP);
        long absVal = rounded.abs().longValueExact();
        String s = Long.toString(absVal);

        // Apply Indian grouping: last 3 digits, then groups of 2.
        StringBuilder out = new StringBuilder();
        int len = s.length();
        if (len <= 3) {
            out.append(s);
        } else {
            String last3 = s.substring(len - 3);
            String rest = s.substring(0, len - 3);
            // Group `rest` from the right in pairs of 2.
            StringBuilder restGrouped = new StringBuilder();
            int i = rest.length();
            while (i > 0) {
                int start = Math.max(0, i - 2);
                if (restGrouped.length() > 0) restGrouped.insert(0, ",");
                restGrouped.insert(0, rest.substring(start, i));
                i = start;
            }
            out.append(restGrouped).append(",").append(last3);
        }
        if (rounded.signum() < 0) out.insert(0, "-");
        return out.toString();
    }

    /** Format with the {@code INR } prefix (used by template for cost cells). */
    public static String formatINRWithSymbol(BigDecimal value) {
        return "INR " + formatINR(value);
    }

    /**
     * Like {@link #formatINR(BigDecimal)} but maps {@code null} to the em-dash
     * "—" used on the customer-facing PDF for "no value computable". Zero
     * stays as "0" (a real-but-empty scope is still a valid figure).
     */
    public static String formatINROrDash(BigDecimal value) {
        if (value == null) return "—";
        return formatINR(value);
    }

    /**
     * Format a built-up area value for the customer-facing PDF.
     *
     * <p>Both {@code null} and {@code 0} collapse to "—" — when the project
     * record carries no built-up area we never want to print "0 sqft" on a
     * handover document (regression: project 47 REV 01).
     */
    public static String formatSqftOrDash(BigDecimal value) {
        if (value == null || value.signum() <= 0) return "—";
        return formatINR(value.setScale(0, RoundingMode.HALF_UP));
    }

    // ---- View-model builders ----

    private Map<String, String> buildFormattedNumbers(DpcDocumentDto dto) {
        Map<String, String> fmt = new HashMap<>();
        DpcMasterCostSummaryDto s = dto.masterCostSummary();
        if (s != null) {
            fmt.put("totalOriginal", formatINR(s.totalOriginal()));
            fmt.put("totalCustomized", formatINR(s.totalCustomized()));
            fmt.put("totalVariance", formatINR(s.totalVariance()));
            fmt.put("originalPerSqft", formatINROrDash(s.originalPerSqft()));
            fmt.put("customizedPerSqft", formatINROrDash(s.customizedPerSqft()));
        }
        fmt.put("sqfeet", formatSqftOrDash(dto.sqfeet()));
        return fmt;
    }

    private List<Map<String, Object>> buildScopesView(DpcDocumentDto dto) {
        List<Map<String, Object>> view = new ArrayList<>();
        if (dto.scopes() == null) return view;

        BigDecimal sqft = dto.sqfeet();
        for (DpcDocumentScopeDto scope : dto.scopes()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", scope.id());
            row.put("scopeCode", scope.scopeCode());
            row.put("scopeTitle", scope.scopeTitle());
            row.put("includedInPdf", Boolean.TRUE.equals(scope.includedInPdf()));
            row.put("displayOrder", scope.displayOrder());
            row.put("selectedOptionId", scope.selectedOptionId());
            row.put("selectedOptionDisplayName", scope.selectedOptionDisplayName());
            row.put("selectedOptionRationale", scope.selectedOptionRationale());

            BigDecimal orig = scope.originalAmount();
            BigDecimal cust = scope.customizedAmount();
            row.put("originalAmount", formatINR(orig));
            row.put("customizedAmount", formatINR(cust));
            row.put("variance", formatINR(safeSubtract(cust, orig)));
            row.put("originalPerSqft", formatINROrDash(perSqft(orig, sqft)));
            row.put("customizedPerSqft", formatINROrDash(perSqft(cust, sqft)));

            // Brands as ordered entry list (keeps template loop simple).
            List<Map<String, String>> brands = new ArrayList<>();
            if (scope.brandsResolved() != null) {
                for (Map.Entry<String, String> e : scope.brandsResolved().entrySet()) {
                    Map<String, String> b = new LinkedHashMap<>();
                    b.put("key", e.getKey());
                    b.put("value", e.getValue());
                    brands.add(b);
                }
            }
            row.put("brands", brands);

            row.put("whatYouGet", scope.whatYouGetResolved() != null
                    ? scope.whatYouGetResolved() : List.of());

            // Options with `selected` flag baked in for easy `th:classappend`.
            List<Map<String, Object>> options = new ArrayList<>();
            if (scope.availableOptions() != null) {
                for (DpcScopeOptionDto opt : scope.availableOptions()) {
                    Map<String, Object> o = new LinkedHashMap<>();
                    o.put("id", opt.id());
                    o.put("code", opt.code());
                    o.put("displayName", opt.displayName());
                    o.put("imagePath", opt.imagePath());
                    o.put("selected", scope.selectedOptionId() != null
                            && scope.selectedOptionId().equals(opt.id()));
                    options.add(o);
                }
            }
            row.put("options", options);

            view.add(row);
        }
        return view;
    }

    private Map<String, Object> buildMasterView(DpcDocumentDto dto) {
        Map<String, Object> v = new LinkedHashMap<>();
        DpcMasterCostSummaryDto s = dto.masterCostSummary();
        List<Map<String, String>> rows = new ArrayList<>();
        if (s != null && s.scopes() != null) {
            for (DpcCostRollupDto r : s.scopes()) {
                Map<String, String> row = new LinkedHashMap<>();
                row.put("scopeCode", r.scopeCode() != null ? r.scopeCode() : "");
                row.put("scopeTitle", r.scopeTitle() != null ? r.scopeTitle() : "");
                row.put("originalAmount", formatINR(r.originalAmount()));
                row.put("customizedAmount", formatINR(r.customizedAmount()));
                row.put("variance", formatINR(r.variance()));
                rows.add(row);
            }
        }
        v.put("rows", rows);
        v.put("totalOriginal", s != null ? formatINR(s.totalOriginal()) : "0");
        v.put("totalCustomized", s != null ? formatINR(s.totalCustomized()) : "0");
        v.put("totalVariance", s != null ? formatINR(s.totalVariance()) : "0");
        v.put("originalPerSqft", s != null ? formatINROrDash(s.originalPerSqft()) : "—");
        v.put("customizedPerSqft", s != null ? formatINROrDash(s.customizedPerSqft()) : "—");
        return v;
    }

    private List<Map<String, Object>> buildMilestonesView(DpcDocumentDto dto) {
        List<Map<String, Object>> view = new ArrayList<>();
        if (dto.paymentMilestones() == null) return view;
        for (DpcPaymentMilestoneDto m : dto.paymentMilestones()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("stageNumber", m.stageNumber());
            row.put("stageName", m.stageName());
            row.put("milestoneDescription", m.milestoneDescription() != null ? m.milestoneDescription() : "");
            row.put("stagePercentage", m.stagePercentage() != null
                    ? m.stagePercentage().setScale(0, RoundingMode.HALF_UP).toPlainString() : "0");
            row.put("cumulativePercentage", m.cumulativePercentage() != null
                    ? m.cumulativePercentage().setScale(0, RoundingMode.HALF_UP).toPlainString() : "0");
            row.put("stageAmountInclGst", formatINR(m.stageAmountInclGst()));
            // Bar width as integer percent for the cumulative cash-flow chart
            // (template uses inline style="width: Npx" or "width: N%").
            int cumWidth = m.cumulativePercentage() != null
                    ? m.cumulativePercentage().setScale(0, RoundingMode.HALF_UP).intValue()
                    : 0;
            row.put("cumulativeBarWidth", Math.max(0, Math.min(100, cumWidth)));
            view.add(row);
        }
        return view;
    }

    private List<Map<String, Object>> buildCustomizationsView(DpcDocumentDto dto) {
        List<Map<String, Object>> view = new ArrayList<>();
        if (dto.customizationLines() == null) return view;
        for (DpcCustomizationLineDto line : dto.customizationLines()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("title", line.title() != null ? line.title() : "");
            row.put("description", line.description() != null ? line.description() : "");
            row.put("amount", formatINR(line.amount()));
            row.put("source", line.source() != null ? line.source() : "");
            view.add(row);
        }
        return view;
    }

    /**
     * Compute per-sqft rate. Returns {@code null} (not zero) when the project
     * has no built-up area on file — callers pair this with
     * {@link #formatINROrDash(BigDecimal)} so the PDF shows "—" instead of
     * a misleading "0".
     */
    private static BigDecimal perSqft(BigDecimal amount, BigDecimal sqft) {
        if (amount == null || sqft == null || sqft.signum() <= 0) {
            return null;
        }
        return amount.divide(sqft, 0, RoundingMode.HALF_UP);
    }

    private static BigDecimal safeSubtract(BigDecimal a, BigDecimal b) {
        BigDecimal aa = a != null ? a : BigDecimal.ZERO;
        BigDecimal bb = b != null ? b : BigDecimal.ZERO;
        return aa.subtract(bb);
    }
}
