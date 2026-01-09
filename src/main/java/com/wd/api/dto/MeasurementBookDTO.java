package com.wd.api.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeasurementBookDTO {
    private Long id;
    private Long projectId;
    private Long labourId;
    private Long boqItemId;
    private String description;
    private LocalDate measurementDate;
    private BigDecimal length;
    private BigDecimal breadth;
    private BigDecimal depth;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal rate;
    private BigDecimal totalAmount;
}
