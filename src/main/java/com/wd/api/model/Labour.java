package com.wd.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "labour")
public class Labour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String phone;

    @Column(name = "trade_type", nullable = false)
    private String tradeType; // 'CARPENTER', 'PLUMBER', 'ELECTRICIAN', 'MASON', 'HELPER', etc.

    @Column(name = "id_proof_type")
    private String idProofType; // 'AADHAAR', 'PAN', 'VOTER_ID'

    @Column(name = "id_proof_number")
    private String idProofNumber;

    @Column(name = "daily_wage", precision = 15, scale = 2, nullable = false)
    private BigDecimal dailyWage;

    @Column(name = "emergency_contact")
    private String emergencyContact;

    @Column(name = "is_active")
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Labour() {
    }

    public Labour(Long id, String name, String phone, String tradeType, String idProofType, String idProofNumber,
            BigDecimal dailyWage, String emergencyContact, boolean active, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.tradeType = tradeType;
        this.idProofType = idProofType;
        this.idProofNumber = idProofNumber;
        this.dailyWage = dailyWage;
        this.emergencyContact = emergencyContact;
        this.active = active;
        this.createdAt = createdAt;
    }

    public static LabourBuilder builder() {
        return new LabourBuilder();
    }

    public static class LabourBuilder {
        private Long id;
        private String name;
        private String phone;
        private String tradeType;
        private String idProofType;
        private String idProofNumber;
        private BigDecimal dailyWage;
        private String emergencyContact;
        private boolean active = true;
        private LocalDateTime createdAt;

        public LabourBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public LabourBuilder name(String name) {
            this.name = name;
            return this;
        }

        public LabourBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public LabourBuilder tradeType(String tradeType) {
            this.tradeType = tradeType;
            return this;
        }

        public LabourBuilder idProofType(String idProofType) {
            this.idProofType = idProofType;
            return this;
        }

        public LabourBuilder idProofNumber(String idProofNumber) {
            this.idProofNumber = idProofNumber;
            return this;
        }

        public LabourBuilder dailyWage(BigDecimal dailyWage) {
            this.dailyWage = dailyWage;
            return this;
        }

        public LabourBuilder emergencyContact(String emergencyContact) {
            this.emergencyContact = emergencyContact;
            return this;
        }

        public LabourBuilder active(boolean active) {
            this.active = active;
            return this;
        }

        public LabourBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Labour build() {
            return new Labour(id, name, phone, tradeType, idProofType, idProofNumber, dailyWage, emergencyContact,
                    active, createdAt);
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getTradeType() {
        return tradeType;
    }

    public String getIdProofType() {
        return idProofType;
    }

    public String getIdProofNumber() {
        return idProofNumber;
    }

    public BigDecimal getDailyWage() {
        return dailyWage;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setTradeType(String tradeType) {
        this.tradeType = tradeType;
    }

    public void setIdProofType(String idProofType) {
        this.idProofType = idProofType;
    }

    public void setIdProofNumber(String idProofNumber) {
        this.idProofNumber = idProofNumber;
    }

    public void setDailyWage(BigDecimal dailyWage) {
        this.dailyWage = dailyWage;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
