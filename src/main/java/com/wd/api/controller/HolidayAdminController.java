package com.wd.api.controller;

import com.wd.api.dto.scheduling.HolidayDto;
import com.wd.api.model.scheduling.Holiday;
import com.wd.api.model.scheduling.HolidayScope;
import com.wd.api.repository.HolidayRepository;
import com.wd.api.service.scheduling.HolidayService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/holidays")
@PreAuthorize("isAuthenticated()")
public class HolidayAdminController {

    private final HolidayRepository repo;
    private final HolidayService holidayService;

    public HolidayAdminController(HolidayRepository repo, HolidayService holidayService) {
        this.repo = repo;
        this.holidayService = holidayService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('HOLIDAY_VIEW')")
    public List<HolidayDto> list(@RequestParam HolidayScope scope, @RequestParam int year) {
        return repo.findByScopeAndDate_Year(scope, year).stream().map(HolidayAdminController::toDto).toList();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('HOLIDAY_MANAGE')")
    public ResponseEntity<HolidayDto> create(@Valid @RequestBody HolidayDto dto) {
        Holiday h = fromDto(new Holiday(), dto);
        h.setSource("admin");
        Holiday saved = repo.save(h);
        holidayService.evictAll();
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('HOLIDAY_MANAGE')")
    public HolidayDto update(@PathVariable Long id, @Valid @RequestBody HolidayDto dto) {
        Holiday h = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Holiday not found: " + id));
        fromDto(h, dto);
        Holiday saved = repo.save(h);
        holidayService.evictAll();
        return toDto(saved);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('HOLIDAY_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repo.deleteById(id);
        holidayService.evictAll();
        return ResponseEntity.noContent().build();
    }

    private static HolidayDto toDto(Holiday h) {
        return new HolidayDto(
                h.getId(), h.getCode(), h.getName(), h.getDate(),
                h.getScope(), h.getScopeRef(), h.getRecurrenceType(), h.getActive());
    }

    private static Holiday fromDto(Holiday h, HolidayDto dto) {
        h.setCode(dto.code());
        h.setName(dto.name());
        h.setDate(dto.date());
        h.setScope(dto.scope());
        h.setScopeRef(dto.scopeRef());
        h.setRecurrenceType(dto.recurrenceType());
        if (dto.active() != null) h.setActive(dto.active());
        return h;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadInput(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
