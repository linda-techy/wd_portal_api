package com.wd.api.controller;

import com.wd.api.dto.scheduling.PredecessorListRequest;
import com.wd.api.model.scheduling.TaskPredecessor;
import com.wd.api.service.scheduling.TaskGraphValidator;
import com.wd.api.service.scheduling.TaskPredecessorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks/{taskId}/predecessors")
@PreAuthorize("isAuthenticated()")
public class TaskPredecessorController {

    private final TaskPredecessorService service;

    public TaskPredecessorController(TaskPredecessorService service) {
        this.service = service;
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('TASK_EDIT','TASK_CREATE')")
    public List<TaskPredecessor> replace(@PathVariable Long taskId,
                                         @Valid @RequestBody PredecessorListRequest req) {
        List<TaskPredecessorService.PredecessorEntry> entries = req.predecessors().stream()
                .map(e -> new TaskPredecessorService.PredecessorEntry(e.predecessorId(), e.lagDays()))
                .toList();
        return service.replacePredecessors(taskId, entries);
    }

    @ExceptionHandler(TaskGraphValidator.CycleDetectedException.class)
    public ResponseEntity<String> handleCycle(TaskGraphValidator.CycleDetectedException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadInput(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
