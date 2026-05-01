package com.wd.api.estimation.dto;

import com.wd.api.estimation.domain.enums.ProjectType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CalculatePreviewRequestValidationTest {

    private final Validator v;

    CalculatePreviewRequestValidationTest() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            v = factory.getValidator();
        }
    }

    @Test
    void validRequest_passes() {
        CalculatePreviewRequest req = minimalValidRequest();
        Set<ConstraintViolation<CalculatePreviewRequest>> violations = v.validate(req);
        assertThat(violations).isEmpty();
    }

    @Test
    void missingProjectType_fails() {
        CalculatePreviewRequest req = new CalculatePreviewRequest(
                null, UUID.randomUUID(), null, null,
                validDimensions(), List.of(), List.of(), List.of(), List.of(),
                BigDecimal.ZERO, new BigDecimal("0.18"));
        assertThat(v.validate(req)).anyMatch(c -> c.getPropertyPath().toString().equals("projectType"));
    }

    @Test
    void missingPackageId_fails() {
        CalculatePreviewRequest req = new CalculatePreviewRequest(
                ProjectType.NEW_BUILD, null, null, null,
                validDimensions(), List.of(), List.of(), List.of(), List.of(),
                BigDecimal.ZERO, new BigDecimal("0.18"));
        assertThat(v.validate(req)).anyMatch(c -> c.getPropertyPath().toString().equals("packageId"));
    }

    @Test
    void missingDimensions_fails() {
        CalculatePreviewRequest req = new CalculatePreviewRequest(
                ProjectType.NEW_BUILD, UUID.randomUUID(), null, null,
                null, List.of(), List.of(), List.of(), List.of(),
                BigDecimal.ZERO, new BigDecimal("0.18"));
        assertThat(v.validate(req)).anyMatch(c -> c.getPropertyPath().toString().equals("dimensions"));
    }

    @Test
    void discountAbove50Percent_fails() {
        CalculatePreviewRequest req = new CalculatePreviewRequest(
                ProjectType.NEW_BUILD, UUID.randomUUID(), null, null,
                validDimensions(), List.of(), List.of(), List.of(), List.of(),
                new BigDecimal("0.51"), new BigDecimal("0.18"));
        assertThat(v.validate(req)).anyMatch(c -> c.getPropertyPath().toString().equals("discountPercent"));
    }

    @Test
    void negativeGstRate_fails() {
        CalculatePreviewRequest req = new CalculatePreviewRequest(
                ProjectType.NEW_BUILD, UUID.randomUUID(), null, null,
                validDimensions(), List.of(), List.of(), List.of(), List.of(),
                BigDecimal.ZERO, new BigDecimal("-0.01"));
        assertThat(v.validate(req)).anyMatch(c -> c.getPropertyPath().toString().equals("gstRate"));
    }

    private CalculatePreviewRequest minimalValidRequest() {
        return new CalculatePreviewRequest(
                ProjectType.NEW_BUILD, UUID.randomUUID(), null, null,
                validDimensions(),
                List.of(), List.of(), List.of(), List.of(),
                BigDecimal.ZERO, new BigDecimal("0.18"));
    }

    private DimensionsDto validDimensions() {
        return new DimensionsDto(
                List.of(new FloorDto("GF", new BigDecimal("35"), new BigDecimal("30"))),
                BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
