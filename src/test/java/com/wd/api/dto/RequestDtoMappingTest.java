package com.wd.api.dto;

import com.wd.api.model.BoqItem;
import com.wd.api.model.CctvCamera;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.DelayLog;
import com.wd.api.model.DesignPackageTemplate;
import com.wd.api.model.Lead;
import com.wd.api.model.LeadInteraction;
import com.wd.api.model.MaterialIndent;
import com.wd.api.model.MaterialIndentItem;
import com.wd.api.model.MilestoneTemplate;
import com.wd.api.model.PortalUser;
import com.wd.api.model.ProjectInvoice;
import com.wd.api.model.ProjectMilestone;
import com.wd.api.model.ProjectPhase;
import com.wd.api.model.ProjectVariation;
import com.wd.api.model.ProjectWarranty;
import com.wd.api.model.PurchaseInvoice;
import com.wd.api.model.PurchaseOrder;
import com.wd.api.model.PurchaseOrderItem;
import com.wd.api.model.QualityCheck;
import com.wd.api.model.Receipt;
import com.wd.api.model.RetentionRelease;
import com.wd.api.model.SubcontractMeasurement;
import com.wd.api.model.SubcontractPayment;
import com.wd.api.model.SubcontractWorkOrder;
import com.wd.api.model.Task;
import com.wd.api.model.VendorPayment;
import com.wd.api.model.VendorQuotation;
import com.wd.api.model.enums.PurchaseOrderStatus;
import com.wd.api.model.enums.StreamProtocol;
import com.wd.api.model.enums.VariationStatus;
import com.wd.api.model.enums.WarrantyStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plain-POJO unit tests for the {@code toEntity()} mapping of the 18 request-DTO
 * records under {@code com.wd.api.dto}. No Spring context — these only exercise
 * record construction, accessors and the field-by-field copy into the JPA entity.
 *
 * <p>Each test builds the record with non-null sample values for every component,
 * maps to the entity, and asserts a representative set of scalar getters plus the
 * identity of entity-typed / collection components.
 */
class RequestDtoMappingTest {

    private static final LocalDate D = LocalDate.of(2026, 1, 1);
    private static final LocalDateTime DT = LocalDateTime.of(2026, 1, 1, 9, 30);

    @Test
    void cctvCameraRequest_mapsAllFields() {
        CctvCameraRequest req = new CctvCameraRequest(
                "Front Gate", "Main Entrance", "Hikvision", StreamProtocol.HLS,
                "http://cam/stream.m3u8", "http://cam/snap.jpg", "admin", "secret",
                554, Boolean.TRUE, "1080p", D, 1);

        CctvCamera e = req.toEntity();

        assertThat(e).isNotNull();
        assertThat(e.getCameraName()).isEqualTo("Front Gate");
        assertThat(e.getLocation()).isEqualTo("Main Entrance");
        assertThat(e.getProvider()).isEqualTo("Hikvision");
        assertThat(e.getStreamProtocol()).isEqualTo(StreamProtocol.HLS);
        assertThat(e.getStreamUrl()).isEqualTo("http://cam/stream.m3u8");
        assertThat(e.getSnapshotUrl()).isEqualTo("http://cam/snap.jpg");
        assertThat(e.getUsername()).isEqualTo("admin");
        assertThat(e.getPassword()).isEqualTo("secret");
        assertThat(e.getPort()).isEqualTo(554);
        assertThat(e.getIsActive()).isTrue();
        assertThat(e.getResolution()).isEqualTo("1080p");
        assertThat(e.getInstallationDate()).isEqualTo(D);
        assertThat(e.getDisplayOrder()).isEqualTo(1);
    }

