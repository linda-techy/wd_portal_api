package com.wd.api.estimation.dto;

public enum SubResourceType {
    INCLUSION,
    EXCLUSION,
    ASSUMPTION,
    PAYMENT_MILESTONE;

    /**
     * Maps a URL path segment to the corresponding enum value.
     * Accepted values: inclusions, exclusions, assumptions, payment-milestones.
     */
    public static SubResourceType forPath(String pathSegment) {
        return switch (pathSegment) {
            case "inclusions"        -> INCLUSION;
            case "exclusions"        -> EXCLUSION;
            case "assumptions"       -> ASSUMPTION;
            case "payment-milestones" -> PAYMENT_MILESTONE;
            default -> throw new IllegalArgumentException(
                    "Unknown sub-resource type: " + pathSegment
                    + ". Expected one of: inclusions, exclusions, assumptions, payment-milestones");
        };
    }
}
