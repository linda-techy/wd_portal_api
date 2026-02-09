package com.wd.api.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "sqft_categories")
public class SqftCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 100)
    private String name;

    @Column(name = "min_sqft")
    private Integer minSqft;

    @Column(name = "max_sqft")
    private Integer maxSqft;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMinSqft() {
        return minSqft;
    }

    public void setMinSqft(Integer minSqft) {
        this.minSqft = minSqft;
    }

    public Integer getMaxSqft() {
        return maxSqft;
    }

    public void setMaxSqft(Integer maxSqft) {
        this.maxSqft = maxSqft;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
