package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.LabourDTO;
import com.wd.api.dto.LabourAttendanceDTO;
import com.wd.api.dto.LabourSearchFilter;
import com.wd.api.dto.MeasurementBookDTO;
import com.wd.api.model.Labour;
import com.wd.api.model.PortalUser;
import com.wd.api.model.WageSheet;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.service.LabourService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/labour")
@RequiredArgsConstructor
public class LabourController {

    private final LabourService labourService;
    private final PortalUserRepository portalUserRepository;

    @GetMapping("/search")
    public ResponseEntity<Page<Labour>> searchLabour(@ModelAttribute LabourSearchFilter filter) {
        return ResponseEntity.ok(labourService.searchLabour(filter));
    }

    @PostMapping
    public ResponseEntity<LabourDTO> createLabour(@RequestBody LabourDTO dto) {
        return ResponseEntity.ok(labourService.createLabour(dto));
    }

    @GetMapping
    @Deprecated
    public ResponseEntity<List<LabourDTO>> getAllLabour() {
        return ResponseEntity.ok(labourService.getAllLabour());
    }

    @PostMapping("/attendance")
    public ResponseEntity<List<LabourAttendanceDTO>> recordAttendance(@RequestBody List<LabourAttendanceDTO> dtoList) {
        return ResponseEntity.ok(labourService.recordAttendance(dtoList));
    }

    @PostMapping("/mb")
    public ResponseEntity<MeasurementBookDTO> createMBEntry(@RequestBody MeasurementBookDTO dto) {
        return ResponseEntity.ok(labourService.createMBEntry(dto));
    }

    @GetMapping("/mb/project/{projectId}")
    public ResponseEntity<List<MeasurementBookDTO>> getMBEntries(@PathVariable Long projectId) {
        return ResponseEntity.ok(labourService.getMBEntriesByProject(projectId));
    }

    @PostMapping("/wagesheet/generate")
    public ResponseEntity<com.wd.api.model.WageSheet> generateWageSheet(
            @RequestParam Long projectId,
            @RequestParam String start,
            @RequestParam String end) {
        return ResponseEntity.ok(labourService.generateWageSheet(projectId, java.time.LocalDate.parse(start),
                java.time.LocalDate.parse(end)));
    }

    @PostMapping("/advance")
    public ResponseEntity<com.wd.api.model.LabourAdvance> createAdvance(
            @RequestParam Long labourId,
            @RequestParam java.math.BigDecimal amount,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.ok(labourService.createAdvance(labourId, amount, notes));
    }

    @PutMapping("/wagesheet/{id}/approve")
    public ResponseEntity<?> approveWageSheet(
            @PathVariable Long id,
            @RequestParam(required = false) String notes,
            Authentication auth) {
        Long userId = getCurrentUserId(auth);
        WageSheet sheet = labourService.approveWageSheet(id, userId, notes);
        return ResponseEntity.ok(ApiResponse.success("Wage sheet approved", sheet));
    }

    @PutMapping("/wagesheet/{id}/mark-paid")
    public ResponseEntity<?> markWagesheetPaid(@PathVariable Long id) {
        WageSheet sheet = labourService.markWagesheetPaid(id);
        return ResponseEntity.ok(ApiResponse.success("Wage sheet marked as paid", sheet));
    }

    private Long getCurrentUserId(Authentication auth) {
        String email = auth.getName();
        PortalUser user = portalUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }
}
