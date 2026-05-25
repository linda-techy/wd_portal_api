package com.wd.api.dto;

import com.wd.api.dto.VariationOrderDtos.CreateVariationOrderRequest;
import com.wd.api.model.enums.VOCategory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the CO/VO split invariant (audit P1-3): a Variation Order must carry a
 * non-null voCategory, otherwise it would be persisted with voCategory IS NULL and
 * wrongly surface in the Change Order list instead of the Variation Order list.
 */
class CreateVariationOrderRequestValidationTest {

    private final Validator v;

    CreateVariationOrderRequestValidationTest() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            v = factory.getValidator();
        }
    }

    @Test
    void nullVoCategory_fails() {
        CreateVariationOrderRequest req = request(null);
        Set<ConstraintViolation<CreateVariationOrderRequest>> violations = v.validate(req);
        assertThat(violations).anyMatch(c -> c.getPropertyPath().toString().equals("voCategory"));
    }

    @Test
    void validVoCategory_hasNoVoCategoryViolation() {
        CreateVariationOrderRequest req = request(VOCategory.MATERIAL_HEAVY);
        assertThat(v.validate(req)).noneMatch(c -> c.getPropertyPath().toString().equals("voCategory"));
    }

    /** Otherwise-valid request; voCategory is the field under test. */
    private CreateVariationOrderRequest request(VOCategory voCategory) {
        return new CreateVariationOrderRequest(
                1L,                          // boqDocumentId
                "Extra waterproofing",       // title
                null,                        // description
                null,                        // justification
                null,                        // scopeNotes
                "SCOPE_ADDITION",            // coType
                voCategory,
                null,                        // revisesCoId
                null,                        // mappedStageIds
                new BigDecimal("1500.00"),   // netAmountExGst
                null,                        // gstRate
                null,                        // reviewDeadline
                List.of()                    // lineItems
        );
    }
}
