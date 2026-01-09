package com.wd.api.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class SqftCategories {

    private UUID id;
    private String category;
    private Integer lowestSqft;
    private Integer highestSqft;
    private String modifiedBy;
    private LocalDateTime updateDate;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getLowestSqft() {
        return lowestSqft;
    }

    public void setLowestSqft(Integer lowestSqft) {
        this.lowestSqft = lowestSqft;
    }

    public Integer getHighestSqft() {
        return highestSqft;
    }

    public void setHighestSqft(Integer highestSqft) {
        this.highestSqft = highestSqft;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
    }
}
