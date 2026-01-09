package com.wd.api.dto;

import com.wd.api.model.enums.PaymentMethod;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabourPaymentDTO {
    private Long id;
    private Long labourId;
    private String labourName;
    private Long projectId;
    private String projectName;
    private Long mbEntryId;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private PaymentMethod paymentMethod;
    private String notes;
}
