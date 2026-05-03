package com.wd.api.estimation.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.wd.api.estimation.domain.enums.EstimationPricingMode;
import com.wd.api.estimation.dto.EstimationSubResourceResponse;
import com.wd.api.estimation.dto.LeadEstimationDetailResponse;
import com.wd.api.estimation.dto.LineItemDto;
import com.wd.api.model.Lead;
import com.wd.api.repository.LeadRepository;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class EstimationPdfService {

    private final LeadEstimationService leadEstimationService;
    private final LeadRepository leadRepo;

    public EstimationPdfService(LeadEstimationService leadEstimationService,
                                 LeadRepository leadRepo) {
        this.leadEstimationService = leadEstimationService;
        this.leadRepo = leadRepo;
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
            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            doc.add(new Paragraph("Walldot Builders", titleFont));
            doc.add(new Paragraph(
                    "Estimation " + detail.estimationNo() + "  ·  " + detail.status()));
            doc.add(new Paragraph(
                    "Generated " + LocalDate.now() + "  ·  Valid until " + detail.validUntil()));
            doc.add(new Paragraph(" "));

            // ----------------------------------------------------------------
            // Lead info block
            // ----------------------------------------------------------------
            if (lead != null) {
                doc.add(new Paragraph("Lead: " + lead.getName()));
            }
            doc.add(new Paragraph("Project type: " + detail.projectType()));
            doc.add(new Paragraph(" "));

            boolean isBudgetary = detail.pricingMode() == EstimationPricingMode.BUDGETARY;

            if (isBudgetary) {
                // ------------------------------------------------------------
                // Budgetary mode: estimated area + range; no line items.
                // ------------------------------------------------------------
                doc.add(new Paragraph("Budgetary estimate",
                        new Font(Font.HELVETICA, 13, Font.BOLD)));
                doc.add(new Paragraph(
                        "Estimated buildable area: "
                                + (detail.estimatedAreaSqft() != null
                                        ? detail.estimatedAreaSqft().toPlainString()
                                        : "—")
                                + " sqft"));
                doc.add(new Paragraph(" "));
            } else {
                // ----------------------------------------------------------------
                // Line items table
                // ----------------------------------------------------------------
                doc.add(new Paragraph("Line items", new Font(Font.HELVETICA, 13, Font.BOLD)));
                PdfPTable lineItemsTable = new PdfPTable(new float[]{4, 1, 1, 1.5f, 1.5f});
                lineItemsTable.setWidthPercentage(100);
                for (String h : new String[]{"Description", "Qty", "Unit", "Rate", "Amount"}) {
                    PdfPCell cell = new PdfPCell(new Phrase(h, new Font(Font.HELVETICA, 10, Font.BOLD)));
                    cell.setBackgroundColor(new Color(220, 220, 220));
                    lineItemsTable.addCell(cell);
                }
                for (LineItemDto li : detail.lineItems()) {
                    lineItemsTable.addCell(li.description());
                    lineItemsTable.addCell(li.quantity() != null ? li.quantity().toPlainString() : "");
                    lineItemsTable.addCell(li.unit() != null ? li.unit() : "");
                    lineItemsTable.addCell(li.unitRate() != null ? li.unitRate().toPlainString() : "");
                    lineItemsTable.addCell(li.amount().toPlainString());
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
                        ? detail.grandTotalMin().toPlainString() : "—";
                String high = detail.grandTotalMax() != null
                        ? detail.grandTotalMax().toPlainString() : "—";
                doc.add(new Paragraph(
                        "Estimated range (incl. GST): \u20b9" + low + "  \u2013  \u20b9" + high,
                        new Font(Font.HELVETICA, 14, Font.BOLD)));
                doc.add(new Paragraph(
                        "Range = (area \u00d7 base rate) \u00b110%, then GST applied. "
                                + "Final figure available after detailed estimate.",
                        new Font(Font.HELVETICA, 9, Font.ITALIC)));
            } else {
                doc.add(new Paragraph("Subtotal: \u20b9"
                        + (detail.subtotal() != null ? detail.subtotal().toPlainString() : "0")));
                doc.add(new Paragraph("Discount: \u20b9"
                        + (detail.discountAmount() != null ? detail.discountAmount().toPlainString() : "0")));
                doc.add(new Paragraph("GST: \u20b9"
                        + (detail.gstAmount() != null ? detail.gstAmount().toPlainString() : "0")));
                doc.add(new Paragraph(
                        "Grand total: \u20b9" + detail.grandTotal().toPlainString(),
                        new Font(Font.HELVETICA, 14, Font.BOLD)));
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
        doc.add(new Paragraph(title, new Font(Font.HELVETICA, 13, Font.BOLD)));
        for (EstimationSubResourceResponse item : items) {
            String line = "  \u2022 " + item.label();
            if (item.description() != null && !item.description().isBlank()) {
                line += " \u2014 " + item.description();
            }
            doc.add(new Paragraph(line));
        }
        doc.add(new Paragraph(" "));
    }

    private void renderPaymentMilestones(Document doc,
                                          List<EstimationSubResourceResponse> items)
            throws DocumentException {
        if (items == null || items.isEmpty()) return;
        doc.add(new Paragraph("Payment milestones", new Font(Font.HELVETICA, 13, Font.BOLD)));
        PdfPTable t = new PdfPTable(new float[]{1, 4, 1.5f});
        t.setWidthPercentage(60);
        for (String h : new String[]{"#", "Milestone", "%"}) {
            PdfPCell cell = new PdfPCell(new Phrase(h, new Font(Font.HELVETICA, 10, Font.BOLD)));
            t.addCell(cell);
        }
        int i = 1;
        for (EstimationSubResourceResponse m : items) {
            t.addCell(String.valueOf(i++));
            t.addCell(m.label());
            t.addCell(m.percentage() != null ? m.percentage().toPlainString() + "%" : "");
        }
        doc.add(t);
        doc.add(new Paragraph(" "));
    }
}
