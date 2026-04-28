package com.wd.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Company Information Configuration
 * Loads company details from application.properties for use in PDF templates and documents
 */
@Configuration
@Component
@ConfigurationProperties(prefix = "company")
public class CompanyInfoConfig {

    private String name = "Walldot Builders LLP";
    private String address = "123 Construction Avenue, Building City, State - 123456";
    private String phone = "+91 9074954874";
    private String email = "info@walldotbuilders.com";
    private String website = "www.walldotbuilders.com";
    private String gst = "GST123456789";
    private String pan;
    private String llpin;

    /**
     * Default signatory + city for the customer-facing SQFT_RATE quotation
     * footer ("With Regards, [name], Managing Director, [company], [city]").
     * Mirror's Walldot's actual paper format. Override per-deployment via
     * {@code company.signatory-name} / {@code company.city} in
     * {@code application.properties}.
     */
    private String signatoryName = "Praveen P F";
    private String city = "Thrissur";

    /**
     * Path to the company logo image rendered on the quotation PDF header.
     *
     * <p>Accepts either a {@code classpath:} URI (e.g.
     * {@code classpath:branding/walldot-logo.png}) — file is bundled with the
     * JAR — or an absolute filesystem path. PNG, JPEG, and SVG are supported.
     *
     * <p>Default points at {@code branding/walldot-logo.png} on the classpath;
     * drop a file at {@code wd_portal_api/src/main/resources/branding/walldot-logo.png}
     * and rebuild to have it appear. If the file is missing the header
     * gracefully renders without a logo (text only).
     */
    private String logoPath = "classpath:branding/walldot-logo.png";

    // Bank details — used by quotation PDF for advance-payment instructions.
    private String bankAccountName;
    private String bankAccountNumber;
    private String bankIfsc;
    private String bankName;
    private String bankBranch;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getGst() {
        return gst;
    }

    public void setGst(String gst) {
        this.gst = gst;
    }

    public String getPan() {
        return pan;
    }

    public void setPan(String pan) {
        this.pan = pan;
    }

    public String getLlpin() {
        return llpin;
    }

    public void setLlpin(String llpin) {
        this.llpin = llpin;
    }

    public String getBankAccountName() {
        return bankAccountName;
    }

    public void setBankAccountName(String bankAccountName) {
        this.bankAccountName = bankAccountName;
    }

    public String getBankAccountNumber() {
        return bankAccountNumber;
    }

    public void setBankAccountNumber(String bankAccountNumber) {
        this.bankAccountNumber = bankAccountNumber;
    }

    public String getBankIfsc() {
        return bankIfsc;
    }

    public void setBankIfsc(String bankIfsc) {
        this.bankIfsc = bankIfsc;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBankBranch() {
        return bankBranch;
    }

    public void setBankBranch(String bankBranch) {
        this.bankBranch = bankBranch;
    }

    public String getLogoPath() {
        return logoPath;
    }

    public void setLogoPath(String logoPath) {
        this.logoPath = logoPath;
    }

    public String getSignatoryName() {
        return signatoryName;
    }

    public void setSignatoryName(String signatoryName) {
        this.signatoryName = signatoryName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
