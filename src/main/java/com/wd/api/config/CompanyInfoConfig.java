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
    private String phone = "+91 1234567890";
    private String email = "info@walldotbuilders.com";
    private String website = "www.walldotbuilders.com";
    private String gst = "GST123456789";
    private String pan;

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
}
