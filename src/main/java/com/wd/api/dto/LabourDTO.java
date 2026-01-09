package com.wd.api.dto;

import com.wd.api.model.enums.IdProofType;
import com.wd.api.model.enums.LabourTradeType;
import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabourDTO {
    private Long id;
    private String name;
    private String phone;
    private LabourTradeType tradeType;
    private IdProofType idProofType;
    private String idProofNumber;
    private BigDecimal dailyWage;
    private String emergencyContact;
    private boolean active;
}
