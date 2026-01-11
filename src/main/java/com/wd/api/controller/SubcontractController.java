package com.wd.api.controller;

import com.wd.api.model.SubcontractMeasurement;
import com.wd.api.model.SubcontractPayment;
import com.wd.api.model.SubcontractWorkOrder;
import com.wd.api.service.SubcontractService;
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

    @PostMapping("/project/{projectId}")
    public ResponseEntity<SubcontractWorkOrder> createWorkOrder(@PathVariable Long projectId,
            @RequestParam Long vendorId,
            @RequestBody SubcontractWorkOrder workOrder) {
        return ResponseEntity.ok(subcontractService.createWorkOrder(projectId, vendorId, workOrder));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<SubcontractWorkOrder>> getProjectSubcontracts(@PathVariable Long projectId) {
        return ResponseEntity.ok(subcontractService.getProjectSubcontracts(projectId));
    }

    @PostMapping("/{workOrderId}/measurements")
    public ResponseEntity<SubcontractMeasurement> recordMeasurement(@PathVariable Long workOrderId,
            @RequestBody SubcontractMeasurement measurement) {
        return ResponseEntity.ok(subcontractService.recordMeasurement(workOrderId, measurement));
    }

    @PostMapping("/{workOrderId}/payments")
    public ResponseEntity<SubcontractPayment> processPayment(@PathVariable Long workOrderId,
            @RequestBody SubcontractPayment payment) {
        return ResponseEntity.ok(subcontractService.processPayment(workOrderId, payment));
    }

    @PostMapping("/retention/release")
    public ResponseEntity<com.wd.api.model.RetentionRelease> releaseRetention(
            @RequestBody com.wd.api.model.RetentionRelease release) {
        return ResponseEntity.ok(subcontractService.releaseRetention(release));
    }
}
