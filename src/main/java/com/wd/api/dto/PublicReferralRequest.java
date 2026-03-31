package com.wd.api.dto;

import com.wd.api.validation.ValidReferralEmails;
import jakarta.validation.constraints.*;

/**
 * Public referral submission — submitted by a customer referring a friend
 * via the Next.js website or the customer mobile app.
 * No authentication required.
 */
@ValidReferralEmails
public class PublicReferralRequest {

    // ── Referred person (the actual lead) ────────────────────────────────────

    @NotBlank(message = "Referral name is required")
    @Size(min = 2, max = 100)
    private String referralName;

    @Email(message = "Please provide a valid email for the referral")
    private String referralEmail;

    @NotBlank(message = "Referral phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
    private String referralPhone;

    // ── Referrer information (person who is making the referral) ─────────────

    @NotBlank(message = "Your name is required")
    @Size(min = 2, max = 100)
    private String yourName;

    @Email(message = "Please provide a valid email address")
    private String yourEmail;

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
    private String yourPhone;

    // ── Project details ───────────────────────────────────────────────────────

    private String projectType;

    private String estimatedBudget;

    private String location;

    private String state;

    private String district;

    /** Additional notes from the referrer. */
    private String message;

    // Getters & setters
    public String getReferralName() { return referralName; }
    public void setReferralName(String referralName) { this.referralName = referralName; }

    public String getReferralEmail() { return referralEmail; }
    public void setReferralEmail(String referralEmail) { this.referralEmail = referralEmail; }

    public String getReferralPhone() { return referralPhone; }
    public void setReferralPhone(String referralPhone) { this.referralPhone = referralPhone; }

    public String getYourName() { return yourName; }
    public void setYourName(String yourName) { this.yourName = yourName; }

    public String getYourEmail() { return yourEmail; }
    public void setYourEmail(String yourEmail) { this.yourEmail = yourEmail; }

    public String getYourPhone() { return yourPhone; }
    public void setYourPhone(String yourPhone) { this.yourPhone = yourPhone; }

    public String getProjectType() { return projectType; }
    public void setProjectType(String projectType) { this.projectType = projectType; }

    public String getEstimatedBudget() { return estimatedBudget; }
    public void setEstimatedBudget(String estimatedBudget) { this.estimatedBudget = estimatedBudget; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

}
