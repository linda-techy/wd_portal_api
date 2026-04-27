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
}
