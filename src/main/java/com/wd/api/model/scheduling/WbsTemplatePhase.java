package com.wd.api.model.scheduling;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "wbs_template_phase")
public class WbsTemplatePhase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private WbsTemplate template;

    @Column(nullable = false)
    private Integer sequence;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "role_hint", length = 64)
    private String roleHint;

    @Column(name = "monsoon_sensitive", nullable = false)
    private Boolean monsoonSensitive = Boolean.FALSE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "phase", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.LAZY)
    @OrderBy("sequence ASC")
    private List<WbsTemplateTask> tasks = new ArrayList<>();

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (monsoonSensitive == null) monsoonSensitive = Boolean.FALSE;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public WbsTemplate getTemplate() { return template; }
    public void setTemplate(WbsTemplate template) { this.template = template; }
    public Integer getSequence() { return sequence; }
    public void setSequence(Integer sequence) { this.sequence = sequence; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRoleHint() { return roleHint; }
    public void setRoleHint(String roleHint) { this.roleHint = roleHint; }
    public Boolean getMonsoonSensitive() { return monsoonSensitive; }
    public void setMonsoonSensitive(Boolean monsoonSensitive) {
        this.monsoonSensitive = monsoonSensitive == null ? Boolean.FALSE : monsoonSensitive;
    }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<WbsTemplateTask> getTasks() { return tasks; }
    public void setTasks(List<WbsTemplateTask> tasks) { this.tasks = tasks; }
}
