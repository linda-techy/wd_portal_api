package com.wd.api.exception;

/**
 * Exception thrown when attempting to create a project with a duplicate code.
 * Project codes must be unique across the system.
 */
public class DuplicateProjectCodeException extends RuntimeException {

    private final String projectCode;
    private final Long existingProjectId;

    public DuplicateProjectCodeException(String code) {
        super(String.format("Project code '%s' is already in use", code));
        this.projectCode = code;
        this.existingProjectId = null;
    }

    public DuplicateProjectCodeException(String code, Long existingProjectId) {
        super(String.format("Project code '%s' is already in use by project ID %d", code, existingProjectId));
        this.projectCode = code;
        this.existingProjectId = existingProjectId;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public Long getExistingProjectId() {
        return existingProjectId;
    }
}
