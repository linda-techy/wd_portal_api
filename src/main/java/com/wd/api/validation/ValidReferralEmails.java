package com.wd.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation to ensure that a referrer is not referring themselves.
 * Applied at the class level to validate relationships between fields.
 */
@Constraint(validatedBy = ReferralEmailValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidReferralEmails {
    String message() default "You cannot refer yourself. Please enter a different email address for the person you're referring.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
