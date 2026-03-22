package com.wd.api.validation;

import com.wd.api.dto.PublicReferralRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validator implementation for ValidReferralEmails annotation.
 * Ensures that the referrer and the person being referred are different people.
 */
public class ReferralEmailValidator implements ConstraintValidator<ValidReferralEmails, PublicReferralRequest> {

    private static final Logger logger = LoggerFactory.getLogger(ReferralEmailValidator.class);

    @Override
    public void initialize(ValidReferralEmails constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(PublicReferralRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true; // Let @NotNull handle null checks
        }

        String yourEmail = request.getYourEmail();
        String referralEmail = request.getReferralEmail();
        String yourPhone = request.getYourPhone();
        String referralPhone = request.getReferralPhone();

        // Check if emails match (case-insensitive)
        if (yourEmail != null && referralEmail != null) {
            if (yourEmail.trim().equalsIgnoreCase(referralEmail.trim())) {
                logger.warn("Self-referral attempt detected: same email {} used for both referrer and referral", yourEmail);
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    "You cannot refer yourself. The referrer email and referral email must be different.")
                    .addPropertyNode("referralEmail")
                    .addConstraintViolation();
                return false;
            }
        }

        // Check if phone numbers match
        if (yourPhone != null && referralPhone != null) {
            String cleanYourPhone = yourPhone.replaceAll("\\s+", "");
            String cleanReferralPhone = referralPhone.replaceAll("\\s+", "");
            if (!cleanYourPhone.isEmpty() && cleanYourPhone.equals(cleanReferralPhone)) {
                logger.warn("Self-referral attempt detected: same phone {} used for both referrer and referral", cleanYourPhone);
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    "You cannot use the same phone number for both you and the person you're referring.")
                    .addPropertyNode("referralPhone")
                    .addConstraintViolation();
                return false;
            }
        }

        return true;
    }
}
