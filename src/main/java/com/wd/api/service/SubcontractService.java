package com.wd.api.service;

import com.wd.api.dto.SubcontractSummaryDTO;
import com.wd.api.model.*;
import com.wd.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Subcontract Service
 * Business logic for subcontractor work order management
 */
@Service
@RequiredArgsConstructor
public class SubcontractService {

    private final SubcontractWorkOrderRepository workOrderRepo;
    private final SubcontractMeasurementRepository measurementRepo;
    private final SubcontractPaymentRepository paymentRepo;
    private final VendorRepository vendorRepo;
    private final CustomerProjectRepository projectRepo;
    private final PortalUserRepository userRepo;

    // ===== WORK ORDER OPERATIONS =====

    @Transactional
    public SubcontractWorkOrder createWorkOrder(SubcontractWorkOrder workOrder) {
        // Generate work order number
        workOrder.setWorkOrderNumber(generateWorkOrderNumber());
        return workOrderRepo.save(workOrder);
    }

    @Transactional
    public SubcontractWorkOrder issueWorkOrder(Long workOrderId) {
        SubcontractWorkOrder wo = workOrderRepo.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("Work order not found"));

        if (!wo.canBeIssued()) {
            throw new RuntimeException("Work order cannot be issued in current status: " + wo.getStatus());
        }

