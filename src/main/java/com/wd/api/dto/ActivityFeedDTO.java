package com.wd.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityFeedDTO {
    private Long id;
    private String title;
    private String description;
    private String activityType; // Icon/Color logic often coupled here
    private LocalDateTime createdAt;
    private String createdByName;
}
