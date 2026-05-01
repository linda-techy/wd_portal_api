package com.wd.api.estimation.service.calc.exception;

import com.wd.api.estimation.domain.enums.ProjectType;

public class UnsupportedProjectTypeException extends RuntimeException {
    private final ProjectType projectType;

    public UnsupportedProjectTypeException(ProjectType projectType) {
        super("Project type " + projectType + " is not supported by EstimationCalculator yet");
        this.projectType = projectType;
    }

    public ProjectType getProjectType() {
        return projectType;
    }
}
