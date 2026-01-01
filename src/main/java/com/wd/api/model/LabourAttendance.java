package com.wd.api.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "labour_attendance")
public class LabourAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "labour_id", nullable = false)
    private Labour labour;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(nullable = false)
    private String status; // 'PRESENT', 'ABSENT', 'HALF_DAY'

    @Column(name = "hours_worked")
    private Double hoursWorked;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    protected void onCreate() {
        recordedAt = LocalDateTime.now();
    }

    public LabourAttendance() {
    }

    public LabourAttendance(Long id, CustomerProject project, Labour labour, LocalDate attendanceDate, String status,
            Double hoursWorked, LocalDateTime recordedAt) {
        this.id = id;
        this.project = project;
        this.labour = labour;
        this.attendanceDate = attendanceDate;
        this.status = status;
        this.hoursWorked = hoursWorked;
        this.recordedAt = recordedAt;
    }

    public static LabourAttendanceBuilder builder() {
        return new LabourAttendanceBuilder();
    }

    public static class LabourAttendanceBuilder {
        private Long id;
        private CustomerProject project;
        private Labour labour;
        private LocalDate attendanceDate;
        private String status;
        private Double hoursWorked;
        private LocalDateTime recordedAt;

        public LabourAttendanceBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public LabourAttendanceBuilder project(CustomerProject project) {
            this.project = project;
            return this;
        }

        public LabourAttendanceBuilder labour(Labour labour) {
            this.labour = labour;
            return this;
        }

        public LabourAttendanceBuilder attendanceDate(LocalDate attendanceDate) {
            this.attendanceDate = attendanceDate;
            return this;
        }

        public LabourAttendanceBuilder status(String status) {
            this.status = status;
            return this;
        }

        public LabourAttendanceBuilder hoursWorked(Double hoursWorked) {
            this.hoursWorked = hoursWorked;
            return this;
        }

        public LabourAttendanceBuilder recordedAt(LocalDateTime recordedAt) {
            this.recordedAt = recordedAt;
            return this;
        }

        public LabourAttendance build() {
            return new LabourAttendance(id, project, labour, attendanceDate, status, hoursWorked, recordedAt);
        }
    }

    public Long getId() {
        return id;
    }

    public CustomerProject getProject() {
        return project;
    }

    public Labour getLabour() {
        return labour;
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

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setProject(CustomerProject project) {
        this.project = project;
    }

    public void setLabour(Labour labour) {
        this.labour = labour;
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