    @Test
    void delayLogRequest_mapsAllFields() {
        ProjectPhase phase = new ProjectPhase();
        DelayLogRequest req = new DelayLogRequest(
                phase, "WEATHER", D, D.plusDays(3), "Heavy rain", "WEATHER",
                "CLIENT", 3, "Slab postponed", true, "Rain delay", "Handover +3d");

        DelayLog e = req.toEntity();

        assertThat(e).isNotNull();
        assertThat(e.getPhase()).isSameAs(phase);
        assertThat(e.getDelayType()).isEqualTo("WEATHER");
        assertThat(e.getFromDate()).isEqualTo(D);
        assertThat(e.getToDate()).isEqualTo(D.plusDays(3));
        assertThat(e.getReasonText()).isEqualTo("Heavy rain");
        assertThat(e.getReasonCategory()).isEqualTo("WEATHER");
        assertThat(e.getResponsibleParty()).isEqualTo("CLIENT");
        assertThat(e.getDurationDays()).isEqualTo(3);
        assertThat(e.getImpactDescription()).isEqualTo("Slab postponed");
        assertThat(e.isCustomerVisible()).isTrue();
        assertThat(e.getCustomerSummary()).isEqualTo("Rain delay");
        assertThat(e.getImpactOnHandover()).isEqualTo("Handover +3d");
    }

    @Test
    void designPackageTemplateRequest_mapsAllFields() {
        DesignPackageTemplateRequest req = new DesignPackageTemplateRequest(
                "PKG-A", "Premium", "Best value", "Full design pack",
                BigDecimal.valueOf(120), BigDecimal.valueOf(5), 3, "3D,2D", 2, Boolean.TRUE);

        DesignPackageTemplate e = req.toEntity();

        assertThat(e).isNotNull();
        assertThat(e.getCode()).isEqualTo("PKG-A");
        assertThat(e.getName()).isEqualTo("Premium");
        assertThat(e.getTagline()).isEqualTo("Best value");
        assertThat(e.getDescription()).isEqualTo("Full design pack");
        assertThat(e.getRatePerSqft()).isEqualByComparingTo(BigDecimal.valueOf(120));
        assertThat(e.getFullPaymentDiscountPct()).isEqualByComparingTo(BigDecimal.valueOf(5));
        assertThat(e.getRevisionsIncluded()).isEqualTo(3);
        assertThat(e.getFeatures()).isEqualTo("3D,2D");
        assertThat(e.getDisplayOrder()).isEqualTo(2);
        assertThat(e.getIsActive()).isTrue();
    }

    @Test
    void leadInteractionRequest_mapsAllFields() {
        LeadInteractionRequest req = new LeadInteractionRequest(
                7L, "CALL", DT, 15, "Follow up", "Discussed quote", "POSITIVE",
                "Send proposal", DT.plusDays(2), "Phone", "{\"k\":\"v\"}");

        LeadInteraction e = req.toEntity();

        assertThat(e).isNotNull();
        assertThat(e.getLeadId()).isEqualTo(7L);
        assertThat(e.getInteractionType()).isEqualTo("CALL");
        assertThat(e.getInteractionDate()).isEqualTo(DT);
        assertThat(e.getDurationMinutes()).isEqualTo(15);
        assertThat(e.getSubject()).isEqualTo("Follow up");
        assertThat(e.getNotes()).isEqualTo("Discussed quote");
        assertThat(e.getOutcome()).isEqualTo("POSITIVE");
        assertThat(e.getNextAction()).isEqualTo("Send proposal");
        assertThat(e.getNextActionDate()).isEqualTo(DT.plusDays(2));
        assertThat(e.getLocation()).isEqualTo("Phone");
        assertThat(e.getMetadata()).isEqualTo("{\"k\":\"v\"}");
    }

    @Test
    void materialIndentRequest_mapsAllFields() {
        MaterialIndentItem item = new MaterialIndentItem();
        List<MaterialIndentItem> items = List.of(item);
        MaterialIndentRequest req = new MaterialIndentRequest(
                "IND-001", D, D.plusDays(5), MaterialIndent.IndentPriority.LOW, "Urgent", items);

        MaterialIndent e = req.toEntity();

        assertThat(e).isNotNull();
        assertThat(e.getIndentNumber()).isEqualTo("IND-001");
        assertThat(e.getRequestDate()).isEqualTo(D);
        assertThat(e.getRequiredDate()).isEqualTo(D.plusDays(5));
        assertThat(e.getPriority()).isEqualTo(MaterialIndent.IndentPriority.LOW);
        assertThat(e.getNotes()).isEqualTo("Urgent");
        assertThat(e.getItems()).isSameAs(items);
    }

