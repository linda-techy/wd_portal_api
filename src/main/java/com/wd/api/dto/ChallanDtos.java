package com.wd.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ChallanDtos {

    public static class ChallanResponse {
        private Long id;
        private Long transactionId;
        private String challanNumber;
        private String fy;
        private LocalDateTime transactionDate;
        private BigDecimal amount;
        private String clientName;
        private String projectName;
        private String status;
        private LocalDateTime generatedAt;

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getTransactionId() {
            return transactionId;
        }

        public void setTransactionId(Long transactionId) {
            this.transactionId = transactionId;
        }

        public String getChallanNumber() {
            return challanNumber;
        }

        public void setChallanNumber(String challanNumber) {
            this.challanNumber = challanNumber;
        }

        public String getFy() {
            return fy;
        }

        public void setFy(String fy) {
            this.fy = fy;
        }

        public LocalDateTime getTransactionDate() {
            return transactionDate;
        }

        public void setTransactionDate(LocalDateTime transactionDate) {
            this.transactionDate = transactionDate;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getClientName() {
            return clientName;
        }

        public void setClientName(String clientName) {
            this.clientName = clientName;
        }

        public String getProjectName() {
            return projectName;
        }

        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public LocalDateTime getGeneratedAt() {
            return generatedAt;
        }

        public void setGeneratedAt(LocalDateTime generatedAt) {
            this.generatedAt = generatedAt;
        }
    }

    public static class ChallanFilterRequest {
        private String fy;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private List<Long> ids;

        public String getFy() {
            return fy;
        }

        public void setFy(String fy) {
            this.fy = fy;
        }

        public LocalDateTime getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDateTime startDate) {
            this.startDate = startDate;
        }

        public LocalDateTime getEndDate() {
            return endDate;
        }

        public void setEndDate(LocalDateTime endDate) {
            this.endDate = endDate;
        }

        public List<Long> getIds() {
            return ids;
        }

        public void setIds(List<Long> ids) {
            this.ids = ids;
        }
    }
}
