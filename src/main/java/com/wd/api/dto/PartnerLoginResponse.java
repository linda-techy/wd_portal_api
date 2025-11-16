package com.wd.api.dto;

public class PartnerLoginResponse {
    
    private String token;
    private String partnerId;
    private String name;
    private String phone;
    private String email;
    private String partnershipType;
    private String firmName;
    private String status;
    
    // Constructor
    public PartnerLoginResponse() {}
    
    public PartnerLoginResponse(String token, String partnerId, String name, String phone, 
                                String email, String partnershipType, String firmName, String status) {
        this.token = token;
        this.partnerId = partnerId;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.partnershipType = partnershipType;
        this.firmName = firmName;
        this.status = status;
    }
    
    // Getters and Setters
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getPartnerId() {
        return partnerId;
    }
    
    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
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
    
    public String getPartnershipType() {
        return partnershipType;
    }
    
    public void setPartnershipType(String partnershipType) {
        this.partnershipType = partnershipType;
    }
    
    public String getFirmName() {
        return firmName;
    }
    
    public void setFirmName(String firmName) {
        this.firmName = firmName;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}

