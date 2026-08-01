package com.wd.api.estimation.controller;

import com.wd.api.estimation.service.calc.exception.UnsupportedProjectTypeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = EstimationPreviewController.class)
public class EstimationPreviewExceptionHandler {

    private static final String KEY_ERROR = "error";
    private static final String KEY_MESSAGE = "message";

    @ExceptionHandler(UnsupportedProjectTypeException.class)
    public ResponseEntity<Map<String, String>> unsupportedType(UnsupportedProjectTypeException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of(
                        KEY_ERROR, "unsupported-project-type",
                        "projectType", ex.getProjectType().name(),
                        KEY_MESSAGE, ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> illegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(KEY_ERROR, "invalid-request", KEY_MESSAGE, ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> illegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of(KEY_ERROR, "preview-not-available", KEY_MESSAGE, ex.getMessage()));
    }
}
