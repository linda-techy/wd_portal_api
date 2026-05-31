package com.wd.api.dto;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.Lead;
import com.wd.api.model.Task;
import com.wd.api.model.Task.TaskPriority;
import com.wd.api.model.Task.TaskStatus;

import java.time.LocalDate;

/**
 * Request DTO for create/update task endpoints (S4684).
 *
 * Replaces direct @RequestBody binding of the JPA {@link Task} entity. Field
 * names/types mirror exactly what the Task entity exposed for inbound JSON
 * binding, so the wire contract is unchanged.
 *
 * Excluded (server-managed / not JSON-bindable on the entity):
 *  - id, createdAt, updatedAt, deletedAt, version, audit ids (BaseEntity)
 *  - createdBy  (@JsonIgnore; stamped from the auth principal in the service)
 *  - assignedTo (@JsonIgnore; set via the dedicated PUT /{id}/assign endpoint)
 *
 * Kept client-supplied relations as the same entity types so callers can still
 * pass {@code "project": {"id": 50}} / {@code "lead": {"id": 7}} on the body.
 */
public record TaskRequest(
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        CustomerProject project,
        Lead lead,
        @jakarta.validation.constraints.NotNull(message = "Due date is mandatory for task accountability and project timeline tracking")
        LocalDate dueDate,
        LocalDate startDate,
        LocalDate endDate,
        Integer progressPercent,
        Boolean customerVisible,
        Long milestoneId,
        LocalDate actualEndDate,
        LocalDate actualStartDate,
        LocalDate esDate,
        LocalDate efDate,
        LocalDate lsDate,
        LocalDate lfDate,
        Integer totalFloatDays,
        Boolean isCritical,
        Boolean monsoonSensitive,
        String rejectionReason,
        Integer weight,
        Integer durationDays) {

    public Task toEntity() {
        Task t = new Task();
        t.setTitle(title);
        t.setDescription(description);
        t.setStatus(status);
        t.setPriority(priority);
        t.setProject(project);
        t.setLead(lead);
        t.setDueDate(dueDate);
        t.setStartDate(startDate);
        t.setEndDate(endDate);
        t.setProgressPercent(progressPercent);
        t.setCustomerVisible(customerVisible);
        t.setMilestoneId(milestoneId);
        t.setActualEndDate(actualEndDate);
        t.setActualStartDate(actualStartDate);
        t.setEsDate(esDate);
        t.setEfDate(efDate);
        t.setLsDate(lsDate);
        t.setLfDate(lfDate);
        t.setTotalFloatDays(totalFloatDays);
        t.setIsCritical(isCritical);
        t.setMonsoonSensitive(monsoonSensitive);
        t.setRejectionReason(rejectionReason);
        t.setWeight(weight);
        t.setDurationDays(durationDays);
        return t;
    }
}
