package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.CustomerSiteReportDto;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.CustomerUser;
import com.wd.api.model.SiteReport;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.SiteReportRepository;
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
 * Controller for customer-facing site reports API.
 * Customers can view site reports for their projects only.
 */
@RestController
@RequestMapping("/api/customer/site-reports")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerSiteReportController {

        private static final Logger logger = LoggerFactory.getLogger(CustomerSiteReportController.class);

        private final SiteReportRepository siteReportRepository;
        private final CustomerProjectRepository projectRepository;
        private final AuthService authService;

        public CustomerSiteReportController(
                        SiteReportRepository siteReportRepository,
                        CustomerProjectRepository projectRepository,
                        AuthService authService) {
                this.siteReportRepository = siteReportRepository;
                this.projectRepository = projectRepository;
                this.authService = authService;
        }

        /**
         * Get all site reports for projects associated with the current customer.
         * 
         * @param projectId Optional filter by specific project
         * @param page      Page number (default: 0)
         * @param size      Page size (default: 20)
         * @return Paginated list of site reports with photos (customer-safe DTOs)
         */
        @GetMapping
        public ResponseEntity<ApiResponse<Page<CustomerSiteReportDto>>> getCustomerSiteReports(
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
                                        logger.warn("Customer {} attempted to access reports for unauthorized project {}",
                                                        currentCustomer.getId(), projectId);
                                        return ResponseEntity.status(403)
                                                        .body(ApiResponse.error("Access denied to this project"));
                                }
                                projectIds = List.of(projectId);
                        }

                        // Create pageable with sorting by report date descending
                        Pageable pageable = PageRequest.of(page, size, Sort.by("reportDate").descending());

                        // Fetch site reports for customer's projects
                        Page<SiteReport> reports = siteReportRepository.findByProjectIdIn(projectIds, pageable);

                        // Convert to customer-safe DTOs
                        Page<CustomerSiteReportDto> reportDtos = reports.map(CustomerSiteReportDto::new);

                        return ResponseEntity.ok(ApiResponse.success(
                                        "Site reports retrieved successfully",
                                        reportDtos));

                } catch (Exception e) {
                        logger.error("Error fetching customer site reports: {}", e.getMessage(), e);
                        return ResponseEntity.status(500)
                                        .body(ApiResponse.error("Failed to retrieve site reports: " + e.getMessage()));
                }
        }

        /**
         * Get a specific site report by ID.
         * Verifies the customer has access to the project.
         * 
         * @param id Site report ID
         * @return Site report with all photos (customer-safe DTO)
         */
        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<CustomerSiteReportDto>> getSiteReportById(@PathVariable Long id) {
                try {
                        CustomerUser currentCustomer = authService.getCurrentCustomerUser();

                        @SuppressWarnings("null")
                        SiteReport report = siteReportRepository.findById(id)
                                        .orElse(null);

                        if (report == null) {
                                return ResponseEntity.status(404)
                                                .body(ApiResponse.error("Site report not found with id: " + id));
                        }

                        // Verify customer has access to this project
                        List<CustomerProject> customerProjects = projectRepository
                                        .findByCustomer_IdAndDeletedAtIsNull(currentCustomer.getId());
                        boolean hasAccess = report.getProject() != null && customerProjects.stream()
                                        .anyMatch(p -> p.getId().equals(report.getProject().getId()));

                        if (!hasAccess) {
                                logger.warn("Customer {} attempted to access unauthorized report {}",
                                                currentCustomer.getId(), id);
                                return ResponseEntity.status(403)
                                                .body(ApiResponse.error("Access denied to this report"));
                        }

                        // Convert to customer-safe DTO
                        CustomerSiteReportDto reportDto = new CustomerSiteReportDto(report);

                        return ResponseEntity.ok(ApiResponse.success(
                                        "Site report retrieved successfully",
                                        reportDto));

                } catch (Exception e) {
                        logger.error("Error fetching site report id={}: {}", id, e.getMessage(), e);
                        return ResponseEntity.status(500)
                                        .body(ApiResponse.error("Failed to retrieve site report: " + e.getMessage()));
                }
        }
}