    @Test
    void projectMilestoneRequest_mapsAllFields() {
        CustomerProject project = new CustomerProject();
        MilestoneTemplate template = new MilestoneTemplate();
        ProjectMilestoneRequest req = new ProjectMilestoneRequest(
                project, "Foundation", "Foundation stage", BigDecimal.valueOf(10),
                BigDecimal.valueOf(100000), "PENDING", D, D.plusDays(30), template,
                BigDecimal.valueOf(50), BigDecimal.valueOf(20), "MANUAL",
                D.plusDays(1), D.plusDays(29));

        ProjectMilestone e = req.toEntity();

        assertThat(e).isNotNull();
        assertThat(e.getProject()).isSameAs(project);
        assertThat(e.getName()).isEqualTo("Foundation");
        assertThat(e.getDescription()).isEqualTo("Foundation stage");
        assertThat(e.getMilestonePercentage()).isEqualByComparingTo(BigDecimal.valueOf(10));
        assertThat(e.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100000));
        assertThat(e.getStatus()).isEqualTo("PENDING");
        assertThat(e.getDueDate()).isEqualTo(D);
        assertThat(e.getCompletedDate()).isEqualTo(D.plusDays(30));
        assertThat(e.getTemplate()).isSameAs(template);
        assertThat(e.getCompletionPercentage()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(e.getWeightPercentage()).isEqualByComparingTo(BigDecimal.valueOf(20));
        assertThat(e.getProgressSource()).isEqualTo("MANUAL");
        assertThat(e.getActualStartDate()).isEqualTo(D.plusDays(1));
        assertThat(e.getActualEndDate()).isEqualTo(D.plusDays(29));
    }

    @Test
    void projectVariationRequest_mapsAllFields() {
        ProjectVariationRequest req = new ProjectVariationRequest(
                "Extra room", BigDecimal.valueOf(50000), Boolean.TRUE, VariationStatus.DRAFT,
                "Client request", BigDecimal.valueOf(45000), 5, "N/A");

        ProjectVariation e = req.toEntity();

        assertThat(e).isNotNull();
        assertThat(e.getDescription()).isEqualTo("Extra room");
        assertThat(e.getEstimatedAmount()).isEqualByComparingTo(BigDecimal.valueOf(50000));
        assertThat(e.getClientApproved()).isTrue();
        assertThat(e.getStatus()).isEqualTo(VariationStatus.DRAFT);
        assertThat(e.getNotes()).isEqualTo("Client request");
        assertThat(e.getCostImpact()).isEqualByComparingTo(BigDecimal.valueOf(45000));
        assertThat(e.getTimeImpactWorkingDays()).isEqualTo(5);
        assertThat(e.getRejectionReason()).isEqualTo("N/A");
    }

    @Test
    void projectWarrantyRequest_mapsAllFields() {
        ProjectWarrantyRequest req = new ProjectWarrantyRequest(
                "Waterproofing", "5yr warranty", "Dr Fixit", D, D.plusYears(5),
                WarrantyStatus.ACTIVE, "Covers leaks");

        ProjectWarranty e = req.toEntity();

        assertThat(e).isNotNull();
        assertThat(e.getComponentName()).isEqualTo("Waterproofing");
        assertThat(e.getDescription()).isEqualTo("5yr warranty");
        assertThat(e.getProviderName()).isEqualTo("Dr Fixit");
        assertThat(e.getStartDate()).isEqualTo(D);
        assertThat(e.getEndDate()).isEqualTo(D.plusYears(5));
        assertThat(e.getStatus()).isEqualTo(WarrantyStatus.ACTIVE);
        assertThat(e.getCoverageDetails()).isEqualTo("Covers leaks");
    }

