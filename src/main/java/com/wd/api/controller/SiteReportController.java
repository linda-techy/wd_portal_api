package com.wd.api.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.SiteReportSearchFilter;
import com.wd.api.model.SiteReport;
import com.wd.api.model.PortalUser;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.SiteVisit;
import com.wd.api.model.enums.ReportType;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.SiteVisitRepository;
import com.wd.api.service.SiteReportService;
import com.wd.api.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.wd.api.dto.SiteReportDto;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/site-reports")
public class SiteReportController {

    private static final Logger logger = LoggerFactory.getLogger(SiteReportController.class);
    private final SiteReportService siteReportService;
    private final CustomerProjectRepository projectRepository;
    private final SiteVisitRepository siteVisitRepository;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public SiteReportController(SiteReportService siteReportService,
            CustomerProjectRepository projectRepository,
            SiteVisitRepository siteVisitRepository,
            AuthService authService,
            ObjectMapper objectMapper) {
        this.siteReportService = siteReportService;
        this.projectRepository = projectRepository;
        this.siteVisitRepository = siteVisitRepository;
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchSiteReports(@ModelAttribute SiteReportSearchFilter filter) {
        try {
            Page<SiteReport> reports = siteReportService.searchSiteReports(filter);
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            logger.error("Error searching site reports: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to search site reports: " + e.getMessage()));
        }
    }

    @GetMapping("/project/{projectId}")
    @Deprecated
    public ResponseEntity<ApiResponse<List<SiteReportDto>>> getReportsByProject(@PathVariable Long projectId) {
        try {
            List<SiteReport> reports = siteReportService.getReportsByProject(projectId);
            List<SiteReportDto> reportDtos = reports.stream()
                    .map(SiteReportDto::new)
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success("Reports retrieved successfully", reportDtos));
        } catch (Exception e) {
            logger.error("Error fetching project reports for projectId={}: {}", projectId, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to retrieve project reports: " + e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<SiteReport>>> getMyReports() {
        try {
            PortalUser currentUser = authService.getCurrentUser();
            List<SiteReport> reports = siteReportService.getReportsByUser(currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success("My reports retrieved successfully", reports));
        } catch (Exception e) {
            logger.error("Error fetching my reports: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to retrieve your reports: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SiteReport>> getReportById(@PathVariable Long id) {
        try {
            SiteReport report = siteReportService.getReportById(id);
            if (report == null) {
                return ResponseEntity.status(404).body(ApiResponse.error("Report not found"));
            }
            return ResponseEntity.ok(ApiResponse.success("Report retrieved successfully", report));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                logger.warn("Site report not found: id={}", id);
                return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
            }
            logger.error("Error fetching report by id={}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to retrieve report: " + e.getMessage()));
        }
    }

    @PostMapping(consumes = { "multipart/form-data" })
    public ResponseEntity<ApiResponse<SiteReportDto>> createReport(
            @RequestPart("report") String reportJson,
            @RequestPart(value = "photos", required = true) List<MultipartFile> photos) {
        try {
            // Validate that at least one photo is provided
            if (photos == null || photos.isEmpty()) {
                logger.warn("Attempt to create site report without photos");
                return ResponseEntity.status(400)
                        .body(ApiResponse.error("At least one photo is required for site report submission"));
            }

            Map<String, Object> reportData = objectMapper.readValue(reportJson,
                    new TypeReference<Map<String, Object>>() {
                    });
            PortalUser currentUser = authService.getCurrentUser();

            // Validate required fields
            if (reportData.get("title") == null || reportData.get("title").toString().trim().isEmpty()) {
                return ResponseEntity.status(400)
                        .body(ApiResponse.error("Title is required"));
            }
            if (reportData.get("projectId") == null) {
                return ResponseEntity.status(400)
                        .body(ApiResponse.error("Project ID is required"));
            }

            SiteReport report = new SiteReport();
            report.setTitle((String) reportData.get("title"));
            report.setDescription((String) reportData.get("description"));
            report.setStatus("SUBMITTED");
            report.setSubmittedBy(currentUser);

            if (reportData.containsKey("reportType") && reportData.get("reportType") != null) {
                try {
                    report.setReportType(ReportType.valueOf((String) reportData.get("reportType")));
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid report type: {}", reportData.get("reportType"));
                    return ResponseEntity.status(400)
                            .body(ApiResponse.error("Invalid report type: " + reportData.get("reportType")));
                }
            }

            report.setCreatedByUserId(currentUser.getId());

            Long projectId = Long.valueOf(reportData.get("projectId").toString());
            @SuppressWarnings("null")
            CustomerProject project = projectRepository.findById(projectId)
                    .orElse(null);
            if (project == null) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("Project not found with id: " + projectId));
            }
            report.setProject(project);

            // Set reporting fields
            if (reportData.containsKey("weather")) {
                report.setWeather((String) reportData.get("weather"));
            }
            if (reportData.containsKey("manpowerDeployed") && reportData.get("manpowerDeployed") != null) {
                report.setManpowerDeployed(Integer.valueOf(reportData.get("manpowerDeployed").toString()));
            }
            if (reportData.containsKey("equipmentUsed")) {
                report.setEquipmentUsed((String) reportData.get("equipmentUsed"));
            }
            if (reportData.containsKey("workProgress")) {
                report.setWorkProgress((String) reportData.get("workProgress"));
            }

            if (reportData.containsKey("siteVisitId") && reportData.get("siteVisitId") != null) {
                Long visitId = Long.valueOf(reportData.get("siteVisitId").toString());
                @SuppressWarnings("null")
                SiteVisit visit = siteVisitRepository.findById(visitId)
                        .orElse(null);
                if (visit == null) {
                    return ResponseEntity.status(404)
                            .body(ApiResponse.error("Site Visit not found with id: " + visitId));
                }
                report.setSiteVisit(visit);
            }

            SiteReport savedReport = siteReportService.createReport(report, photos);
            return ResponseEntity
                    .ok(ApiResponse.success("Report created successfully", new SiteReportDto(savedReport)));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            logger.error("Invalid JSON in report data: {}", e.getMessage());
            return ResponseEntity.status(400)
                    .body(ApiResponse.error("Invalid report data format: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating site report: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to create site report: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReport(@PathVariable Long id) {
        try {
            siteReportService.deleteReport(id);
            return ResponseEntity.ok(ApiResponse.success("Report deleted successfully"));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                logger.warn("Attempted to delete non-existent report: id={}", id);
                return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
            }
            logger.error("Error deleting report id={}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to delete report: " + e.getMessage()));
        }
    }
}
