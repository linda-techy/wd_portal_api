package com.wd.api.exception;

import com.wd.api.model.enums.ProjectPhase;
import com.wd.api.model.enums.ProjectStatus;

/**
 * Exception thrown when attempting an invalid project state transition.
 * For example, trying to move from COMPLETION back to DESIGN phase.
 */
public class InvalidProjectStateException extends RuntimeException {

    private final Long projectId;
    private final Object currentState;
    private final Object targetState;

    public InvalidProjectStateException(Long projectId, ProjectPhase currentPhase, ProjectPhase targetPhase) {
        super(String.format("Invalid phase transition for project %d: cannot move from %s to %s",
                projectId, currentPhase, targetPhase));
        this.projectId = projectId;
        this.currentState = currentPhase;
        this.targetState = targetPhase;
    }

    public InvalidProjectStateException(Long projectId, ProjectStatus currentStatus, ProjectStatus targetStatus) {
        super(String.format("Invalid status transition for project %d: cannot change from %s to %s",
                projectId, currentStatus, targetStatus));
        this.projectId = projectId;
        this.currentState = currentStatus;
        this.targetState = targetStatus;
    }

    public InvalidProjectStateException(String message) {
        super(message);
        this.projectId = null;
        this.currentState = null;
        this.targetState = null;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Object getCurrentState() {
        return currentState;
    }

    public Object getTargetState() {
        return targetState;
    }
}
