package com.wd.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wd.api.model.SiteReport;
import com.wd.api.model.PortalUser;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.SiteVisit;
import com.wd.api.model.enums.ReportType;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.SiteVisitRepository;
import com.wd.api.service.SiteReportService;
import com.wd.api.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/site-reports")
public class SiteReportController {

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
    public ResponseEntity<List<SiteReport>> getReportsByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(siteReportService.getReportsByProject(projectId));
    }

    @GetMapping("/me")
    public ResponseEntity<List<SiteReport>> getMyReports() {
        PortalUser currentUser = authService.getCurrentUser();
        return ResponseEntity.ok(siteReportService.getReportsByUser(currentUser.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SiteReport> getReportById(@PathVariable Long id) {
        return ResponseEntity.ok(siteReportService.getReportById(id));
    }

    @PostMapping(consumes = { "multipart/form-data" })
    public ResponseEntity<SiteReport> createReport(
            @RequestPart("report") String reportJson,
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos) throws Exception {

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

        return ResponseEntity.ok(siteReportService.createReport(report, photos));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable Long id) {
        siteReportService.deleteReport(id);
        return ResponseEntity.ok().build();
    }
}
