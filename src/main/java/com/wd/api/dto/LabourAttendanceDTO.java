package com.wd.api.dto;

import java.time.LocalDate;

public class LabourAttendanceDTO {
    private Long id;
    private Long projectId;
    private Long labourId;
    private String labourName;
    private LocalDate attendanceDate;
    private String status;
    private Double hoursWorked;

    public LabourAttendanceDTO() {
    }

    public LabourAttendanceDTO(Long id, Long projectId, Long labourId, String labourName, LocalDate attendanceDate,
            String status, Double hoursWorked) {
        this.id = id;
        this.projectId = projectId;
        this.labourId = labourId;
        this.labourName = labourName;
        this.attendanceDate = attendanceDate;
        this.status = status;
        this.hoursWorked = hoursWorked;
    }

    public static LabourAttendanceDTOBuilder builder() {
        return new LabourAttendanceDTOBuilder();
    }

    public static class LabourAttendanceDTOBuilder {
        private Long id;
        private Long projectId;
        private Long labourId;
        private String labourName;
        private LocalDate attendanceDate;
        private String status;
        private Double hoursWorked;

        public LabourAttendanceDTOBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public LabourAttendanceDTOBuilder projectId(Long projectId) {
            this.projectId = projectId;
            return this;
        }

        public LabourAttendanceDTOBuilder labourId(Long labourId) {
            this.labourId = labourId;
            return this;
        }

        public LabourAttendanceDTOBuilder labourName(String labourName) {
            this.labourName = labourName;
            return this;
        }

        public LabourAttendanceDTOBuilder attendanceDate(LocalDate attendanceDate) {
            this.attendanceDate = attendanceDate;
            return this;
        }

        public LabourAttendanceDTOBuilder status(String status) {
            this.status = status;
            return this;
        }

        public LabourAttendanceDTOBuilder hoursWorked(Double hoursWorked) {
            this.hoursWorked = hoursWorked;
            return this;
        }

        public LabourAttendanceDTO build() {
            return new LabourAttendanceDTO(id, projectId, labourId, labourName, attendanceDate, status, hoursWorked);
        }
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getLabourId() {
        return labourId;
    }

    public String getLabourName() {
        return labourName;
    }

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public String getStatus() {
        return status;
    }

    public Double getHoursWorked() {
        return hoursWorked;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public void setLabourId(Long labourId) {
        this.labourId = labourId;
    }

    public void setLabourName(String labourName) {
        this.labourName = labourName;
    }

    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setHoursWorked(Double hoursWorked) {
        this.hoursWorked = hoursWorked;
    }
}
