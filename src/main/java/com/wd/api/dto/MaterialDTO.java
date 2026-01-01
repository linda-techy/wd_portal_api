package com.wd.api.dto;

public class MaterialDTO {
    private Long id;
    private String name;
    private String unit;
    private String category;
    private boolean active;

    public MaterialDTO() {
    }

    public MaterialDTO(Long id, String name, String unit, String category, boolean active) {
        this.id = id;
        this.name = name;
        this.unit = unit;
        this.category = category;
        this.active = active;
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

        public MaterialDTO build() {
            return new MaterialDTO(id, name, unit, category, active);
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
}
