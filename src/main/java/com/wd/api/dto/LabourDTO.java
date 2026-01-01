package com.wd.api.dto;

import java.math.BigDecimal;

public class LabourDTO {
    private Long id;
    private String name;
    private String phone;
    private String tradeType;
    private String idProofType;
    private String idProofNumber;
    private BigDecimal dailyWage;
    private String emergencyContact;
    private boolean active;

    public LabourDTO() {
    }

    public LabourDTO(Long id, String name, String phone, String tradeType, String idProofType, String idProofNumber,
            BigDecimal dailyWage, String emergencyContact, boolean active) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.tradeType = tradeType;
        this.idProofType = idProofType;
        this.idProofNumber = idProofNumber;
        this.dailyWage = dailyWage;
        this.emergencyContact = emergencyContact;
        this.active = active;
    }

    public static LabourDTOBuilder builder() {
        return new LabourDTOBuilder();
    }

    public static class LabourDTOBuilder {
        private Long id;
        private String name;
        private String phone;
        private String tradeType;
        private String idProofType;
        private String idProofNumber;
        private BigDecimal dailyWage;
        private String emergencyContact;
        private boolean active;

        public LabourDTOBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public LabourDTOBuilder name(String name) {
            this.name = name;
            return this;
        }

        public LabourDTOBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public LabourDTOBuilder tradeType(String tradeType) {
            this.tradeType = tradeType;
            return this;
        }

        public LabourDTOBuilder idProofType(String idProofType) {
            this.idProofType = idProofType;
            return this;
        }

        public LabourDTOBuilder idProofNumber(String idProofNumber) {
            this.idProofNumber = idProofNumber;
            return this;
        }

        public LabourDTOBuilder dailyWage(BigDecimal dailyWage) {
            this.dailyWage = dailyWage;
            return this;
        }

        public LabourDTOBuilder emergencyContact(String emergencyContact) {
            this.emergencyContact = emergencyContact;
            return this;
        }

        public LabourDTOBuilder active(boolean active) {
            this.active = active;
            return this;
        }

        public LabourDTO build() {
            return new LabourDTO(id, name, phone, tradeType, idProofType, idProofNumber, dailyWage, emergencyContact,
                    active);
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

    public void setId(Long id) {
        this.id = id;
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
