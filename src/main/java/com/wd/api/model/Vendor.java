package com.wd.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vendors")
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "contact_person")
    private String contactPerson;

    @Column(nullable = false, unique = true)
    private String phone;

    @Column(unique = true)
    private String email;

    @Column(unique = true, length = 15)
    private String gstin;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "vendor_type", nullable = false)
    private String vendorType; // 'MATERIAL', 'LABOUR', 'SERVICES'

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "ifsc_code")
    private String ifscCode;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Vendor() {
    }

    public Vendor(Long id, String name, String contactPerson, String phone, String email, String gstin, String address,
            String vendorType, String bankName, String accountNumber, String ifscCode, boolean active,
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

    public static VendorBuilder builder() {
        return new VendorBuilder();
    }

    public static class VendorBuilder {
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
        private boolean active = true;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public VendorBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public VendorBuilder name(String name) {
            this.name = name;
            return this;
        }

        public VendorBuilder contactPerson(String contactPerson) {
            this.contactPerson = contactPerson;
            return this;
        }

        public VendorBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public VendorBuilder email(String email) {
            this.email = email;
            return this;
        }

        public VendorBuilder gstin(String gstin) {
            this.gstin = gstin;
            return this;
        }

        public VendorBuilder address(String address) {
            this.address = address;
            return this;
        }

        public VendorBuilder vendorType(String vendorType) {
            this.vendorType = vendorType;
            return this;
        }

        public VendorBuilder bankName(String bankName) {
            this.bankName = bankName;
            return this;
        }

        public VendorBuilder accountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public VendorBuilder ifscCode(String ifscCode) {
            this.ifscCode = ifscCode;
            return this;
        }

        public VendorBuilder active(boolean active) {
            this.active = active;
            return this;
        }

        public VendorBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public VendorBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Vendor build() {
            return new Vendor(id, name, contactPerson, phone, email, gstin, address, vendorType, bankName,
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
}
