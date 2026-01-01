package com.wd.api.controller;

import com.wd.api.dto.SubcontractSummaryDTO;
import com.wd.api.model.SubcontractMeasurement;
import com.wd.api.model.SubcontractPayment;
import com.wd.api.model.SubcontractWorkOrder;
import com.wd.api.service.SubcontractService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Subcontract Management REST Controller
 * Handles work orders, measurements, and payments for subcontractors
 */
@RestController
@RequestMapping("/api/subcontracts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SubcontractController {

    private final SubcontractService subcontractService;

    // ===== WORK ORDER ENDPOINTS =====

    /**
     * Create a new subcontract work order
     */
    @PostMapping("/work-orders")
    public ResponseEntity<SubcontractWorkOrder> createWorkOrder(@RequestBody SubcontractWorkOrder workOrder) {
        SubcontractWorkOrder created = subcontractService.createWorkOrder(workOrder);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get work order by ID
     */
    @GetMapping("/work-orders/{id}")
    public ResponseEntity<SubcontractWorkOrder> getWorkOrder(@PathVariable Long id) {
        // Implementation would use findById from repository
        return ResponseEntity.ok().build();
    }

    /**
     * Get all work orders for a project
     */
    @GetMapping("/projects/{projectId}/work-orders")
    public ResponseEntity<List<SubcontractWorkOrder>> getProjectWorkOrders(@PathVariable Long projectId) {
        List<SubcontractWorkOrder> workOrders = subcontractService.getWorkOrdersByProject(projectId);
        return ResponseEntity.ok(workOrders);
    }

    /**
     * Get all active work orders
     */
    @GetMapping("/work-orders/active")
    public ResponseEntity<List<SubcontractWorkOrder>> getActiveWorkOrders() {
        List<SubcontractWorkOrder> activeOrders = subcontractService.getActiveWorkOrders();
        return ResponseEntity.ok(activeOrders);
    }

    /**
     * Issue a work order (change status to ISSUED)
     */
    @PostMapping("/work-orders/{id}/issue")
    public ResponseEntity<SubcontractWorkOrder> issueWorkOrder(@PathVariable Long id) {
        SubcontractWorkOrder issued = subcontractService.issueWorkOrder(id);
        return ResponseEntity.ok(issued);
    }

    /**
     * Complete a work order
     */
    @PostMapping("/work-orders/{id}/complete")
    public ResponseEntity<SubcontractWorkOrder> completeWorkOrder(
            @PathVariable Long id,
            @RequestParam String completionDate) {
        // Parse date and call service
        return ResponseEntity.ok().build();
    }

    /**
     * Terminate a work order
     */
    @PostMapping("/work-orders/{id}/terminate")
    public ResponseEntity<SubcontractWorkOrder> terminateWorkOrder(
            @PathVariable Long id,
            @RequestBody String reason) {
        SubcontractWorkOrder terminated = subcontractService.terminateWorkOrder(id, reason);
        return ResponseEntity.ok(terminated);
    }

    // ===== MEASUREMENT ENDPOINTS =====

    /**
     * Record a new measurement for unit-rate contract
     */
    @PostMapping("/work-orders/{workOrderId}/measurements")
    public ResponseEntity<SubcontractMeasurement> recordMeasurement(
            @PathVariable Long workOrderId,
            @RequestBody SubcontractMeasurement measurement) {
        SubcontractMeasurement recorded = subcontractService.recordMeasurement(workOrderId, measurement);
        return ResponseEntity.status(HttpStatus.CREATED).body(recorded);
    }

    /**
     * Get all measurements for a work order
     */
    @GetMapping("/work-orders/{workOrderId}/measurements")
    public ResponseEntity<List<SubcontractMeasurement>> getWorkOrderMeasurements(@PathVariable Long workOrderId) {
        List<SubcontractMeasurement> measurements = subcontractService.getMeasurementsByWorkOrder(workOrderId);
        return ResponseEntity.ok(measurements);
    }

    /**
     * Approve a measurement
     */
    @PostMapping("/measurements/{measurementId}/approve")
    public ResponseEntity<SubcontractMeasurement> approveMeasurement(
            @PathVariable Long measurementId,
            @RequestParam Long approvedById) {
        SubcontractMeasurement approved = subcontractService.approveMeasurement(measurementId, approvedById);
        return ResponseEntity.ok(approved);
    }

    /**
     * Reject a measurement
     */
    @PostMapping("/measurements/{measurementId}/reject")
    public ResponseEntity<SubcontractMeasurement> rejectMeasurement(
            @PathVariable Long measurementId,
            @RequestParam Long rejectedById,
            @RequestBody String reason) {
        SubcontractMeasurement rejected = subcontractService.rejectMeasurement(measurementId, rejectedById, reason);
        return ResponseEntity.ok(rejected);
    }

    /**
     * Get all pending measurements
     */
    @GetMapping("/measurements/pending")
    public ResponseEntity<List<SubcontractMeasurement>> getPendingMeasurements() {
        List<SubcontractMeasurement> pending = subcontractService.getPendingMeasurements();
        return ResponseEntity.ok(pending);
    }

    // ===== PAYMENT ENDPOINTS =====

    /**
     * Record a payment for a work order
     */
    @PostMapping("/work-orders/{workOrderId}/payments")
    public ResponseEntity<SubcontractPayment> recordPayment(
            @PathVariable Long workOrderId,
            @RequestBody SubcontractPayment payment) {
        SubcontractPayment recorded = subcontractService.recordPayment(payment);
        return ResponseEntity.status(HttpStatus.CREATED).body(recorded);
    }

    /**
     * Get all payments for a work order
     */
    @GetMapping("/work-orders/{workOrderId}/payments")
    public ResponseEntity<List<SubcontractPayment>> getWorkOrderPayments(@PathVariable Long workOrderId) {
        List<SubcontractPayment> payments = subcontractService.getPaymentsByWorkOrder(workOrderId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get all payments for a project
     */
    @GetMapping("/projects/{projectId}/payments")
    public ResponseEntity<List<SubcontractPayment>> getProjectPayments(@PathVariable Long projectId) {
        List<SubcontractPayment> payments = subcontractService.getPaymentsByProject(projectId);
        return ResponseEntity.ok(payments);
    }

    // ===== SUMMARY & REPORTING ENDPOINTS =====

    /**
     * Get financial summary for a work order
     */
    @GetMapping("/work-orders/{workOrderId}/summary")
    public ResponseEntity<SubcontractSummaryDTO> getWorkOrderSummary(@PathVariable Long workOrderId) {
        SubcontractSummaryDTO summary = subcontractService.getWorkOrderSummary(workOrderId);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get all subcontract summaries for a project
     */
    @GetMapping("/projects/{projectId}/summaries")
    public ResponseEntity<List<SubcontractSummaryDTO>> getProjectSubcontractSummaries(@PathVariable Long projectId) {
        List<SubcontractSummaryDTO> summaries = subcontractService.getProjectSubcontractSummaries(projectId);
        return ResponseEntity.ok(summaries);
    }

    // ===== EXCEPTION HANDLING =====

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
