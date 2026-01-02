package com.wd.api.service;

import com.wd.api.model.*;
import com.wd.api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SubcontractService {

    private final SubcontractWorkOrderRepository workOrderRepository;
    private final SubcontractMeasurementRepository measurementRepository;
    private final SubcontractPaymentRepository paymentRepository;
    private final CustomerProjectRepository projectRepository;
    private final VendorRepository vendorRepository; // Assuming this exists

    public SubcontractService(SubcontractWorkOrderRepository workOrderRepository,
            SubcontractMeasurementRepository measurementRepository,
            SubcontractPaymentRepository paymentRepository,
            CustomerProjectRepository projectRepository,
            VendorRepository vendorRepository) {
        this.workOrderRepository = workOrderRepository;
        this.measurementRepository = measurementRepository;
        this.paymentRepository = paymentRepository;
        this.projectRepository = projectRepository;
        this.vendorRepository = vendorRepository;
    }

    @Transactional
    public SubcontractWorkOrder createWorkOrder(Long projectId, Long vendorId, SubcontractWorkOrder workOrder) {
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
        SubcontractWorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("Work Order not found"));

        measurement.setWorkOrder(workOrder);
        measurement.setAmount(measurement.getQuantity().multiply(measurement.getRate()));
        measurement.setCreatedAt(LocalDateTime.now());

        return measurementRepository.save(measurement);
    }

    @Transactional
    public SubcontractPayment processPayment(Long workOrderId, SubcontractPayment payment) {
        SubcontractWorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("Work Order not found"));

        payment.setWorkOrder(workOrder);
        payment.setCreatedAt(LocalDateTime.now());

        // Calculate Net Amount (Gross - TDS)
        BigDecimal tds = payment.getGrossAmount().multiply(payment.getTdsPercentage().divide(new BigDecimal("100")));
        payment.setTdsAmount(tds);
        payment.setNetAmount(payment.getGrossAmount().subtract(tds));

        return paymentRepository.save(payment);
    }

    public List<SubcontractWorkOrder> getProjectSubcontracts(Long projectId) {
        return workOrderRepository.findByProjectId(projectId);
    }
}
