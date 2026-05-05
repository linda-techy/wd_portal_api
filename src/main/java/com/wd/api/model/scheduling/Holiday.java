package com.wd.api.model.scheduling;

import com.wd.api.model.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;

@SQLDelete(sql = "UPDATE holiday SET deleted_at = NOW() WHERE id = ? AND version = ?")
@Where(clause = "deleted_at IS NULL")
@Entity
@Table(name = "holiday")
public class Holiday extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private HolidayScope scope;

    @Column(name = "scope_ref", length = 64)
    private String scopeRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_type", nullable = false, length = 16)
    private HolidayRecurrenceType recurrenceType;

    @Column(length = 32)
    private String source;

    @Column(name = "is_active", nullable = false)
    private Boolean active = Boolean.TRUE;

    public Holiday() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public HolidayScope getScope() { return scope; }
    public void setScope(HolidayScope scope) { this.scope = scope; }
    public String getScopeRef() { return scopeRef; }
    public void setScopeRef(String scopeRef) { this.scopeRef = scopeRef; }
    public HolidayRecurrenceType getRecurrenceType() { return recurrenceType; }
    public void setRecurrenceType(HolidayRecurrenceType recurrenceType) { this.recurrenceType = recurrenceType; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
