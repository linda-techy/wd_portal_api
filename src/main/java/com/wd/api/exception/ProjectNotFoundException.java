package com.wd.api.exception;

/**
 * Exception thrown when a project is not found by ID or code.
 * Results in HTTP 404 Not Found response.
 */
public class ProjectNotFoundException extends RuntimeException {

    private final Long projectId;
    private final String projectCode;

    public ProjectNotFoundException(Long id) {
        super(String.format("Project not found with ID: %d", id));
        this.projectId = id;
        this.projectCode = null;
    }

    public ProjectNotFoundException(String code) {
        super(String.format("Project not found with code: %s", code));
        this.projectId = null;
        this.projectCode = code;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getProjectCode() {
        return projectCode;
    }
}
