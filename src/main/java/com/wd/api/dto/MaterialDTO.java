package com.wd.api.dto;

import jakarta.validation.constraints.Pattern;

public class MaterialDTO {
    private Long id;
    private String name;
    private String unit;
    private String category;
    private boolean active;

    // G-15: GST HSN (goods, 4-8 digits) or SAC (services, 6 digits typically
    // starting with 99). Optional in request payloads for now so existing
    // callers don't break; format is validated when supplied.
    @Pattern(regexp = "^[0-9]{4,8}$",
            message = "HSN/SAC code must be 4-8 digits (e.g. '7308' for steel structures)")
    private String hsnSacCode;

    public MaterialDTO() {
    }

    public MaterialDTO(Long id, String name, String unit, String category, boolean active) {
        this(id, name, unit, category, active, null);
    }

    public MaterialDTO(Long id, String name, String unit, String category,
                       boolean active, String hsnSacCode) {
        this.id = id;
        this.name = name;
        this.unit = unit;
        this.category = category;
        this.active = active;
        this.hsnSacCode = hsnSacCode;
    }

    public static MaterialDTOBuilder builder() {
        return new MaterialDTOBuilder();
    }

    public static class MaterialDTOBuilder {
        private Long id;
        private String name;
        private String unit;
        private String category;
        private boolean active;
        private String hsnSacCode;

        public MaterialDTOBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public MaterialDTOBuilder name(String name) {
            this.name = name;
            return this;
        }

        public MaterialDTOBuilder unit(String unit) {
            this.unit = unit;
            return this;
        }

        public MaterialDTOBuilder category(String category) {
            this.category = category;
            return this;
        }

        public MaterialDTOBuilder active(boolean active) {
            this.active = active;
            return this;
        }

        public MaterialDTOBuilder hsnSacCode(String hsnSacCode) {
            this.hsnSacCode = hsnSacCode;
            return this;
        }

        public MaterialDTO build() {
            return new MaterialDTO(id, name, unit, category, active, hsnSacCode);
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    public String getCategory() {
        return category;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getHsnSacCode() {
        return hsnSacCode;
    }

    public void setHsnSacCode(String hsnSacCode) {
        this.hsnSacCode = hsnSacCode;
    }
}
