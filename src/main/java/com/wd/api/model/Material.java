package com.wd.api.model;

import com.wd.api.model.enums.MaterialCategory;
import com.wd.api.model.enums.MaterialUnit;
import jakarta.persistence.*;

@Entity
@Table(name = "materials")
public class Material extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    /**
     * Unit of measurement for this material.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaterialUnit unit;

    /**
     * Category classification for this material.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaterialCategory category;

    @Column(name = "is_active")
    private boolean active = true;

    public Material() {
    }

    public Material(Long id, String name, MaterialUnit unit, MaterialCategory category, boolean active) {
        this.id = id;
        this.name = name;
        this.unit = unit;
        this.category = category;
        this.active = active;
    }

    public static MaterialBuilder builder() {
        return new MaterialBuilder();
    }

    public static class MaterialBuilder {
        private Long id;
        private String name;
        private MaterialUnit unit;
        private MaterialCategory category;
        private boolean active = true;

        public MaterialBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public MaterialBuilder name(String name) {
            this.name = name;
            return this;
        }

        public MaterialBuilder unit(MaterialUnit unit) {
            this.unit = unit;
            return this;
        }

        public MaterialBuilder category(MaterialCategory category) {
            this.category = category;
            return this;
        }

        public MaterialBuilder active(boolean active) {
            this.active = active;
            return this;
        }

        public Material build() {
            return new Material(id, name, unit, category, active);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public MaterialUnit getUnit() {
        return unit;
    }

    public MaterialCategory getCategory() {
        return category;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUnit(MaterialUnit unit) {
        this.unit = unit;
    }

    public void setCategory(MaterialCategory category) {
        this.category = category;
    }
}
