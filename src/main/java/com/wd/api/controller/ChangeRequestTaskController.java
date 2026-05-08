package com.wd.api.controller;

import com.wd.api.dto.changerequest.ChangeRequestTaskCreateRequest;
import com.wd.api.dto.changerequest.ChangeRequestTaskDto;
import com.wd.api.dto.changerequest.ChangeRequestTaskPredecessorRequest;
import com.wd.api.dto.changerequest.ChangeRequestTaskUpdateRequest;
import com.wd.api.model.PortalUser;
import com.wd.api.model.changerequest.ChangeRequestTask;
import com.wd.api.model.changerequest.ChangeRequestTaskPredecessor;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.service.changerequest.ChangeRequestTaskService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Author-time admin endpoints for a Change Request's proposed scope. Mounts
 * under /api/projects/{projectId}/change-requests/{crId}/tasks. All mutating
 * operations require {@code CR_SUBMIT} authority and the parent CR to be in
 * DRAFT or SUBMITTED status (enforced at the service layer); reads require
 * {@code TASK_VIEW}.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/change-requests/{crId}")
public class ChangeRequestTaskController {

    @Autowired private ChangeRequestTaskService service;
    @Autowired private PortalUserRepository portalUserRepository;

    @PostMapping("/tasks")
    @PreAuthorize("hasAuthority('CR_SUBMIT')")
    public ChangeRequestTaskDto addTask(@PathVariable Long projectId,
                                        @PathVariable Long crId,
                                        @RequestBody @Valid ChangeRequestTaskCreateRequest req,
                                        Authentication auth) {
        ChangeRequestTask saved = service.addTask(crId, req, getCurrentUserId(auth));
        return ChangeRequestTaskDto.from(saved);
    }

    @PatchMapping("/tasks/{taskId}")
    @PreAuthorize("hasAuthority('CR_SUBMIT')")
    public ChangeRequestTaskDto updateTask(@PathVariable Long projectId,
                                           @PathVariable Long crId,
                                           @PathVariable Long taskId,
                                           @RequestBody @Valid ChangeRequestTaskUpdateRequest req,
                                           Authentication auth) {
        return ChangeRequestTaskDto.from(service.updateTask(crId, taskId, req, getCurrentUserId(auth)));
    }

    @DeleteMapping("/tasks/{taskId}")
    @PreAuthorize("hasAuthority('CR_SUBMIT')")
    public ResponseEntity<Void> deleteTask(@PathVariable Long projectId,
                                           @PathVariable Long crId,
                                           @PathVariable Long taskId,
                                           Authentication auth) {
        service.removeTask(crId, taskId, getCurrentUserId(auth));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tasks/predecessors")
    @PreAuthorize("hasAuthority('CR_SUBMIT')")
    public ChangeRequestTaskPredecessor addPredecessor(@PathVariable Long projectId,
                                                       @PathVariable Long crId,
                                                       @RequestBody @Valid ChangeRequestTaskPredecessorRequest req,
                                                       Authentication auth) {
        return service.addPredecessor(crId, req, getCurrentUserId(auth));
    }

    @GetMapping("/tasks")
    @PreAuthorize("hasAuthority('TASK_VIEW')")
    public List<ChangeRequestTaskDto> listTasks(@PathVariable Long projectId,
                                                @PathVariable Long crId) {
        return service.listTasks(crId).stream().map(ChangeRequestTaskDto::from).toList();
    }

    private Long getCurrentUserId(Authentication auth) {
        if (auth == null || auth.getName() == null) return null;
        return portalUserRepository.findByEmail(auth.getName()).map(PortalUser::getId).orElse(null);
    }
}
