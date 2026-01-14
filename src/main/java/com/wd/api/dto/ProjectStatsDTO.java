package com.wd.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * DTO for Project Statistics
 * Used in dashboard and project overview screens
 */
public class ProjectStatsDTO {
    
    @JsonProperty("total_projects")
    private Long totalProjects;
    
    @JsonProperty("active_projects")
    private Long activeProjects;
    
    @JsonProperty("completed_projects")
    private Long completedProjects;
    
    @JsonProperty("projects_by_phase")
    private Map<String, Long> projectsByPhase;
    
    @JsonProperty("projects_by_type")
    private Map<String, Long> projectsByType;
    
    @JsonProperty("completion_rate")
    private Double completionRate;
    
    @JsonProperty("on_track_count")
    private Long onTrackCount;
    
    @JsonProperty("delayed_count")
    private Long delayedCount;
    
    // Constructors
    public ProjectStatsDTO() {
    }
    
    // Getters and Setters
    public Long getTotalProjects() {
        return totalProjects;
    }
    
    public void setTotalProjects(Long totalProjects) {
        this.totalProjects = totalProjects;
    }
    
    public Long getActiveProjects() {
        return activeProjects;
    }
    
    public void setActiveProjects(Long activeProjects) {
        this.activeProjects = activeProjects;
    }
    
    public Long getCompletedProjects() {
        return completedProjects;
    }
    
    public void setCompletedProjects(Long completedProjects) {
        this.completedProjects = completedProjects;
    }
    
    public Map<String, Long> getProjectsByPhase() {
        return projectsByPhase;
    }
    
    public void setProjectsByPhase(Map<String, Long> projectsByPhase) {
        this.projectsByPhase = projectsByPhase;
    }
    
    public Map<String, Long> getProjectsByType() {
        return projectsByType;
    }
    
    public void setProjectsByType(Map<String, Long> projectsByType) {
        this.projectsByType = projectsByType;
    }
    
    public Double getCompletionRate() {
        return completionRate;
    }
    
    public void setCompletionRate(Double completionRate) {
        this.completionRate = completionRate;
    }
    
    public Long getOnTrackCount() {
        return onTrackCount;
    }
    
    public void setOnTrackCount(Long onTrackCount) {
        this.onTrackCount = onTrackCount;
    }
    
    public Long getDelayedCount() {
        return delayedCount;
    }
    
    public void setDelayedCount(Long delayedCount) {
        this.delayedCount = delayedCount;
    }
}

