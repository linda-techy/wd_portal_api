package com.wd.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wd.api.dto.ApiResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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

    @GetMapping("/project/{projectId}")
    public ResponseEntity<ApiResponse<List<SiteReport>>> getReportsByProject(@PathVariable Long projectId) {
        try {
            List<SiteReport> reports = siteReportService.getReportsByProject(projectId);
            return ResponseEntity.ok(ApiResponse.success("Reports retrieved successfully", reports));
        } catch (Exception e) {
            logger.error("Error fetching project reports", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<SiteReport>>> getMyReports() {
        try {
            PortalUser currentUser = authService.getCurrentUser();
            List<SiteReport> reports = siteReportService.getReportsByUser(currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success("My reports retrieved successfully", reports));
        } catch (Exception e) {
            logger.error("Error fetching my reports", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
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
        } catch (Exception e) {
            logger.error("Error fetching report by ID", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    @PostMapping(consumes = { "multipart/form-data" })
    public ResponseEntity<ApiResponse<SiteReport>> createReport(
            @RequestPart("report") String reportJson,
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos) {
        try {
            Map<String, Object> reportData = objectMapper.readValue(reportJson, Map.class);
            PortalUser currentUser = authService.getCurrentUser();

            SiteReport report = new SiteReport();
            report.setTitle((String) reportData.get("title"));
            report.setDescription((String) reportData.get("description"));
            report.setStatus("SUBMITTED");
            report.setSubmittedBy(currentUser);

            if (reportData.containsKey("reportType")) {
                report.setReportType(ReportType.valueOf((String) reportData.get("reportType")));
            }

            Long projectId = Long.valueOf(reportData.get("projectId").toString());
            CustomerProject project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found"));
            report.setProject(project);

            if (reportData.containsKey("siteVisitId") && reportData.get("siteVisitId") != null) {
                Long visitId = Long.valueOf(reportData.get("siteVisitId").toString());
                SiteVisit visit = siteVisitRepository.findById(visitId)
                        .orElseThrow(() -> new RuntimeException("Site Visit not found"));
                report.setSiteVisit(visit);
            }

            SiteReport savedReport = siteReportService.createReport(report, photos);
            return ResponseEntity.ok(ApiResponse.success("Report created successfully", savedReport));
        } catch (Exception e) {
            logger.error("Error creating site report", e);
            return ResponseEntity.status(500).body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReport(@PathVariable Long id) {
        try {
            siteReportService.deleteReport(id);
            return ResponseEntity.ok(ApiResponse.success("Report deleted successfully"));
        } catch (Exception e) {
            logger.error("Error deleting report", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }
}
