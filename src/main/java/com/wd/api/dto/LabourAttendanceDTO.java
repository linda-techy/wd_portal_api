package com.wd.api.dto;

import com.wd.api.model.enums.AttendanceStatus;
import lombok.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabourAttendanceDTO {
    private Long id;
    private Long projectId;
    private Long labourId;
    private String labourName;
    private LocalDate attendanceDate;
    private AttendanceStatus status;
    private Double hoursWorked;
}
