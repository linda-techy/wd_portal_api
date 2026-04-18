package com.wd.api.controller;

import com.wd.api.model.BoqItem;
import com.wd.api.model.DelayLog;
import com.wd.api.model.Observation;
import com.wd.api.model.PaymentStage;
import com.wd.api.model.QualityCheck;
import com.wd.api.repository.BoqItemRepository;
import com.wd.api.repository.DelayLogRepository;
import com.wd.api.repository.ObservationRepository;
import com.wd.api.repository.PaymentStageRepository;
import com.wd.api.repository.QualityCheckRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * CSV export endpoints for Portal — one endpoint per domain.
 * All responses use UTF-8 with BOM so Excel opens them correctly.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/export")
@PreAuthorize("isAuthenticated()")
public class ExportController {

    private final BoqItemRepository boqItemRepository;
    private final PaymentStageRepository paymentStageRepository;
    private final DelayLogRepository delayLogRepository;
    private final QualityCheckRepository qualityCheckRepository;
    private final ObservationRepository observationRepository;

    public ExportController(
            BoqItemRepository boqItemRepository,
            PaymentStageRepository paymentStageRepository,
            DelayLogRepository delayLogRepository,
            QualityCheckRepository qualityCheckRepository,
            ObservationRepository observationRepository) {
        this.boqItemRepository = boqItemRepository;
        this.paymentStageRepository = paymentStageRepository;
        this.delayLogRepository = delayLogRepository;
        this.qualityCheckRepository = qualityCheckRepository;
        this.observationRepository = observationRepository;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Wraps a text field in double-quotes and escapes embedded quotes. */
    private static String q(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static String safe(Object value) {
        return value == null ? "" : value.toString();
    }

    private ResponseEntity<byte[]> csvResponse(String csv, String filename) {
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(bytes);
    }

    // ── 1. BOQ ───────────────────────────────────────────────────────────────

    @GetMapping("/boq")
    @PreAuthorize("hasAuthority('BOQ_VIEW')")
    public ResponseEntity<byte[]> exportBoqCsv(@PathVariable Long projectId) {
        List<BoqItem> items = boqItemRepository.findByProjectIdWithAssociations(projectId);

        StringBuilder csv = new StringBuilder("\uFEFF"); // UTF-8 BOM
        csv.append("Item Code,Description,Work Type,Unit,Quantity,Rate,Amount,Executed Qty,Billed Qty,Status\n");

        for (BoqItem item : items) {
            csv.append(q(item.getItemCode())).append(",");
            csv.append(q(item.getDescription())).append(",");
            csv.append(q(item.getWorkType() != null ? item.getWorkType().getName() : "")).append(",");
            csv.append(q(item.getUnit())).append(",");
            csv.append(safe(item.getQuantity())).append(",");
            csv.append(safe(item.getUnitRate())).append(",");
            csv.append(safe(item.getTotalAmount())).append(",");
            csv.append(safe(item.getExecutedQuantity())).append(",");
            csv.append(safe(item.getBilledQuantity())).append(",");
            csv.append(safe(item.getStatus())).append("\n");
        }

        String filename = String.format("boq_%s.csv", LocalDate.now());
        return csvResponse(csv.toString(), filename);
    }

    // ── 2. Payments (Payment Stages) ─────────────────────────────────────────

    @GetMapping("/payments")
    @PreAuthorize("hasAuthority('STAGE_VIEW')")
    public ResponseEntity<byte[]> exportPaymentsCsv(@PathVariable Long projectId) {
        List<PaymentStage> stages = paymentStageRepository.findByProjectIdOrderByStageNumberAsc(projectId);

        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("Stage #,Stage Name,Amount (ex GST),GST Amount,Amount (incl GST),Retention Held,Net Payable,Paid Amount,Status,Due Date,Milestone Description\n");

        for (PaymentStage s : stages) {
            csv.append(safe(s.getStageNumber())).append(",");
            csv.append(q(s.getStageName())).append(",");
            csv.append(safe(s.getStageAmountExGst())).append(",");
            csv.append(safe(s.getGstAmount())).append(",");
            csv.append(safe(s.getStageAmountInclGst())).append(",");
            csv.append(safe(s.getRetentionHeld())).append(",");
            csv.append(safe(s.getNetPayableAmount())).append(",");
            csv.append(safe(s.getPaidAmount())).append(",");
            csv.append(safe(s.getStatus())).append(",");
            csv.append(safe(s.getDueDate())).append(",");
            csv.append(q(s.getMilestoneDescription())).append("\n");
        }

        String filename = String.format("payments_%s.csv", LocalDate.now());
        return csvResponse(csv.toString(), filename);
    }

    // ── 3. Delays ─────────────────────────────────────────────────────────────

    @GetMapping("/delays")
    @PreAuthorize("hasAuthority('DELAY_VIEW')")
    public ResponseEntity<byte[]> exportDelaysCsv(@PathVariable Long projectId) {
        List<DelayLog> delays = delayLogRepository.findByProjectId(projectId);

        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("Date,Category,Duration Days,Responsible Party,Impact\n");

        for (DelayLog d : delays) {
            csv.append(safe(d.getFromDate())).append(",");
            csv.append(q(d.getReasonCategory() != null ? d.getReasonCategory() : d.getDelayType())).append(",");
            csv.append(safe(d.getDurationDays())).append(",");
            csv.append(q(d.getResponsibleParty())).append(",");
            csv.append(q(d.getImpactDescription())).append("\n");
        }

        String filename = String.format("delays_%s.csv", LocalDate.now());
        return csvResponse(csv.toString(), filename);
    }

    // ── 4. Quality Checks ────────────────────────────────────────────────────

    @GetMapping("/quality-checks")
    @PreAuthorize("hasAuthority('PROJECT_VIEW')")
    public ResponseEntity<byte[]> exportQualityChecksCsv(@PathVariable Long projectId) {
        List<QualityCheck> checks = qualityCheckRepository.findByProjectId(projectId);

        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("Name,Status,Result,Date,Inspector,Remarks\n");

        for (QualityCheck c : checks) {
            String inspector = c.getConductedBy() != null
                    ? c.getConductedBy().getFirstName() + " " + c.getConductedBy().getLastName()
                    : "";
            csv.append(q(c.getTitle())).append(",");
            csv.append(safe(c.getStatus())).append(",");
            csv.append(safe(c.getResult())).append(",");
            csv.append(safe(c.getCheckDate() != null ? c.getCheckDate().toLocalDate() : "")).append(",");
            csv.append(q(inspector)).append(",");
            csv.append(q(c.getRemarks())).append("\n");
        }

        String filename = String.format("quality_checks_%s.csv", LocalDate.now());
        return csvResponse(csv.toString(), filename);
    }

    // ── 5. Observations ──────────────────────────────────────────────────────

    @GetMapping("/observations")
    @PreAuthorize("hasAuthority('PROJECT_VIEW')")
    public ResponseEntity<byte[]> exportObservationsCsv(@PathVariable Long projectId) {
        List<Observation> observations = observationRepository.findByProjectIdOrderByReportedDateDesc(projectId);

        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("Title,Priority,Status,Reported Date,Resolved Date,Description\n");

        for (Observation o : observations) {
            csv.append(q(o.getTitle())).append(",");
            csv.append(safe(o.getPriority())).append(",");
            csv.append(safe(o.getStatus())).append(",");
            csv.append(safe(o.getReportedDate() != null ? o.getReportedDate().toLocalDate() : "")).append(",");
            csv.append(safe(o.getResolvedDate() != null ? o.getResolvedDate().toLocalDate() : "")).append(",");
            csv.append(q(o.getDescription())).append("\n");
        }

        String filename = String.format("observations_%s.csv", LocalDate.now());
        return csvResponse(csv.toString(), filename);
    }
}