    @Test
    void purchaseOrderRequest_mapsAllFields() {
        MaterialIndent indent = new MaterialIndent();
        VendorQuotation quotation = new VendorQuotation();
        PurchaseOrderItem item = new PurchaseOrderItem();
        List<PurchaseOrderItem> items = List.of(item);
        PurchaseOrderRequest req = new PurchaseOrderRequest(
                "PO-001", indent, quotation, D, D.plusDays(10), BigDecimal.valueOf(118000),
                BigDecimal.valueOf(18000), BigDecimal.valueOf(100000), PurchaseOrderStatus.DRAFT,
                "Deliver to site", items);

        PurchaseOrder e = req.toEntity();

        assertThat(e).isNotNull();
        assertThat(e.getPoNumber()).isEqualTo("PO-001");
        assertThat(e.getIndent()).isSameAs(indent);
        assertThat(e.getQuotation()).isSameAs(quotation);
        assertThat(e.getPoDate()).isEqualTo(D);
        assertThat(e.getExpectedDeliveryDate()).isEqualTo(D.plusDays(10));
        assertThat(e.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(118000));
        assertThat(e.getGstAmount()).isEqualByComparingTo(BigDecimal.valueOf(18000));
        assertThat(e.getNetAmount()).isEqualByComparingTo(BigDecimal.valueOf(100000));
        assertThat(e.getStatus()).isEqualTo(PurchaseOrderStatus.DRAFT);
        assertThat(e.getNotes()).isEqualTo("Deliver to site");
        assertThat(e.getItems()).isSameAs(items);
    }

    @Test
    void qualityCheckRequest_mapsAllFields() {
        QualityCheckRequest req = new QualityCheckRequest(
                "Slab check", "Check concrete", DT, "PASS", "OK", "No issues");

        QualityCheck e = req.toEntity();

        assertThat(e).isNotNull();
        assertThat(e.getTitle()).isEqualTo("Slab check");
        assertThat(e.getDescription()).isEqualTo("Check concrete");
        assertThat(e.getCheckDate()).isEqualTo(DT);
        assertThat(e.getStatus()).isEqualTo("PASS");
        assertThat(e.getResult()).isEqualTo("OK");
        assertThat(e.getRemarks()).isEqualTo("No issues");
    }

    @Test
    void receiptRequest_mapsAllFields() {
        CustomerProject project = new CustomerProject();
        ProjectInvoice invoice = new ProjectInvoice();
        ReceiptRequest req = new ReceiptRequest(
                project, invoice, "RCT-001", BigDecimal.valueOf(25000), D, "UPI",
                "TXN123", "Advance");

        Receipt e = req.toEntity();

        assertThat(e).isNotNull();
        assertThat(e.getProject()).isSameAs(project);
        assertThat(e.getInvoice()).isSameAs(invoice);
        assertThat(e.getReceiptNumber()).isEqualTo("RCT-001");
        assertThat(e.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(25000));
        assertThat(e.getPaymentDate()).isEqualTo(D);
        assertThat(e.getPaymentMethod()).isEqualTo("UPI");
        assertThat(e.getTransactionReference()).isEqualTo("TXN123");
        assertThat(e.getNotes()).isEqualTo("Advance");
    }

