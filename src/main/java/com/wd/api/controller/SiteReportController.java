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
import com.wd.api.model.enums.SiteReportStatus;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.SiteVisitRepository;
import com.wd.api.service.SiteReportActivityService;
import com.wd.api.service.SiteReportInputValidator;
import com.wd.api.service.SiteReportService;
import com.wd.api.service.AuthService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.wd.api.dto.SiteReportDto;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/site-reports")
@PreAuthorize("isAuthenticated()")
public class SiteReportController {

    private final SiteReportService siteReportService;
    private final CustomerProjectRepository projectRepository;
    private final SiteVisitRepository siteVisitRepository;
    private final AuthService authService;
    private final ObjectMapper objectMapper;
    private final SiteReportActivityService activityService;

    public SiteReportController(SiteReportService siteReportService,
            CustomerProjectRepository projectRepository,
            SiteVisitRepository siteVisitRepository,
            AuthService authService,
            ObjectMapper objectMapper,
            SiteReportActivityService activityService) {
        this.siteReportService = siteReportService;
        this.projectRepository = projectRepository;
        this.siteVisitRepository = siteVisitRepository;
        this.authService = authService;
        this.objectMapper = objectMapper;
        this.activityService = activityService;
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('SITE_REPORT_VIEW')")
    public ResponseEntity<?> searchSiteReports(@ModelAttribute SiteReportSearchFilter filter) {
        Page<SiteReportDto> reports = siteReportService.searchSiteReports(filter);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/project/{projectId}")
    @Deprecated
    @PreAuthorize("hasAuthority('SITE_REPORT_VIEW')")
    public ResponseEntity<ApiResponse<List<SiteReportDto>>> getReportsByProject(@PathVariable Long projectId) {
        List<SiteReport> reports = siteReportService.getReportsByProject(projectId);
        List<SiteReportDto> reportDtos = reports.stream()
                .map(SiteReportDto::new)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Reports retrieved successfully", reportDtos));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('SITE_REPORT_VIEW')")
    public ResponseEntity<ApiResponse<List<SiteReport>>> getMyReports() {
        PortalUser currentUser = authService.getCurrentUser();
        List<SiteReport> reports = siteReportService.getReportsByUser(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("My reports retrieved successfully", reports));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SITE_REPORT_VIEW')")
    public ResponseEntity<ApiResponse<SiteReport>> getReportById(@PathVariable Long id) {
        SiteReport report = siteReportService.getReportById(id);
        return ResponseEntity.ok(ApiResponse.success("Report retrieved successfully", report));
    }

    @PostMapping(consumes = { "multipart/form-data" })
    @PreAuthorize("hasAuthority('SITE_REPORT_CREATE')")
    public ResponseEntity<ApiResponse<SiteReportDto>> createReport(
            @RequestPart("report") String reportJson,
            @RequestPart(value = "photos", required = true) List<MultipartFile> photos) throws Exception {
        
        // Validate that at least one photo is provided + cap upload size
        if (photos == null || photos.isEmpty()) {
            throw new BusinessException("At least one photo is required for site report submission",
                HttpStatus.BAD_REQUEST, "PHOTOS_REQUIRED");
        }
        SiteReportInputValidator.validatePhotoCount(photos);

        Map<String, Object> reportData;
        try {
            reportData = objectMapper.readValue(reportJson,
                    new TypeReference<Map<String, Object>>() {});
        } catch (com.fasterxml.jackson.core.JsonProcessingException jsonEx) {
            // Wrap parser errors in BusinessException so the error envelope
            // shape is consistent with every other validation in this method.
            throw new BusinessException("Malformed report JSON: " + jsonEx.getOriginalMessage(),
                    HttpStatus.BAD_REQUEST, "INVALID_JSON");
        }
        PortalUser currentUser = authService.getCurrentUser();

        // Validate required fields
        if (reportData.get("title") == null || reportData.get("title").toString().trim().isEmpty()) {
            throw new BusinessException("Title is required", HttpStatus.BAD_REQUEST, "TITLE_REQUIRED");
        }
        if (reportData.get("projectId") == null) {
            throw new BusinessException("Project ID is required", HttpStatus.BAD_REQUEST, "PROJECT_ID_REQUIRED");
        }

        SiteReport report = new SiteReport();
        String title = (String) reportData.get("title");
        SiteReportInputValidator.validateTitleLen(title);
        report.setTitle(title);
        report.setDescription((String) reportData.get("description"));
        report.setStatus(SiteReportStatus.SUBMITTED);
        report.setSubmittedBy(currentUser);

        if (reportData.containsKey("reportType") && reportData.get("reportType") != null) {
            try {
                report.setReportType(ReportType.valueOf(reportData.get("reportType").toString()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid report type: " + reportData.get("reportType"), 
                    HttpStatus.BAD_REQUEST, "INVALID_REPORT_TYPE");
            }
        }

        report.setCreatedByUserId(currentUser.getId());

        Long projectId = parseLong(reportData.get("projectId"));
        CustomerProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        report.setProject(project);

        // Set reporting fields
        if (reportData.containsKey("weather")) {
            String weather = (String) reportData.get("weather");
            SiteReportInputValidator.validateWeatherLen(weather);
            report.setWeather(weather);
        }
        if (reportData.containsKey("manpowerDeployed") && reportData.get("manpowerDeployed") != null) {
            Integer m = parseInt(reportData.get("manpowerDeployed"));
            if (m != null && m < 0) {
                throw new BusinessException("manpowerDeployed must be >= 0",
                        HttpStatus.BAD_REQUEST, "INVALID_MANPOWER");
            }
            report.setManpowerDeployed(m);
        }
        if (reportData.containsKey("equipmentUsed")) {
            report.setEquipmentUsed((String) reportData.get("equipmentUsed"));
        }
        if (reportData.containsKey("workProgress")) {
            report.setWorkProgress((String) reportData.get("workProgress"));
        }

        // Set GPS/Location fields with bounds validation
        if (reportData.containsKey("latitude") && reportData.get("latitude") != null) {
            Double lat = parseDouble(reportData.get("latitude"));
            SiteReportInputValidator.validateLatitude(lat, "report");
            report.setLatitude(lat);
        }
        if (reportData.containsKey("longitude") && reportData.get("longitude") != null) {
            Double lng = parseDouble(reportData.get("longitude"));
            SiteReportInputValidator.validateLongitude(lng, "report");
            report.setLongitude(lng);
        }
        if (reportData.containsKey("locationAccuracy") && reportData.get("locationAccuracy") != null) {
            report.setLocationAccuracy(parseDouble(reportData.get("locationAccuracy")));
        }

        if (reportData.containsKey("siteVisitId") && reportData.get("siteVisitId") != null) {
            Long visitId = parseLong(reportData.get("siteVisitId"));
            SiteVisit visit = siteVisitRepository.findById(visitId)
                    .orElseThrow(() -> new ResourceNotFoundException("SiteVisit", visitId));
            report.setSiteVisit(visit);
        }

        SiteReport savedReport = siteReportService.createReport(report, photos, currentUser);
        return ResponseEntity
                .ok(ApiResponse.success("Report created successfully", new SiteReportDto(savedReport)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SITE_REPORT_EDIT')")
    public ResponseEntity<ApiResponse<Void>> deleteReport(@PathVariable Long id) {
        siteReportService.deleteReport(id);
        return ResponseEntity.ok(ApiResponse.success("Report deleted successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SITE_REPORT_EDIT')")
    public ResponseEntity<ApiResponse<SiteReportDto>> updateReport(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updateData) {
        
        PortalUser currentUser = authService.getCurrentUser();
        SiteReport report = siteReportService.getReportById(id);

        // Verify ownership or admin rights
        if (!report.getSubmittedBy().getId().equals(currentUser.getId()) && 
            (currentUser.getRole() == null || !"ADMIN".equals(currentUser.getRole().getCode()))) {
            throw new BusinessException("You don't have permission to update this report", 
                HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        // Update fields if provided
        if (updateData.containsKey("title")) {
            String title = (String) updateData.get("title");
            SiteReportInputValidator.validateTitleLen(title);
            report.setTitle(title);
        }
        if (updateData.containsKey("description")) {
            report.setDescription((String) updateData.get("description"));
        }
        if (updateData.containsKey("weather")) {
            String weather = (String) updateData.get("weather");
            SiteReportInputValidator.validateWeatherLen(weather);
            report.setWeather(weather);
        }
        if (updateData.containsKey("manpowerDeployed") && updateData.get("manpowerDeployed") != null) {
            Integer m = parseInt(updateData.get("manpowerDeployed"));
            if (m != null && m < 0) {
                throw new BusinessException("manpowerDeployed must be >= 0",
                        HttpStatus.BAD_REQUEST, "INVALID_MANPOWER");
            }
            report.setManpowerDeployed(m);
        }
        if (updateData.containsKey("equipmentUsed")) {
            report.setEquipmentUsed((String) updateData.get("equipmentUsed"));
        }
        if (updateData.containsKey("workProgress")) {
            report.setWorkProgress((String) updateData.get("workProgress"));
        }
        if (updateData.containsKey("latitude") && updateData.get("latitude") != null) {
            Double lat = parseDouble(updateData.get("latitude"));
            SiteReportInputValidator.validateLatitude(lat, "report");
            report.setLatitude(lat);
        }
        if (updateData.containsKey("longitude") && updateData.get("longitude") != null) {
            Double lng = parseDouble(updateData.get("longitude"));
            SiteReportInputValidator.validateLongitude(lng, "report");
            report.setLongitude(lng);
        }

        SiteReport updatedReport = siteReportService.updateReport(report);
        return ResponseEntity.ok(ApiResponse.success("Report updated successfully", 
            new SiteReportDto(updatedReport)));
    }

    @PostMapping("/{id}/photos")
    @PreAuthorize("hasAuthority('SITE_REPORT_EDIT')")
    public ResponseEntity<ApiResponse<SiteReportDto>> addPhotosToReport(
            @PathVariable Long id,
            @RequestPart(value = "photos", required = true) List<MultipartFile> photos,
            @RequestPart(value = "metadata", required = false) String metadataJson) throws Exception {

        SiteReportInputValidator.validatePhotoCount(photos);
        PortalUser currentUser = authService.getCurrentUser();
        
        // Parse photo metadata if provided
        List<Map<String, Object>> metadata = null;
        if (metadataJson != null && !metadataJson.isEmpty()) {
            metadata = objectMapper.readValue(metadataJson, 
                new TypeReference<List<Map<String, Object>>>() {});
        }

        SiteReport updatedReport = siteReportService.addPhotosToReport(id, photos, metadata, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Photos added successfully", 
            new SiteReportDto(updatedReport)));
    }

    @DeleteMapping("/{reportId}/photos/{photoId}")
    @PreAuthorize("hasAuthority('SITE_REPORT_EDIT')")
    public ResponseEntity<ApiResponse<Void>> deletePhoto(
            @PathVariable Long reportId,
            @PathVariable Long photoId) {
        
        PortalUser currentUser = authService.getCurrentUser();
        siteReportService.deletePhoto(reportId, photoId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Photo deleted successfully"));
    }

    /**
     * Replace-all save of the per-activity manpower breakdown (V84).
     * The Flutter form sends the full list each time; service deletes
     * the prior set and inserts these rows.
     */
    @PutMapping("/{id}/activities")
    @PreAuthorize("hasAuthority('SITE_REPORT_EDIT')")
    public ResponseEntity<?> replaceActivities(
            @PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestBody java.util.List<com.wd.api.dto.SiteReportActivityRequest> activities) {
        try {
            java.util.List<com.wd.api.dto.SiteReportActivityResponse> body =
                    activityService.replaceActivities(id, activities).stream()
                            .map(com.wd.api.dto.SiteReportActivityResponse::from)
                            .toList();
            return ResponseEntity.ok(ApiResponse.success("Activities saved", body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}/activities")
    @PreAuthorize("hasAuthority('SITE_REPORT_VIEW')")
    public ResponseEntity<?> listActivities(@PathVariable Long id) {
        java.util.List<com.wd.api.dto.SiteReportActivityResponse> body =
                activityService.listForReport(id).stream()
                        .map(com.wd.api.dto.SiteReportActivityResponse::from)
                        .toList();
        return ResponseEntity.ok(ApiResponse.success("Activities", body));
    }

    @PutMapping("/{id}/photos/order")
    @PreAuthorize("hasAuthority('SITE_REPORT_EDIT')")
    public ResponseEntity<ApiResponse<Void>> reorderPhotos(
            @PathVariable Long id,
            @RequestBody Map<String, List<Long>> orderData) {
        
        PortalUser currentUser = authService.getCurrentUser();
        List<Long> photoIds = orderData.get("photoIds");
        
        if (photoIds == null || photoIds.isEmpty()) {
            throw new BusinessException("Photo IDs are required", 
                HttpStatus.BAD_REQUEST, "PHOTO_IDS_REQUIRED");
        }

        siteReportService.reorderPhotos(id, photoIds, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Photos reordered successfully"));
    }
    private Long parseLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.valueOf(new java.math.BigDecimal(val.toString()).setScale(0, java.math.RoundingMode.HALF_UP).toPlainString());
    }

    private Integer parseInt(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).intValue();
        return Integer.valueOf(new java.math.BigDecimal(val.toString()).setScale(0, java.math.RoundingMode.HALF_UP).toPlainString());
    }

    private Double parseDouble(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).doubleValue();
        return Double.valueOf(val.toString());
    }
}
