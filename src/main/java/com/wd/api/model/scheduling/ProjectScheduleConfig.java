package com.wd.api.model.scheduling;

import com.wd.api.model.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@SQLDelete(sql = "UPDATE project_schedule_config SET deleted_at = NOW() WHERE id = ? AND version = ?")
@Where(clause = "deleted_at IS NULL")
@Entity
@Table(name = "project_schedule_config")
public class ProjectScheduleConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false, unique = true)
    private Long projectId;

    @Column(name = "sunday_working", nullable = false)
    private Boolean sundayWorking = Boolean.FALSE;

    @Column(name = "monsoon_start_month_day", nullable = false)
    private Short monsoonStartMonthDay = (short) 601;

    @Column(name = "monsoon_end_month_day", nullable = false)
    private Short monsoonEndMonthDay = (short) 930;

    @Column(name = "district_code", length = 16)
    private String districtCode;

    public ProjectScheduleConfig() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Boolean getSundayWorking() { return sundayWorking; }
    public void setSundayWorking(Boolean sundayWorking) { this.sundayWorking = sundayWorking; }
    public Short getMonsoonStartMonthDay() { return monsoonStartMonthDay; }
    public void setMonsoonStartMonthDay(Short monsoonStartMonthDay) { this.monsoonStartMonthDay = monsoonStartMonthDay; }
    public Short getMonsoonEndMonthDay() { return monsoonEndMonthDay; }
    public void setMonsoonEndMonthDay(Short monsoonEndMonthDay) { this.monsoonEndMonthDay = monsoonEndMonthDay; }
    public String getDistrictCode() { return districtCode; }
    public void setDistrictCode(String districtCode) { this.districtCode = districtCode; }
}