    @Test
    void retentionReleaseRequest_mapsAllFields() {
        SubcontractWorkOrder workOrder = new SubcontractWorkOrder();
        RetentionReleaseRequest req = new RetentionReleaseRequest(
                workOrder, D, BigDecimal.valueOf(5000), "Final release");

        RetentionRelease e = req.toEntity();

        assertThat(e).isNotNull();
        assertThat(e.getWorkOrder()).isSameAs(workOrder);
        assertThat(e.getReleaseDate()).isEqualTo(D);
        assertThat(e.getAmountReleased()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(e.getNotes()).isEqualTo("Final release");
    }

    @Test
    void subcontractMeasurementRequest_mapsAllFields() {
        SubcontractMeasurementRequest req = new SubcontractMeasurementRequest(
                D, "Brickwork", BigDecimal.valueOf(100), "SQM", BigDecimal.valueOf(45), "BILL-1");

        SubcontractMeasurement e = req.toEntity();

        assertThat(e).isNotNull();
        assertThat(e.getMeasurementDate()).isEqualTo(D);
        assertThat(e.getDescription()).isEqualTo("Brickwork");
        assertThat(e.getQuantity()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(e.getUnit()).isEqualTo("SQM");
        assertThat(e.getRate()).isEqualByComparingTo(BigDecimal.valueOf(45));
        assertThat(e.getBillNumber()).isEqualTo("BILL-1");
    }

    @Test
    void subcontractPaymentRequest_mapsAllFields() {
        SubcontractPaymentRequest req = new SubcontractPaymentRequest(
                D, BigDecimal.valueOf(20000), BigDecimal.valueOf(2), "NEFT");

        SubcontractPayment e = req.toEntity();

        assertThat(e).isNotNull();
        assertThat(e.getPaymentDate()).isEqualTo(D);
        assertThat(e.getGrossAmount()).isEqualByComparingTo(BigDecimal.valueOf(20000));
        assertThat(e.getTdsPercentage()).isEqualByComparingTo(BigDecimal.valueOf(2));
        assertThat(e.getPaymentMode()).isEqualTo("NEFT");
    }

    @Test
    void subcontractWorkOrderRequest_mapsAllFields() {
        BoqItem boqItem = new BoqItem();
        SubcontractWorkOrderRequest req = new SubcontractWorkOrderRequest(
                "WO-001", boqItem, "Plastering", SubcontractWorkOrder.MeasurementBasis.UNIT_RATE,
                BigDecimal.valueOf(80000), "SQM", BigDecimal.valueOf(50), D, D.plusDays(20),
                D.plusDays(18), "30 days credit", BigDecimal.valueOf(5));

        SubcontractWorkOrder e = req.toEntity();

        assertThat(e).isNotNull();
        assertThat(e.getWorkOrderNumber()).isEqualTo("WO-001");
        assertThat(e.getBoqItem()).isSameAs(boqItem);
        assertThat(e.getScopeDescription()).isEqualTo("Plastering");
        assertThat(e.getMeasurementBasis()).isEqualTo(SubcontractWorkOrder.MeasurementBasis.UNIT_RATE);
        assertThat(e.getNegotiatedAmount()).isEqualByComparingTo(BigDecimal.valueOf(80000));
        assertThat(e.getUnit()).isEqualTo("SQM");
        assertThat(e.getRate()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(e.getStartDate()).isEqualTo(D);
        assertThat(e.getTargetCompletionDate()).isEqualTo(D.plusDays(20));
        assertThat(e.getActualCompletionDate()).isEqualTo(D.plusDays(18));
        assertThat(e.getPaymentTerms()).isEqualTo("30 days credit");
        assertThat(e.getRetentionPercentage()).isEqualByComparingTo(BigDecimal.valueOf(5));
    }

    @Test
    void taskRequest_mapsAllFields() {
        CustomerProject project = new CustomerProject();
        Lead lead = new Lead();
        TaskRequest req = new TaskRequest(
                "Pour slab", "Ground floor slab", Task.TaskStatus.PENDING, Task.TaskPriority.LOW,
                project, lead, D, D.plusDays(1), D.plusDays(2), 25, Boolean.TRUE, 99L,
                D.plusDays(3), D.plusDays(4), D, D, D, D, 2, Boolean.TRUE, Boolean.TRUE,
                "Not started", 10, 3);

        Task e = req.toEntity();

        assertThat(e).isNotNull();
        assertThat(e.getTitle()).isEqualTo("Pour slab");
        assertThat(e.getDescription()).isEqualTo("Ground floor slab");
        assertThat(e.getStatus()).isEqualTo(Task.TaskStatus.PENDING);
        assertThat(e.getPriority()).isEqualTo(Task.TaskPriority.LOW);
        assertThat(e.getProject()).isSameAs(project);
        assertThat(e.getLead()).isSameAs(lead);
        assertThat(e.getDueDate()).isEqualTo(D);
        assertThat(e.getStartDate()).isEqualTo(D.plusDays(1));
        assertThat(e.getEndDate()).isEqualTo(D.plusDays(2));
        assertThat(e.getProgressPercent()).isEqualTo(25);
        assertThat(e.getCustomerVisible()).isTrue();
        assertThat(e.getMilestoneId()).isEqualTo(99L);
        assertThat(e.getActualEndDate()).isEqualTo(D.plusDays(3));
        assertThat(e.getActualStartDate()).isEqualTo(D.plusDays(4));
        assertThat(e.getEsDate()).isEqualTo(D);
        assertThat(e.getEfDate()).isEqualTo(D);
        assertThat(e.getLsDate()).isEqualTo(D);
        assertThat(e.getLfDate()).isEqualTo(D);
        assertThat(e.getTotalFloatDays()).isEqualTo(2);
        assertThat(e.getIsCritical()).isTrue();
        assertThat(e.getMonsoonSensitive()).isTrue();
        assertThat(e.getRejectionReason()).isEqualTo("Not started");
        assertThat(e.getWeight()).isEqualTo(10);
        assertThat(e.getDurationDays()).isEqualTo(3);
    }

    @Test
    void vendorPaymentRequest_mapsAllFields() {
        PurchaseInvoice invoice = new PurchaseInvoice();
        PortalUser paidBy = new PortalUser();
        PortalUser approvedBy = new PortalUser();
        VendorPaymentRequest req = new VendorPaymentRequest(
                invoice, D, BigDecimal.valueOf(50000), BigDecimal.valueOf(1000),
                BigDecimal.valueOf(500), BigDecimal.valueOf(48500), VendorPayment.PaymentMode.NEFT,
                "TXN555", "CHQ-1", "HDFC", paidBy, approvedBy, "Paid in full");

        VendorPayment e = req.toEntity();

        assertThat(e).isNotNull();
        assertThat(e.getInvoice()).isSameAs(invoice);
        assertThat(e.getPaymentDate()).isEqualTo(D);
        assertThat(e.getAmountPaid()).isEqualByComparingTo(BigDecimal.valueOf(50000));
        assertThat(e.getTdsDeducted()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(e.getOtherDeductions()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(e.getNetPaid()).isEqualByComparingTo(BigDecimal.valueOf(48500));
        assertThat(e.getPaymentMode()).isEqualTo(VendorPayment.PaymentMode.NEFT);
        assertThat(e.getTransactionReference()).isEqualTo("TXN555");
        assertThat(e.getChequeNumber()).isEqualTo("CHQ-1");
        assertThat(e.getBankName()).isEqualTo("HDFC");
        assertThat(e.getPaidBy()).isSameAs(paidBy);
        assertThat(e.getApprovedBy()).isSameAs(approvedBy);
        assertThat(e.getNotes()).isEqualTo("Paid in full");
    }

    @Test
    void vendorQuotationRequest_mapsAllFields() {
        VendorQuotationRequest req = new VendorQuotationRequest(
                BigDecimal.valueOf(75000), "Cement, Steel", BigDecimal.valueOf(2000),
                BigDecimal.valueOf(13500), D.plusDays(7), D.plusDays(30),
                "http://docs/quote.pdf", "Valid for 30 days");

        VendorQuotation e = req.toEntity();

        assertThat(e).isNotNull();
        assertThat(e.getQuotedAmount()).isEqualByComparingTo(BigDecimal.valueOf(75000));
        assertThat(e.getItemsIncluded()).isEqualTo("Cement, Steel");
        assertThat(e.getDeliveryCharges()).isEqualByComparingTo(BigDecimal.valueOf(2000));
        assertThat(e.getTaxAmount()).isEqualByComparingTo(BigDecimal.valueOf(13500));
        assertThat(e.getExpectedDeliveryDate()).isEqualTo(D.plusDays(7));
        assertThat(e.getValidUntil()).isEqualTo(D.plusDays(30));
        assertThat(e.getDocumentUrl()).isEqualTo("http://docs/quote.pdf");
        assertThat(e.getNotes()).isEqualTo("Valid for 30 days");
    }
}
