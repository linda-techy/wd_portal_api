package com.wd.api.service.scheduling;

public class BaselineAlreadyExistsException extends RuntimeException {
    public BaselineAlreadyExistsException(Long projectId) {
        super("Project " + projectId + " already has a baseline; re-baselining is not supported in S2");
    }
}
