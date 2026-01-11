package com.wd.api.controller;

import com.wd.api.dto.LabourDTO;
import com.wd.api.dto.LabourAttendanceDTO;
import com.wd.api.dto.MeasurementBookDTO;
import com.wd.api.service.LabourService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/labour")
@RequiredArgsConstructor
public class LabourController {

    private final LabourService labourService;

    @PostMapping
    public ResponseEntity<LabourDTO> createLabour(@RequestBody LabourDTO dto) {
        return ResponseEntity.ok(labourService.createLabour(dto));
    }

    @GetMapping
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
}