        wo.setStatus(SubcontractWorkOrder.WorkOrderStatus.ISSUED);
        return workOrderRepo.save(wo);
    }

    @Transactional
    public SubcontractWorkOrder completeWorkOrder(Long workOrderId, LocalDate completionDate) {
        SubcontractWorkOrder wo = workOrderRepo.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("Work order not found"));

        wo.setStatus(SubcontractWorkOrder.WorkOrderStatus.COMPLETED);
        wo.setActualCompletionDate(completionDate);
        return workOrderRepo.save(wo);
    }

    @Transactional
    public SubcontractWorkOrder terminateWorkOrder(Long workOrderId, String reason) {
        SubcontractWorkOrder wo = workOrderRepo.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("Work order not found"));

        wo.setStatus(SubcontractWorkOrder.WorkOrderStatus.TERMINATED);
        wo.setTerminationReason(reason);
        return workOrderRepo.save(wo);
    }

    public List<SubcontractWorkOrder> getWorkOrdersByProject(Long projectId) {
        return workOrderRepo.findByProjectId(projectId);
    }

    public List<SubcontractWorkOrder> getActiveWorkOrders() {
        return workOrderRepo.findActiveWorkOrders();
    }

    // ===== MEASUREMENT OPERATIONS =====

    @Transactional
    public SubcontractMeasurement recordMeasurement(Long workOrderId, SubcontractMeasurement measurement) {
        SubcontractWorkOrder wo = workOrderRepo.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("Work order not found"));

        if (!wo.isUnitRateContract()) {
            throw new RuntimeException("Measurements are only for unit-rate contracts");
        }

        // Auto-generate bill number
        int nextBillNum = measurementRepo.getNextBillNumber(workOrderId);
        measurement.setBillNumber("RA Bill " + nextBillNum);

        // Calculate amount
        measurement.calculateAmount();

        // Link to work order
        measurement.setWorkOrder(wo);

        return measurementRepo.save(measurement);
    }

    @Transactional
    public SubcontractMeasurement approveMeasurement(Long measurementId, Long approvedById) {
        SubcontractMeasurement measurement = measurementRepo.findById(measurementId)
                .orElseThrow(() -> new RuntimeException("Measurement not found"));

        PortalUser approver = userRepo.findById(approvedById)
                .orElseThrow(() -> new RuntimeException("Approver not found"));

        measurement.approve(approver);
        return measurementRepo.save(measurement);
    }

    @Transactional
    public SubcontractMeasurement rejectMeasurement(Long measurementId, Long rejectedById, String reason) {
        SubcontractMeasurement measurement = measurementRepo.findById(measurementId)
                .orElseThrow(() -> new RuntimeException("Measurement not found"));

        PortalUser rejector = userRepo.findById(rejectedById)
                .orElseThrow(() -> new RuntimeException("Rejector not found"));

        measurement.reject(rejector, reason);
        return measurementRepo.save(measurement);
    }

    public List<SubcontractMeasurement> getMeasurementsByWorkOrder(Long workOrderId) {
        return measurementRepo.findByWorkOrderId(workOrderId);
    }

    public List<SubcontractMeasurement> getPendingMeasurements() {
        return measurementRepo.findByStatus(SubcontractMeasurement.MeasurementStatus.PENDING);
    }

    // ===== PAYMENT OPERATIONS =====

    @Transactional
    public SubcontractPayment recordPayment(SubcontractPayment payment) {
        SubcontractWorkOrder wo = workOrderRepo.findById(payment.getWorkOrder().getId())
                .orElseThrow(() -> new RuntimeException("Work order not found"));

        if (!wo.canRecordPayment()) {
            throw new RuntimeException("Cannot record payment for work order in status: " + wo.getStatus());
        }

        // Auto-calculate TDS and net amount
        payment.calculateAmounts();

        return paymentRepo.save(payment);
    }

    public List<SubcontractPayment> getPaymentsByWorkOrder(Long workOrderId) {
        return paymentRepo.findByWorkOrderId(workOrderId);
    }

    public List<SubcontractPayment> getPaymentsByProject(Long projectId) {
        return paymentRepo.findByProjectId(projectId);
    }

    // ===== REPORTING & SUMMARY =====

    public SubcontractSummaryDTO getWorkOrderSummary(Long workOrderId) {
        SubcontractWorkOrder wo = workOrderRepo.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("Work order not found"));

        SubcontractSummaryDTO summary = new SubcontractSummaryDTO();

        // Basic info
        summary.setWorkOrderId(wo.getId());
        summary.setWorkOrderNumber(wo.getWorkOrderNumber());
        summary.setProjectId(wo.getProject().getId());
        summary.setProjectName(wo.getProject().getName());
        summary.setVendorId(wo.getVendor().getId());
        summary.setVendorName(wo.getVendor().getName());
        summary.setScopeDescription(wo.getScopeDescription());
        summary.setMeasurementBasis(wo.getMeasurementBasis().name());
        summary.setStatus(wo.getStatus().name());

        // Financial data
        summary.setTotalContractAmount(wo.getNegotiatedAmount());

        if (wo.isUnitRateContract()) {
            // For unit-rate, get measured amounts
            BigDecimal totalMeasured = measurementRepo.getTotalApprovedAmount(workOrderId);
            summary.setTotalMeasuredAmount(totalMeasured);

            List<SubcontractMeasurement> measurements = measurementRepo.findByWorkOrderId(workOrderId);
            summary.setTotalMeasurements(measurements.size());
            summary.setApprovedMeasurements((int) measurements.stream()
                    .filter(m -> m.getStatus() == SubcontractMeasurement.MeasurementStatus.APPROVED).count());
            summary.setPendingMeasurements((int) measurements.stream()
                    .filter(m -> m.getStatus() == SubcontractMeasurement.MeasurementStatus.PENDING).count());
        }

        // Payments
        BigDecimal totalPaid = paymentRepo.getTotalPaidAmount(workOrderId);
        BigDecimal totalTds = paymentRepo.getTotalTdsAmount(workOrderId);
        summary.setTotalPaid(totalPaid);
        summary.setTotalTds(totalTds);
        summary.setTotalPayments((int) paymentRepo.countByWorkOrderId(workOrderId));

        // Balance calculation
        BigDecimal balanceDue;
        if (wo.isLumpsumContract()) {
            balanceDue = wo.getNegotiatedAmount().subtract(totalPaid);
        } else {
            BigDecimal totalMeasured = measurementRepo.getTotalApprovedAmount(workOrderId);
            balanceDue = totalMeasured.subtract(totalPaid);
        }
        summary.setBalanceDue(balanceDue);

        // Percentages
        if (wo.isUnitRateContract() && wo.getNegotiatedAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalMeasured = measurementRepo.getTotalApprovedAmount(workOrderId);
            BigDecimal percentComplete = totalMeasured.divide(wo.getNegotiatedAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            summary.setPercentageCompleted(percentComplete);
        }

        return summary;
    }

    public List<SubcontractSummaryDTO> getProjectSubcontractSummaries(Long projectId) {
        List<SubcontractWorkOrder> workOrders = workOrderRepo.findByProjectId(projectId);
        return workOrders.stream()
                .map(wo -> getWorkOrderSummary(wo.getId()))
                .collect(Collectors.toList());
    }

    // ===== UTILITY METHODS =====

    private String generateWorkOrderNumber() {
        // Format: WAL/SC/YY/NNN
        int year = Year.now().getValue() % 100; // Last 2 digits
        long nextSeq = workOrderRepo.count() + 1;
        return String.format("WAL/SC/%02d/%03d", year, nextSeq);
    }

    public BigDecimal calculateBillableAmount(Long workOrderId) {
        SubcontractWorkOrder wo = workOrderRepo.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("Work order not found"));

        if (wo.isLumpsumContract()) {
            return wo.getNegotiatedAmount();
        } else {
            return measurementRepo.getTotalApprovedAmount(workOrderId);
        }
    }

    public BigDecimal calculateOutstandingAmount(Long workOrderId) {
        BigDecimal billable = calculateBillableAmount(workOrderId);
        BigDecimal paid = paymentRepo.getTotalPaidAmount(workOrderId);
        return billable.subtract(paid);
    }
}
