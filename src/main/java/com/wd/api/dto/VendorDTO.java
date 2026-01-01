package com.wd.api.dto;

import java.time.LocalDateTime;

public class VendorDTO {
    private Long id;
    private String name;
    private String contactPerson;
    private String phone;
    private String email;
    private String gstin;
    private String address;
    private String vendorType;
    private String bankName;
    private String accountNumber;
    private String ifscCode;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public VendorDTO() {
    }

    public VendorDTO(Long id, String name, String contactPerson, String phone, String email, String gstin,
            String address, String vendorType, String bankName, String accountNumber, String ifscCode, boolean active,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.contactPerson = contactPerson;
        this.phone = phone;
        this.email = email;
        this.gstin = gstin;
        this.address = address;
        this.vendorType = vendorType;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.ifscCode = ifscCode;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static VendorDTOBuilder builder() {
        return new VendorDTOBuilder();
    }

    public static class VendorDTOBuilder {
        private Long id;
        private String name;
        private String contactPerson;
        private String phone;
        private String email;
        private String gstin;
        private String address;
        private String vendorType;
        private String bankName;
        private String accountNumber;
        private String ifscCode;
        private boolean active;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public VendorDTOBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public VendorDTOBuilder name(String name) {
            this.name = name;
            return this;
        }

        public VendorDTOBuilder contactPerson(String contactPerson) {
            this.contactPerson = contactPerson;
            return this;
        }

        public VendorDTOBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public VendorDTOBuilder email(String email) {
            this.email = email;
            return this;
        }

        public VendorDTOBuilder gstin(String gstin) {
            this.gstin = gstin;
            return this;
        }

        public VendorDTOBuilder address(String address) {
            this.address = address;
            return this;
        }

        public VendorDTOBuilder vendorType(String vendorType) {
            this.vendorType = vendorType;
            return this;
        }

        public VendorDTOBuilder bankName(String bankName) {
            this.bankName = bankName;
            return this;
        }

        public VendorDTOBuilder accountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public VendorDTOBuilder ifscCode(String ifscCode) {
            this.ifscCode = ifscCode;
            return this;
        }

        public VendorDTOBuilder active(boolean active) {
            this.active = active;
            return this;
        }

        public VendorDTOBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public VendorDTOBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public VendorDTO build() {
            return new VendorDTO(id, name, contactPerson, phone, email, gstin, address, vendorType, bankName,
                    accountNumber, ifscCode, active, createdAt, updatedAt);
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getGstin() {
        return gstin;
    }

    public String getAddress() {
        return address;
    }

    public String getVendorType() {
        return vendorType;
    }

    public String getBankName() {
        return bankName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getIfscCode() {
        return ifscCode;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setGstin(String gstin) {
        this.gstin = gstin;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setVendorType(String vendorType) {
        this.vendorType = vendorType;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public void setIfscCode(String ifscCode) {
        this.ifscCode = ifscCode;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
