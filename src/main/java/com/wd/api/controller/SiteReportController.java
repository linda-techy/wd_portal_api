package com.wd.api.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.SiteReportSearchFilter;
import com.wd.api.exception.BusinessException;
import com.wd.api.exception.ResourceNotFoundException;
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
import org.springframework.http.HttpStatus;
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
        Page<SiteReport> reports = siteReportService.searchSiteReports(filter);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/project/{projectId}")
    @Deprecated
    public ResponseEntity<ApiResponse<List<SiteReportDto>>> getReportsByProject(@PathVariable Long projectId) {
        List<SiteReport> reports = siteReportService.getReportsByProject(projectId);
        List<SiteReportDto> reportDtos = reports.stream()
                .map(SiteReportDto::new)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Reports retrieved successfully", reportDtos));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<SiteReport>>> getMyReports() {
        PortalUser currentUser = authService.getCurrentUser();
        List<SiteReport> reports = siteReportService.getReportsByUser(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("My reports retrieved successfully", reports));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SiteReport>> getReportById(@PathVariable Long id) {
        SiteReport report = siteReportService.getReportById(id);
        return ResponseEntity.ok(ApiResponse.success("Report retrieved successfully", report));
    }

    @PostMapping(consumes = { "multipart/form-data" })
    public ResponseEntity<ApiResponse<SiteReportDto>> createReport(
            @RequestPart("report") String reportJson,
            @RequestPart(value = "photos", required = true) List<MultipartFile> photos) throws Exception {
        
        // Validate that at least one photo is provided
        if (photos == null || photos.isEmpty()) {
            throw new BusinessException("At least one photo is required for site report submission", 
                HttpStatus.BAD_REQUEST, "PHOTOS_REQUIRED");
        }

        Map<String, Object> reportData = objectMapper.readValue(reportJson,
                new TypeReference<Map<String, Object>>() {});
        PortalUser currentUser = authService.getCurrentUser();

        // Validate required fields
        if (reportData.get("title") == null || reportData.get("title").toString().trim().isEmpty()) {
            throw new BusinessException("Title is required", HttpStatus.BAD_REQUEST, "TITLE_REQUIRED");
        }
        if (reportData.get("projectId") == null) {
            throw new BusinessException("Project ID is required", HttpStatus.BAD_REQUEST, "PROJECT_ID_REQUIRED");
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
                throw new BusinessException("Invalid report type: " + reportData.get("reportType"), 
                    HttpStatus.BAD_REQUEST, "INVALID_REPORT_TYPE");
            }
        }

        report.setCreatedByUserId(currentUser.getId());

        Long projectId = Long.valueOf(reportData.get("projectId").toString());
        @SuppressWarnings("null")
        CustomerProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
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
                    .orElseThrow(() -> new ResourceNotFoundException("SiteVisit", visitId));
            report.setSiteVisit(visit);
        }

        SiteReport savedReport = siteReportService.createReport(report, photos, currentUser);
        return ResponseEntity
                .ok(ApiResponse.success("Report created successfully", new SiteReportDto(savedReport)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReport(@PathVariable Long id) {
        siteReportService.deleteReport(id);
        return ResponseEntity.ok(ApiResponse.success("Report deleted successfully"));
    }
}
