package com.wd.api.model.scheduling;

import com.wd.api.model.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;

@SQLDelete(sql = "UPDATE project_holiday_override SET deleted_at = NOW() WHERE id = ? AND version = ?")
@Where(clause = "deleted_at IS NULL")
@Entity
@Table(name = "project_holiday_override")
public class ProjectHolidayOverride extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "holiday_id")
    private Long holidayId;

    @Column(name = "override_date", nullable = false)
    private LocalDate overrideDate;

    @Column(name = "override_name", length = 128)
    private String overrideName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private HolidayOverrideAction action;

    public ProjectHolidayOverride() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getHolidayId() { return holidayId; }
    public void setHolidayId(Long holidayId) { this.holidayId = holidayId; }
    public LocalDate getOverrideDate() { return overrideDate; }
    public void setOverrideDate(LocalDate overrideDate) { this.overrideDate = overrideDate; }
    public String getOverrideName() { return overrideName; }
    public void setOverrideName(String overrideName) { this.overrideName = overrideName; }
    public HolidayOverrideAction getAction() { return action; }
    public void setAction(HolidayOverrideAction action) { this.action = action; }
}
