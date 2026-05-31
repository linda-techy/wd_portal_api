package com.wd.api.controller;

import com.wd.api.dto.MeasurementRejectRequest;
import com.wd.api.dto.RetentionReleaseRequest;
import com.wd.api.dto.SubcontractMeasurementRequest;
import com.wd.api.dto.SubcontractPaymentRequest;
import com.wd.api.dto.SubcontractSearchFilter;
import com.wd.api.dto.SubcontractSummaryDTO;
import com.wd.api.dto.SubcontractWorkOrderRequest;
import com.wd.api.model.RetentionRelease;
import com.wd.api.model.SubcontractMeasurement;
import com.wd.api.model.SubcontractPayment;
import com.wd.api.model.SubcontractWorkOrder;
import com.wd.api.service.SubcontractService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subcontracts")
public class SubcontractController {

    private final SubcontractService subcontractService;

    public SubcontractController(SubcontractService subcontractService) {
        this.subcontractService = subcontractService;
    }

    @GetMapping("/search")
    public ResponseEntity<Page<SubcontractWorkOrder>> searchSubcontracts(@ModelAttribute SubcontractSearchFilter filter) {
        return ResponseEntity.ok(subcontractService.searchSubcontracts(filter));
    }

    @PostMapping("/project/{projectId}")
    public ResponseEntity<SubcontractWorkOrder> createWorkOrder(@PathVariable Long projectId,
            @RequestParam Long vendorId,
            @RequestBody SubcontractWorkOrderRequest workOrder) {
        return ResponseEntity.ok(subcontractService.createWorkOrder(projectId, vendorId, workOrder.toEntity()));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<SubcontractWorkOrder>> getProjectSubcontracts(@PathVariable Long projectId) {
        return ResponseEntity.ok(subcontractService.getProjectSubcontracts(projectId));
    }

    @PostMapping("/{workOrderId}/measurements")
    public ResponseEntity<SubcontractMeasurement> recordMeasurement(@PathVariable Long workOrderId,
            @RequestBody SubcontractMeasurementRequest measurement) {
        return ResponseEntity.ok(subcontractService.recordMeasurement(workOrderId, measurement.toEntity()));
    }

    @PostMapping("/{workOrderId}/payments")
    public ResponseEntity<SubcontractPayment> processPayment(@PathVariable Long workOrderId,
            @RequestBody SubcontractPaymentRequest payment) {
        return ResponseEntity.ok(subcontractService.processPayment(workOrderId, payment.toEntity()));
    }

    @PostMapping("/retention/release")
    public ResponseEntity<com.wd.api.model.RetentionRelease> releaseRetention(
            @RequestBody RetentionReleaseRequest release) {
        return ResponseEntity.ok(subcontractService.releaseRetention(release.toEntity()));
    }

    // Work Order CRUD
    @GetMapping("/{id}")
    public ResponseEntity<SubcontractWorkOrder> getWorkOrder(@PathVariable Long id) {
        return ResponseEntity.ok(subcontractService.getWorkOrder(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubcontractWorkOrder> updateWorkOrder(@PathVariable Long id,
            @RequestBody SubcontractWorkOrderRequest updates) {
        return ResponseEntity.ok(subcontractService.updateWorkOrder(id, updates.toEntity()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkOrder(@PathVariable Long id) {
        subcontractService.deleteWorkOrder(id);
        return ResponseEntity.noContent().build();
    }

    // Measurements
    @GetMapping("/{workOrderId}/measurements")
    public ResponseEntity<List<SubcontractMeasurement>> getWorkOrderMeasurements(@PathVariable Long workOrderId) {
        return ResponseEntity.ok(subcontractService.getWorkOrderMeasurements(workOrderId));
    }

    @PatchMapping("/measurements/{id}/approve")
    public ResponseEntity<SubcontractMeasurement> approveMeasurement(@PathVariable Long id) {
        return ResponseEntity.ok(subcontractService.approveMeasurement(id));
    }

    @PatchMapping("/measurements/{id}/reject")
    public ResponseEntity<SubcontractMeasurement> rejectMeasurement(@PathVariable Long id,
            @RequestBody MeasurementRejectRequest request) {
        return ResponseEntity.ok(subcontractService.rejectMeasurement(id, request.rejectionReason()));
    }

    // Payments
    @GetMapping("/{workOrderId}/payments")
    public ResponseEntity<List<SubcontractPayment>> getWorkOrderPayments(@PathVariable Long workOrderId) {
        return ResponseEntity.ok(subcontractService.getWorkOrderPayments(workOrderId));
    }

    // Summary & Retention
    @GetMapping("/{workOrderId}/summary")
    public ResponseEntity<SubcontractSummaryDTO> getWorkOrderSummary(@PathVariable Long workOrderId) {
        return ResponseEntity.ok(subcontractService.getWorkOrderSummary(workOrderId));
    }

    @GetMapping("/{workOrderId}/retention-releases")
    public ResponseEntity<List<RetentionRelease>> getRetentionReleases(@PathVariable Long workOrderId) {
        return ResponseEntity.ok(subcontractService.getRetentionReleases(workOrderId));
    }
}
