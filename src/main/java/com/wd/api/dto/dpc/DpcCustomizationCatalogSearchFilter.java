package com.wd.api.dto.dpc;

/**
 * Filter / pagination params for searching the DPC customization catalog.
 *
 * <ul>
 *   <li>{@code search} — ILIKE on code OR name</li>
 *   <li>{@code category} — exact match</li>
 *   <li>{@code isActive} — exact match (null = no filter)</li>
 *   <li>Sort defaults to {@code timesUsed desc} so popular items surface first.</li>
 * </ul>
 */
public record DpcCustomizationCatalogSearchFilter(
        String search,
        String category,
        Boolean isActive,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
) {

    public DpcCustomizationCatalogSearchFilter {
        if (page == null || page < 0) page = 0;
        if (size == null || size <= 0) size = 50;
        if (sortBy == null || sortBy.isBlank()) sortBy = "timesUsed";
        if (sortDirection == null || sortDirection.isBlank()) sortDirection = "desc";
    }

    public int pageOrDefault() { return page == null ? 0 : page; }
    public int sizeOrDefault() { return size == null ? 50 : size; }
    public String sortByOrDefault() { return sortBy == null ? "timesUsed" : sortBy; }
    public String sortDirectionOrDefault() { return sortDirection == null ? "desc" : sortDirection; }
}
