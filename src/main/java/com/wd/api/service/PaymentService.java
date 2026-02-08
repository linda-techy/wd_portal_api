package com.wd.api.service;

import com.wd.api.dto.PaymentDtos.*;
import com.wd.api.model.DesignPackagePayment;
import com.wd.api.model.PaymentSchedule;
import com.wd.api.model.PaymentTransaction;
import com.wd.api.repository.DesignPackagePaymentRepository;
import com.wd.api.repository.PaymentScheduleRepository;
import com.wd.api.repository.PaymentTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private static final BigDecimal GST_PERCENTAGE = new BigDecimal("18.00");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final DesignPackagePaymentRepository paymentRepository;
    private final PaymentScheduleRepository scheduleRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final com.wd.api.repository.CustomerProjectRepository projectRepository;
    // private final com.wd.api.repository.RetentionReleaseRepository
    // retentionReleaseRepository; // Removed
    private final com.wd.api.repository.TaxInvoiceRepository taxInvoiceRepository;

    public PaymentService(
            DesignPackagePaymentRepository paymentRepository,
            PaymentScheduleRepository scheduleRepository,
            PaymentTransactionRepository transactionRepository,
            com.wd.api.repository.CustomerProjectRepository projectRepository,
            com.wd.api.repository.TaxInvoiceRepository taxInvoiceRepository) {
        this.paymentRepository = paymentRepository;
        this.scheduleRepository = scheduleRepository;
        this.transactionRepository = transactionRepository;
        this.projectRepository = projectRepository;
        this.taxInvoiceRepository = taxInvoiceRepository;
    }

    @Transactional
    @SuppressWarnings("null")
    public DesignPaymentResponse createDesignPayment(CreateDesignPaymentRequest request, Long createdById) {
        logger.info("Creating design payment for project: {}", request.getProjectId());

        // Check if payment already exists for this project
        if (paymentRepository.existsByProject_Id(request.getProjectId())) {
            throw new IllegalStateException("Design payment already exists for this project");
        }

        // Fetch project reference
        var project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + request.getProjectId()));

        // Calculate amounts
        BigDecimal baseAmount = request.getRatePerSqft().multiply(request.getTotalSqft());
        BigDecimal gstAmount = baseAmount.multiply(GST_PERCENTAGE).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal subtotal = baseAmount.add(gstAmount);

        BigDecimal discountPercentage = request.getDiscountPercentage() != null
                ? request.getDiscountPercentage()
                : BigDecimal.ZERO;
        BigDecimal discountAmount = subtotal.multiply(discountPercentage).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.subtract(discountAmount);

        // Create payment record
        DesignPackagePayment payment = new DesignPackagePayment();
        payment.setProject(project);
        payment.setPackageName(request.getPackageName());
        payment.setRatePerSqft(request.getRatePerSqft());
        payment.setTotalSqft(request.getTotalSqft());
        payment.setBaseAmount(baseAmount);
        payment.setGstPercentage(GST_PERCENTAGE);
        payment.setGstAmount(gstAmount);
        payment.setDiscountPercentage(discountPercentage);
        payment.setDiscountAmount(discountAmount);
        payment.setTotalAmount(totalAmount);
        payment.setPaymentType(request.getPaymentType());
        payment.setStatus("PENDING");
        payment.setCreatedById(createdById);

        // Create payment schedule based on payment type
        if ("FULL".equals(request.getPaymentType())) {
            // Single payment
            PaymentSchedule schedule = new PaymentSchedule();
            schedule.setInstallmentNumber(1);
            schedule.setDescription("Full Payment");
            schedule.setAmount(totalAmount);
            schedule.setStatus("PENDING");
            payment.addSchedule(schedule);
        } else {
            // 3 milestone-based installments
            BigDecimal installmentAmount = totalAmount.divide(new BigDecimal("3"), 2, RoundingMode.HALF_UP);
            BigDecimal remainder = totalAmount.subtract(installmentAmount.multiply(new BigDecimal("3")));

            String[] milestones = { "Advance", "Design Phase", "Post-Design" };
            for (int i = 0; i < 3; i++) {
                PaymentSchedule schedule = new PaymentSchedule();
                schedule.setInstallmentNumber(i + 1);
                schedule.setDescription(milestones[i]);
                // Add remainder to first installment to ensure exact total
                BigDecimal amount = (i == 0) ? installmentAmount.add(remainder) : installmentAmount;
                schedule.setAmount(amount);
                schedule.setStatus("PENDING");
                payment.addSchedule(schedule);
            }
        }

        DesignPackagePayment saved = paymentRepository.save(payment);
        logger.info("Design payment created with ID: {}", saved.getId());

        return toDesignPaymentResponse(saved);
    }

    @Transactional(readOnly = true)
    public DesignPaymentResponse getDesignPaymentByProjectId(Long projectId) {
        DesignPackagePayment payment = paymentRepository.findByProject_Id(projectId)
                .orElseThrow(() -> new IllegalArgumentException("No design payment found for project: " + projectId));
        return toDesignPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public org.springframework.data.domain.Page<DesignPaymentResponse> getAllPayments(String search,
            org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.jpa.domain.Specification<DesignPackagePayment> spec = com.wd.api.repository.spec.PaymentSpecification
                .search(search);
        return paymentRepository.findAll(spec, pageable)
                .map(this::toDesignPaymentResponse);
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public org.springframework.data.domain.Page<DesignPaymentResponse> getPendingPayments(String search,
            org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.jpa.domain.Specification<DesignPackagePayment> spec = org.springframework.data.jpa.domain.Specification
                .allOf(com.wd.api.repository.spec.PaymentSpecification.statusNot("PAID"),
                        com.wd.api.repository.spec.PaymentSpecification.search(search));

        return paymentRepository.findAll(spec, pageable)
                .map(this::toDesignPaymentResponse);
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public org.springframework.data.domain.Page<TransactionResponse> getTransactionHistory(
            String search, String method, String status, LocalDateTime start, LocalDateTime end,
            org.springframework.data.domain.Pageable pageable) {

        org.springframework.data.jpa.domain.Specification<PaymentTransaction> spec = org.springframework.data.jpa.domain.Specification
                .allOf(com.wd.api.repository.spec.TransactionSpecification.search(search),
                        com.wd.api.repository.spec.TransactionSpecification.methodIs(method),
                        com.wd.api.repository.spec.TransactionSpecification.statusIs(status),
                        com.wd.api.repository.spec.TransactionSpecification.dateBetween(start, end));

        return transactionRepository.findAll(spec, pageable)
                .map(this::toTransactionResponse);
    }

    @Transactional
    @SuppressWarnings("null")
    public TransactionResponse recordTransaction(Long scheduleId, RecordTransactionRequest request, Long recordedById) {
        logger.info("Recording transaction for schedule: {}", scheduleId);

        PaymentSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Payment schedule not found: " + scheduleId));

        // Create transaction
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setAmount(request.getAmount());
        transaction.setPaymentMethod(request.getPaymentMethod());
        transaction.setReferenceNumber(request.getReferenceNumber());
        transaction.setPaymentDate(request.getPaymentDate() != null ? request.getPaymentDate() : LocalDateTime.now());
        transaction.setNotes(request.getNotes());
        transaction.setRecordedById(recordedById);

        // TDS Handling (Optional - defaults to 0% if not provided)
        BigDecimal tdsPercentage = request.getTdsPercentage() != null ? request.getTdsPercentage() : BigDecimal.ZERO;
        transaction.setTdsPercentage(tdsPercentage);
        transaction.setTdsDeductedBy(request.getTdsDeductedBy() != null ? request.getTdsDeductedBy() : "CUSTOMER");

        // Payment Category
        transaction
                .setPaymentCategory(request.getPaymentCategory() != null ? request.getPaymentCategory() : "PROGRESS");

        // Use database function for safe receipt generation (fixes race condition)
        transaction.setReceiptNumber(transactionRepository.generateReceiptNumber());
        transaction.setStatus("COMPLETED");
        schedule.addTransaction(transaction);

        // Update paid amount using NET amount (after TDS deduction)
        // The entity @PrePersist will calculate netAmount based on TDS
        BigDecimal netAmount = transaction.getAmount();
        if (tdsPercentage.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal tdsAmount = transaction.getAmount().multiply(tdsPercentage)
                    .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            netAmount = transaction.getAmount().subtract(tdsAmount);
        }

        BigDecimal newPaidAmount = schedule.getPaidAmount().add(netAmount);
        schedule.setPaidAmount(newPaidAmount);

        // Update schedule status
        if (newPaidAmount.compareTo(schedule.getAmount()) >= 0) {
            schedule.setStatus("PAID");
            schedule.setPaidDate(LocalDateTime.now());
        }

        scheduleRepository.save(schedule);

        // Update parent payment status
        updatePaymentStatus(schedule.getDesignPayment());

        logger.info("Transaction recorded for schedule: {}", scheduleId);
        return toTransactionResponse(transaction);
    }

    private void updatePaymentStatus(DesignPackagePayment payment) {
        List<PaymentSchedule> schedules = payment.getSchedules();

        long paidCount = schedules.stream().filter(s -> "PAID".equals(s.getStatus())).count();

        if (paidCount == schedules.size()) {
            payment.setStatus("PAID");
        } else if (paidCount > 0) {
            payment.setStatus("PARTIAL");
        } else {
            payment.setStatus("PENDING");
        }

        paymentRepository.save(payment);
    }

    // ===== Mapping Methods =====

    private DesignPaymentResponse toDesignPaymentResponse(DesignPackagePayment payment) {
        DesignPaymentResponse response = new DesignPaymentResponse();
        response.setId(payment.getId());
        response.setProjectId(payment.getProjectId());

        // Populate metadata
        if (payment.getProject() != null) {
            response.setProjectName(payment.getProject().getName());
            if (payment.getProject().getCustomer() != null) {
                String fullName = payment.getProject().getCustomer().getFirstName() + " " +
                        payment.getProject().getCustomer().getLastName();
                response.setCustomerName(fullName.trim());
            }
        }

        response.setPackageName(payment.getPackageName());
        response.setRatePerSqft(payment.getRatePerSqft());
        response.setTotalSqft(payment.getTotalSqft());
        response.setBaseAmount(payment.getBaseAmount());
        response.setGstPercentage(payment.getGstPercentage());
        response.setGstAmount(payment.getGstAmount());
        response.setDiscountPercentage(payment.getDiscountPercentage());
        response.setDiscountAmount(payment.getDiscountAmount());
        response.setTotalAmount(payment.getTotalAmount());
        response.setPaymentType(payment.getPaymentType());
        response.setStatus(payment.getStatus());
        response.setCreatedAt(payment.getCreatedAt());

        // Calculate totals
        BigDecimal totalPaid = payment.getSchedules().stream()
                .map(PaymentSchedule::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setTotalPaid(totalPaid);
        response.setBalanceDue(payment.getTotalAmount().subtract(totalPaid));

        // Map schedules
        List<ScheduleResponse> schedules = payment.getSchedules().stream()
                .map(this::toScheduleResponse)
                .collect(Collectors.toList());
        response.setSchedules(schedules);

        return response;
    }

    private ScheduleResponse toScheduleResponse(PaymentSchedule schedule) {
        ScheduleResponse response = new ScheduleResponse();
        response.setId(schedule.getId());
        response.setInstallmentNumber(schedule.getInstallmentNumber());
        response.setDescription(schedule.getDescription());
        response.setAmount(schedule.getAmount());
        response.setDueDate(schedule.getDueDate());
        response.setStatus(schedule.getStatus());
        response.setPaidAmount(schedule.getPaidAmount());
        response.setPaidDate(schedule.getPaidDate());

        List<TransactionResponse> transactions = schedule.getTransactions().stream()
                .map(this::toTransactionResponse)
                .collect(Collectors.toList());
        response.setTransactions(transactions);

        return response;
    }

    private TransactionResponse toTransactionResponse(PaymentTransaction transaction) {
        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());

        // Populate metadata
        if (transaction.getSchedule() != null && transaction.getSchedule().getDesignPayment() != null) {
            com.wd.api.model.DesignPackagePayment payment = transaction.getSchedule().getDesignPayment();
            if (payment.getProject() != null) {
                response.setProjectName(payment.getProject().getName());
                if (payment.getProject().getCustomer() != null) {
                    String fullName = payment.getProject().getCustomer().getFirstName() + " " +
                            payment.getProject().getCustomer().getLastName();
                    response.setCustomerName(fullName.trim());
                }
            }
        }

        response.setAmount(transaction.getAmount());
        response.setPaymentMethod(transaction.getPaymentMethod());
        response.setReferenceNumber(transaction.getReferenceNumber());
        response.setPaymentDate(transaction.getPaymentDate());
        response.setNotes(transaction.getNotes());
        response.setRecordedById(transaction.getRecordedById());
        response.setReceiptNumber(transaction.getReceiptNumber());
        response.setStatus(transaction.getStatus());
        response.setTdsPercentage(transaction.getTdsPercentage());
        response.setTdsAmount(transaction.getTdsAmount());
        response.setNetAmount(transaction.getNetAmount());
        response.setTdsDeductedBy(transaction.getTdsDeductedBy());
        response.setPaymentCategory(transaction.getPaymentCategory());
        response.setCreatedAt(transaction.getCreatedAt());

        if (transaction.getChallan() != null) {
            response.setChallanId(transaction.getChallan().getId());
            response.setChallanNumber(transaction.getChallan().getChallanNumber());
        }

        return response;
    }

    // ===== Phase 2: Retention Money Management =====
    // Moved to SubcontractService.java

    // ===== Phase 2: GST Invoice Generation =====

    @Transactional
    @SuppressWarnings("null")
    public com.wd.api.model.TaxInvoice generateGstInvoice(Long paymentId, String placeOfSupply,
            String customerGstin, Long createdById) {
        logger.info("Generating GST invoice for payment: {}", paymentId);

        DesignPackagePayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        // Check if invoice already exists
        if (taxInvoiceRepository.existsByPaymentId(paymentId)) {
            throw new IllegalStateException("Invoice already exists for this payment");
        }

        com.wd.api.model.TaxInvoice invoice = new com.wd.api.model.TaxInvoice();
        invoice.setPaymentId(paymentId);
        invoice.setInvoiceNumber(taxInvoiceRepository.generateInvoiceNumber());
        invoice.setPlaceOfSupply(placeOfSupply);
        invoice.setCustomerGstin(customerGstin);
        invoice.setCreatedById(createdById);

        // Calculate taxable value (base amount before GST)
        BigDecimal taxableValue = payment.getBaseAmount();
        invoice.setTaxableValue(taxableValue);

        // Determine if interstate or intrastate
        // Company is registered in Kerala (name: KERALA)
        // Compare with place of supply to determine GST type
        String companyState = "KERALA"; // Company's registered state
        boolean isInterstate = placeOfSupply != null
                && !placeOfSupply.trim().isEmpty()
                && !placeOfSupply.toUpperCase().trim().equals(companyState);
        invoice.setIsInterstate(isInterstate);

        BigDecimal gstRate = payment.getGstPercentage();

        BigDecimal totalTax;

        if (isInterstate) {
            // Interstate: IGST (full GST rate)
            invoice.setIgstRate(gstRate);
            invoice.setIgstAmount(taxableValue.multiply(gstRate).divide(HUNDRED, 2, RoundingMode.HALF_UP));
            totalTax = invoice.getIgstAmount();
        } else {
            // Intrastate: CGST + SGST (split equally)
            BigDecimal halfRate = gstRate.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
            BigDecimal halfTax = taxableValue.multiply(halfRate).divide(HUNDRED, 2, RoundingMode.HALF_UP);

            invoice.setCgstRate(halfRate);
            invoice.setCgstAmount(halfTax);
            invoice.setSgstRate(halfRate);
            invoice.setSgstAmount(halfTax);
            totalTax = halfTax.add(halfTax);
        }

        invoice.setTotalTaxAmount(totalTax);
        invoice.setInvoiceTotal(taxableValue.add(totalTax));

        taxInvoiceRepository.save(invoice);
        logger.info("GST invoice generated: {}", invoice.getInvoiceNumber());

        return invoice;
    }
}
