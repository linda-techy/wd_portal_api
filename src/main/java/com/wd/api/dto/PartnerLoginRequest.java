package com.wd.api.dto;

import jakarta.validation.constraints.NotBlank;

public class PartnerLoginRequest {
    
    @NotBlank(message = "Phone number is required")
    private String phone;
    
    @NotBlank(message = "Password is required")
    private String password;
    
    // Getters and Setters
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
}

