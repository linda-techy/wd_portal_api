package com.wd.api.estimation.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.wd.api.estimation.domain.enums.EstimationPricingMode;
import com.wd.api.estimation.dto.EstimationSubResourceResponse;
import com.wd.api.estimation.dto.LeadEstimationDetailResponse;
import com.wd.api.estimation.dto.LineItemDto;
import com.wd.api.estimation.util.IndianNumberFormatter;
import com.wd.api.model.Lead;
import com.wd.api.repository.LeadRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class EstimationPdfService {

    private final LeadEstimationService leadEstimationService;
    private final LeadRepository leadRepo;

    // Lazy-init: built once on first PDF and reused. Embeds Arial which has the
    // ₹ (Indian Rupee) glyph at U+20B9; the built-in Helvetica that ships with
    // OpenPDF does not, so without these a missing-glyph box renders next to every
    // amount.
    private BaseFont _baseFontRegular;
    private BaseFont _baseFontBold;

    public EstimationPdfService(LeadEstimationService leadEstimationService,
                                 LeadRepository leadRepo) {
        this.leadEstimationService = leadEstimationService;
        this.leadRepo = leadRepo;
    }

    private synchronized BaseFont baseFontRegular() {
        if (_baseFontRegular == null) {
            _baseFontRegular = loadEmbeddedFont("fonts/Arial.ttf");
        }
        return _baseFontRegular;
    }

    private synchronized BaseFont baseFontBold() {
        if (_baseFontBold == null) {
            _baseFontBold = loadEmbeddedFont("fonts/Arial-Bold.ttf");
        }
        return _baseFontBold;
    }

    private BaseFont loadEmbeddedFont(String classpathLocation) {
        try {
            byte[] bytes = new ClassPathResource(classpathLocation).getInputStream().readAllBytes();
            return BaseFont.createFont(classpathLocation, BaseFont.IDENTITY_H, BaseFont.EMBEDDED,
                    BaseFont.CACHED, bytes, null);
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Failed to load PDF font: " + classpathLocation, e);
        }
    }

    private Font font(float size, boolean bold) {
        return new Font(bold ? baseFontBold() : baseFontRegular(), size);
    }

    public byte[] generatePdf(UUID estimationId) {
        LeadEstimationDetailResponse detail = leadEstimationService.get(estimationId);
        Lead lead = leadRepo.findById(detail.leadId()).orElse(null);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36, 36, 54, 36);
        try {
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // ----------------------------------------------------------------
            // Header
            // ----------------------------------------------------------------
            doc.add(new Paragraph("Walldot Builders", font(18, true)));
            doc.add(new Paragraph(
                    "Estimation " + detail.estimationNo() + "  \u00b7  " + detail.status(),
                    font(11, false)));
            doc.add(new Paragraph(
                    "Generated " + LocalDate.now() + "  \u00b7  Valid until " + detail.validUntil(),
                    font(11, false)));
            doc.add(new Paragraph(" "));

            // ----------------------------------------------------------------
            // Lead info block
            // ----------------------------------------------------------------
            if (lead != null) {
                doc.add(new Paragraph("Lead: " + lead.getName(), font(11, false)));
            }
            doc.add(new Paragraph("Project type: " + detail.projectType(), font(11, false)));
            doc.add(new Paragraph(" "));

            boolean isBudgetary = detail.pricingMode() == EstimationPricingMode.BUDGETARY;

            if (isBudgetary) {
                // ------------------------------------------------------------
                // Budgetary mode: estimated area + range; no line items.
                // ------------------------------------------------------------
                doc.add(new Paragraph("Budgetary estimate", font(13, true)));
                doc.add(new Paragraph(
                        "Estimated buildable area: "
                                + (detail.estimatedAreaSqft() != null
                                        ? IndianNumberFormatter.formatWholeRupee(detail.estimatedAreaSqft())
                                        : "\u2014")
                                + " sqft",
                        font(11, false)));
                doc.add(new Paragraph(" "));
            } else {
                // ----------------------------------------------------------------
                // Line items table
                // ----------------------------------------------------------------
                doc.add(new Paragraph("Line items", font(13, true)));
                PdfPTable lineItemsTable = new PdfPTable(new float[]{4, 1, 1, 1.5f, 1.5f});
                lineItemsTable.setWidthPercentage(100);
                for (String h : new String[]{"Description", "Qty", "Unit", "Rate", "Amount"}) {
                    PdfPCell cell = new PdfPCell(new Phrase(h, font(10, true)));
                    cell.setBackgroundColor(new Color(220, 220, 220));
                    lineItemsTable.addCell(cell);
                }
                for (LineItemDto li : detail.lineItems()) {
                    lineItemsTable.addCell(new PdfPCell(new Phrase(li.description(), font(10, false))));
                    lineItemsTable.addCell(new PdfPCell(new Phrase(
                            li.quantity() != null ? IndianNumberFormatter.formatWithPaisa(li.quantity()) : "", font(10, false))));
                    lineItemsTable.addCell(new PdfPCell(new Phrase(
                            li.unit() != null ? li.unit() : "", font(10, false))));
                    lineItemsTable.addCell(new PdfPCell(new Phrase(
                            li.unitRate() != null ? "\u20b9" + IndianNumberFormatter.formatWithPaisa(li.unitRate()) : "", font(10, false))));
                    lineItemsTable.addCell(new PdfPCell(new Phrase(
                            "\u20b9" + IndianNumberFormatter.formatWithPaisa(li.amount()), font(10, false))));
                }
                doc.add(lineItemsTable);
                doc.add(new Paragraph(" "));
            }

            // ----------------------------------------------------------------
            // Sub-resource sections (only if non-empty)
            // ----------------------------------------------------------------
            renderSubResources(doc, "Inclusions", detail.inclusions());
            renderSubResources(doc, "Exclusions", detail.exclusions());
            renderSubResources(doc, "Assumptions", detail.assumptions());
            renderPaymentMilestones(doc, detail.paymentMilestones());

            // ----------------------------------------------------------------
            // Totals block — branched by mode.
            // ----------------------------------------------------------------
            if (isBudgetary) {
                String low = detail.grandTotalMin() != null
                        ? IndianNumberFormatter.formatWithPaisa(detail.grandTotalMin()) : "\u2014";
                String high = detail.grandTotalMax() != null
                        ? IndianNumberFormatter.formatWithPaisa(detail.grandTotalMax()) : "\u2014";
                doc.add(new Paragraph(
                        "Estimated range (incl. GST): \u20b9" + low + "  \u2013  \u20b9" + high,
                        font(14, true)));
                doc.add(new Paragraph(
                        "Range = (area \u00d7 base rate) \u00b110%, then GST applied. "
                                + "Final figure available after detailed estimate.",
                        font(9, false)));
            } else {
                doc.add(new Paragraph(
                        "Subtotal: \u20b9" + IndianNumberFormatter.formatWithPaisa(detail.subtotal()),
                        font(11, false)));
                doc.add(new Paragraph(
                        "Discount: \u20b9" + IndianNumberFormatter.formatWithPaisa(detail.discountAmount()),
                        font(11, false)));
                doc.add(new Paragraph(
                        "GST: \u20b9" + IndianNumberFormatter.formatWithPaisa(detail.gstAmount()),
                        font(11, false)));
                doc.add(new Paragraph(
                        "Grand total: \u20b9" + IndianNumberFormatter.formatWithPaisa(detail.grandTotal()),
                        font(14, true)));
            }

            doc.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
        return baos.toByteArray();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void renderSubResources(Document doc, String title,
                                     List<EstimationSubResourceResponse> items)
            throws DocumentException {
        if (items == null || items.isEmpty()) return;
        doc.add(new Paragraph(title, font(13, true)));
        for (EstimationSubResourceResponse item : items) {
            String line = "  \u2022 " + item.label();
            if (item.description() != null && !item.description().isBlank()) {
                line += " \u2014 " + item.description();
            }
            doc.add(new Paragraph(line, font(11, false)));
        }
        doc.add(new Paragraph(" "));
    }

    private void renderPaymentMilestones(Document doc,
                                          List<EstimationSubResourceResponse> items)
            throws DocumentException {
        if (items == null || items.isEmpty()) return;
        doc.add(new Paragraph("Payment milestones", font(13, true)));
        PdfPTable t = new PdfPTable(new float[]{1, 4, 1.5f});
        t.setWidthPercentage(60);
        for (String h : new String[]{"#", "Milestone", "%"}) {
            PdfPCell cell = new PdfPCell(new Phrase(h, font(10, true)));
            t.addCell(cell);
        }
        int i = 1;
        for (EstimationSubResourceResponse m : items) {
            t.addCell(new PdfPCell(new Phrase(String.valueOf(i++), font(10, false))));
            t.addCell(new PdfPCell(new Phrase(m.label(), font(10, false))));
            t.addCell(new PdfPCell(new Phrase(
                    m.percentage() != null ? m.percentage().toPlainString() + "%" : "",
                    font(10, false))));
        }
        doc.add(t);
        doc.add(new Paragraph(" "));
    }
}
