package com.wd.api.service;

import com.wd.api.dto.SubcontractSearchFilter;
import com.wd.api.model.*;
import com.wd.api.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SubcontractService {

    private final SubcontractWorkOrderRepository workOrderRepository;
    private final SubcontractMeasurementRepository measurementRepository;
    private final SubcontractPaymentRepository paymentRepository;
    private final CustomerProjectRepository projectRepository;
    private final VendorRepository vendorRepository;
    private final RetentionReleaseRepository retentionReleaseRepository;

    public SubcontractService(SubcontractWorkOrderRepository workOrderRepository,
            SubcontractMeasurementRepository measurementRepository,
            SubcontractPaymentRepository paymentRepository,
            CustomerProjectRepository projectRepository,
            VendorRepository vendorRepository,
            RetentionReleaseRepository retentionReleaseRepository) {
        this.workOrderRepository = workOrderRepository;
        this.measurementRepository = measurementRepository;
        this.paymentRepository = paymentRepository;
        this.projectRepository = projectRepository;
        this.vendorRepository = vendorRepository;
        this.retentionReleaseRepository = retentionReleaseRepository;
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public Page<SubcontractWorkOrder> searchSubcontracts(SubcontractSearchFilter filter) {
        Specification<SubcontractWorkOrder> spec = buildSpecification(filter);
        return workOrderRepository.findAll(spec, filter.toPageable());
    }

    private Specification<SubcontractWorkOrder> buildSpecification(SubcontractSearchFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search across workOrderNumber, workDescription, vendor name
            if (filter.getSearch() != null && !filter.getSearch().isEmpty()) {
                String searchPattern = "%" + filter.getSearch().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("workOrderNumber")), searchPattern),
                        cb.like(cb.lower(root.get("workDescription")), searchPattern),
                        cb.like(cb.lower(root.join("vendor").get("name")), searchPattern)));
            }

            // Filter by projectId
            if (filter.getProjectId() != null) {
                predicates.add(cb.equal(root.get("project").get("id"), filter.getProjectId()));
            }

            // Filter by contractorId (vendor)
            if (filter.getContractorId() != null) {
                predicates.add(cb.equal(root.get("vendor").get("id"), filter.getContractorId()));
            }

            // Filter by workOrderNumber
            if (filter.getWorkOrderNumber() != null && !filter.getWorkOrderNumber().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("workOrderNumber")),
                        "%" + filter.getWorkOrderNumber().toLowerCase() + "%"));
            }

            // Filter by status
            if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
                predicates.add(
                        cb.equal(root.get("status"), SubcontractWorkOrder.WorkOrderStatus.valueOf(filter.getStatus())));
            }

            // Date range filter
            if (filter.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getStartDate().atStartOfDay()));
            }
            if (filter.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getEndDate().atTime(23, 59, 59)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Transactional
    public SubcontractWorkOrder createWorkOrder(Long projectId, Long vendorId, SubcontractWorkOrder workOrder) {
        if (projectId == null || vendorId == null)
            throw new IllegalArgumentException("Project ID and Vendor ID cannot be null");
        CustomerProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        workOrder.setProject(project);
        workOrder.setVendor(vendor);
        workOrder.setStatus(SubcontractWorkOrder.WorkOrderStatus.DRAFT);
        workOrder.setCreatedAt(LocalDateTime.now());
        workOrder.setUpdatedAt(LocalDateTime.now());

        // Generate proper logic for workOrderNumber (e.g. SC-{FY}-{ID})
        // For now using simple random or provided logic
        if (workOrder.getWorkOrderNumber() == null) {
            workOrder.setWorkOrderNumber("SC-" + System.currentTimeMillis());
        }

        return workOrderRepository.save(workOrder);
    }

    @Transactional
    public SubcontractMeasurement recordMeasurement(Long workOrderId, SubcontractMeasurement measurement) {
        if (workOrderId == null)
            throw new IllegalArgumentException("Work Order ID cannot be null");
        SubcontractWorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("Work Order not found"));

        measurement.setWorkOrder(workOrder);
        measurement.setAmount(measurement.getQuantity().multiply(measurement.getRate()));
        measurement.setCreatedAt(LocalDateTime.now());

        return measurementRepository.save(measurement);
    }

    @Transactional
    public SubcontractPayment processPayment(Long workOrderId, SubcontractPayment payment) {
        if (workOrderId == null)
            throw new IllegalArgumentException("Work Order ID cannot be null");
        SubcontractWorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("Work Order not found"));

        payment.setWorkOrder(workOrder);
        payment.setCreatedAt(LocalDateTime.now());

        // Calculate Retention Amount
        BigDecimal retentionPct = workOrder.getRetentionPercentage() != null ? workOrder.getRetentionPercentage()
                : BigDecimal.ZERO;
        BigDecimal retentionAmt = payment.getGrossAmount().multiply(retentionPct.divide(new BigDecimal("100")));
        payment.setRetentionAmount(retentionAmt);

        // Calculate TDS
        BigDecimal tds = payment.getGrossAmount().multiply(payment.getTdsPercentage().divide(new BigDecimal("100")));
        payment.setTdsAmount(tds);

        // Net Amount = Gross - Retention - TDS
        BigDecimal deduction = retentionAmt.add(tds);
        payment.setNetAmount(payment.getGrossAmount().subtract(deduction));

        // Update Accumulated Retention in Work Order
        if (workOrder.getTotalRetentionAccumulated() == null) {
            workOrder.setTotalRetentionAccumulated(BigDecimal.ZERO);
        }
        workOrder.setTotalRetentionAccumulated(workOrder.getTotalRetentionAccumulated().add(retentionAmt));
        workOrderRepository.save(workOrder);

        return paymentRepository.save(payment);
    }

    @Transactional
    @SuppressWarnings("null")
    public RetentionRelease releaseRetention(RetentionRelease release) {
        if (release.getWorkOrder() == null || release.getWorkOrder().getId() == null) {
            throw new IllegalArgumentException("Work Order ID is required");
        }
        SubcontractWorkOrder workOrder = workOrderRepository.findById(release.getWorkOrder().getId())
                .orElseThrow(() -> new RuntimeException("Work Order not found"));

        // Validate Amount
        BigDecimal totalHeld = workOrder.getTotalRetentionAccumulated() != null
                ? workOrder.getTotalRetentionAccumulated()
                : BigDecimal.ZERO;

        // Calculate already released (Naive approach: sum of all approved releases)
        // ideally we store 'released' on workOrder too, but for now lets rely on the
        // check below

        if (release.getAmountReleased().compareTo(totalHeld) > 0) {
            throw new IllegalArgumentException("Cannot release more than accumulated retention");
        }

        release.setWorkOrder(workOrder);
        release.setReleaseDate(release.getReleaseDate() != null ? release.getReleaseDate() : java.time.LocalDate.now());
        release.setStatus(RetentionRelease.ReleaseStatus.APPROVED); // Auto-approve for now

        RetentionRelease savedRelease = retentionReleaseRepository.save(release);

        // CREATE PAYMENT RECORD for the released amount
        SubcontractPayment payment = new SubcontractPayment();
        payment.setWorkOrder(workOrder);
        payment.setPaymentDate(savedRelease.getReleaseDate());
        payment.setGrossAmount(savedRelease.getAmountReleased());
        payment.setTdsPercentage(BigDecimal.ZERO); // No TDS on retention release usually (already deducted?)
        payment.setTdsAmount(BigDecimal.ZERO);
        payment.setRetentionAmount(BigDecimal.ZERO);
        payment.setNetAmount(savedRelease.getAmountReleased());
        payment.setPaymentMode("BANK_TRANSFER"); // Default
        payment.setCreatedAt(LocalDateTime.now());

        paymentRepository.save(payment);

        return savedRelease;
    }

    public List<SubcontractWorkOrder> getProjectSubcontracts(Long projectId) {
        if (projectId == null)
            return List.of();
        return workOrderRepository.findByProjectId(projectId);
    }
}
