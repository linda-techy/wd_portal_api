package com.wd.api.estimation.controller;

import com.wd.api.estimation.service.calc.exception.UnsupportedProjectTypeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = EstimationPreviewController.class)
public class EstimationPreviewExceptionHandler {

    @ExceptionHandler(UnsupportedProjectTypeException.class)
    public ResponseEntity<Map<String, String>> unsupportedType(UnsupportedProjectTypeException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of(
                        "error", "unsupported-project-type",
                        "projectType", ex.getProjectType().name(),
                        "message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> illegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "invalid-request", "message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> illegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", "preview-not-available", "message", ex.getMessage()));
    }
}
