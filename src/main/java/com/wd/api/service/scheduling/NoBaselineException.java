package com.wd.api.service.scheduling;

public class NoBaselineException extends RuntimeException {
    public NoBaselineException(Long projectId) {
        super("Project " + projectId + " has no approved baseline");
    }
}
