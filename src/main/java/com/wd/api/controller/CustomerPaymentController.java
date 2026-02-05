package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.CustomerPaymentScheduleDto;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.CustomerUser;
import com.wd.api.model.PaymentSchedule;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.PaymentScheduleRepository;
import com.wd.api.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for customer-facing payment API.
 * Customers can view payment schedules and transactions for their projects
 * only.
 */
@RestController
@RequestMapping("/api/customer/payments")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerPaymentController {

        private static final Logger logger = LoggerFactory.getLogger(CustomerPaymentController.class);

        private final PaymentScheduleRepository paymentScheduleRepository;
        private final CustomerProjectRepository projectRepository;
        private final AuthService authService;

        public CustomerPaymentController(
                        PaymentScheduleRepository paymentScheduleRepository,
                        CustomerProjectRepository projectRepository,
                        AuthService authService) {
                this.paymentScheduleRepository = paymentScheduleRepository;
                this.projectRepository = projectRepository;
                this.authService = authService;
        }

        /**
         * Get all payment schedules for projects associated with the current customer.
         * 
         * @param projectId Optional filter by specific project
         * @param page      Page number (default: 0)
         * @param size      Page size (default: 20)
         * @return Paginated list of payment schedules with transactions (customer-safe
         *         DTOs)
         */
        @GetMapping
        public ResponseEntity<ApiResponse<Page<CustomerPaymentScheduleDto>>> getCustomerPayments(
                        @RequestParam(required = false) Long projectId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {
                try {
                        // Get current customer user
                        CustomerUser currentCustomer = authService.getCurrentCustomerUser();

                        // Get all projects associated with this customer
                        List<CustomerProject> customerProjects = projectRepository
                                        .findByCustomer_IdAndDeletedAtIsNull(currentCustomer.getId());
                        List<Long> projectIds = customerProjects.stream()
                                        .map(CustomerProject::getId)
                                        .collect(Collectors.toList());

                        if (projectIds.isEmpty()) {
                                return ResponseEntity.ok(ApiResponse.success(
                                                "No projects found for customer",
                                                Page.empty()));
                        }

                        // Filter by specific project if provided
                        if (projectId != null) {
                                if (!projectIds.contains(projectId)) {
                                        logger.warn("Customer {} attempted to access payments for unauthorized project {}",
                                                        currentCustomer.getId(), projectId);
                                        return ResponseEntity.status(403)
                                                        .body(ApiResponse.error("Access denied to this project"));
                                }
                                projectIds = List.of(projectId);
                        }

                        // Create pageable with sorting by due date
                        Pageable pageable = PageRequest.of(page, size, Sort.by("dueDate").ascending());

                        // Fetch payment schedules for customer's projects
                        Page<PaymentSchedule> payments = paymentScheduleRepository.findByProjectIdIn(projectIds,
                                        pageable);

                        // Convert to customer-safe DTOs
                        Page<CustomerPaymentScheduleDto> paymentDtos = payments.map(CustomerPaymentScheduleDto::new);

                        return ResponseEntity.ok(ApiResponse.success(
                                        "Payment schedules retrieved successfully",
                                        paymentDtos));

                } catch (Exception e) {
                        logger.error("Error fetching customer payments", e);
                        return ResponseEntity.status(500)
                                        .body(ApiResponse.error("Failed to retrieve payment schedules"));
                }
        }

        /**
         * Get a specific payment schedule by ID.
         * Verifies the customer has access to the project.
         * 
         * @param id Payment schedule ID
         * @return Payment schedule with all transactions (customer-safe DTO)
         */
        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<CustomerPaymentScheduleDto>> getPaymentScheduleById(@PathVariable Long id) {
                try {
                        CustomerUser currentCustomer = authService.getCurrentCustomerUser();

                        PaymentSchedule schedule = paymentScheduleRepository.findById(id)
                                        .orElse(null);

                        if (schedule == null) {
                                return ResponseEntity.status(404)
                                                .body(ApiResponse.error("Payment schedule not found"));
                        }

                        // CRITICAL FIX: PaymentSchedule -> DesignPackagePayment -> CustomerProject
                        // schedule.getProject() doesn't exist!
                        Long scheduleProjectId = schedule.getDesignPayment() != null
                                        && schedule.getDesignPayment().getProject() != null
                                                        ? schedule.getDesignPayment().getProject().getId()
                                                        : null;

                        if (scheduleProjectId == null) {
                                logger.error("Payment schedule {} has no associated project", id);
                                return ResponseEntity.status(500)
                                                .body(ApiResponse.error("Invalid payment schedule data"));
                        }

                        // Verify customer has access to this project
                        List<CustomerProject> customerProjects = projectRepository
                                        .findByCustomer_IdAndDeletedAtIsNull(currentCustomer.getId());
                        boolean hasAccess = customerProjects.stream()
                                        .anyMatch(p -> p.getId().equals(scheduleProjectId));

                        if (!hasAccess) {
                                logger.warn("Customer {} attempted to access unauthorized payment schedule {}",
                                                currentCustomer.getId(), id);
                                return ResponseEntity.status(403)
                                                .body(ApiResponse.error("Access denied to this payment schedule"));
                        }

                        // Convert to customer-safe DTO
                        CustomerPaymentScheduleDto scheduleDto = new CustomerPaymentScheduleDto(schedule);

                        return ResponseEntity.ok(ApiResponse.success(
                                        "Payment schedule retrieved successfully",
                                        scheduleDto));

                } catch (Exception e) {
                        logger.error("Error fetching payment schedule by ID", e);
                        return ResponseEntity.status(500)
                                        .body(ApiResponse.error("Failed to retrieve payment schedule"));
                }
        }
}
