package com.wd.api.service;

import com.wd.api.dto.LabourDTO;
import com.wd.api.dto.LabourAttendanceDTO;
import com.wd.api.dto.MeasurementBookDTO;
import com.wd.api.model.Labour;
import com.wd.api.model.LabourAttendance;
import com.wd.api.model.MeasurementBook;
import com.wd.api.model.BoqItem;
import com.wd.api.repository.LabourRepository;
import com.wd.api.repository.LabourAttendanceRepository;
import com.wd.api.repository.MeasurementBookRepository;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.BoqItemRepository;
import com.wd.api.repository.WageSheetRepository;
import com.wd.api.repository.LabourAdvanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LabourService {

        private final LabourRepository labourRepository;
        private final LabourAttendanceRepository attendanceRepository;
        private final MeasurementBookRepository mbRepository;
        private final CustomerProjectRepository projectRepository;
        private final BoqItemRepository boqItemRepository;
        private final WageSheetRepository wageSheetRepository;
        private final LabourAdvanceRepository labourAdvanceRepository;

        @Transactional
        public LabourDTO createLabour(LabourDTO dto) {
                Labour labour = Labour.builder()
                                .name(dto.getName())
                                .phone(dto.getPhone())
                                .tradeType(dto.getTradeType())
                                .idProofType(dto.getIdProofType())
                                .idProofNumber(dto.getIdProofNumber())
                                .dailyWage(dto.getDailyWage())
                                .emergencyContact(dto.getEmergencyContact())
                                .active(true)
                                .build();
                Labour savedLabour = labourRepository.save(labour);
                return mapToLabourDTO(java.util.Objects.requireNonNull(savedLabour));
        }

        public List<LabourDTO> getAllLabour() {
                return labourRepository.findAll().stream()
                                .map(this::mapToLabourDTO)
                                .collect(Collectors.toList());
        }

        @Transactional
        public List<LabourAttendanceDTO> recordAttendance(List<LabourAttendanceDTO> dtoList) {
                return dtoList.stream().map(dto -> {
                        Long projectId = java.util.Objects.requireNonNull(dto.getProjectId());
                        var project = projectRepository.findById(projectId)
                                        .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));
                        Long labourId = java.util.Objects.requireNonNull(dto.getLabourId());
                        var labour = labourRepository.findById(labourId)
                                        .orElseThrow(() -> new RuntimeException("Labour not found: " + labourId));

                        LabourAttendance attendance = com.wd.api.model.LabourAttendance.builder()
                                        .project(project)
                                        .labour(labour)
                                        .attendanceDate(dto.getAttendanceDate())
                                        .status(dto.getStatus())
                                        .hoursWorked(dto.getHoursWorked())
                                        .build();
                        LabourAttendance savedAttendance = attendanceRepository.save(attendance);
                        return mapToAttendanceDTO(java.util.Objects.requireNonNull(savedAttendance));
                }).collect(Collectors.toList());
        }

        @Transactional
        public MeasurementBookDTO createMBEntry(MeasurementBookDTO dto) {
                Long projectId = java.util.Objects.requireNonNull(dto.getProjectId());
                var project = projectRepository.findById(projectId)
                                .orElseThrow(() -> new RuntimeException("Project not found"));

                Labour labour = null;
                if (dto.getLabourId() != null) {
                        Long lId = java.util.Objects.requireNonNull(dto.getLabourId());
                        labour = labourRepository.findById(lId)
                                        .orElseThrow(() -> new RuntimeException("Labour not found"));
                }

                BoqItem boqItem = null;
                if (dto.getBoqItemId() != null) {
                        Long bId = java.util.Objects.requireNonNull(dto.getBoqItemId());
                        boqItem = boqItemRepository.findById(bId)
                                        .orElseThrow(() -> new RuntimeException("BOQ Item not found"));
                }

                MeasurementBook mb = MeasurementBook.builder()
                                .project(project)
                                .labour(labour)
                                .boqItem(boqItem)
                                .description(dto.getDescription())
                                .measurementDate(dto.getMeasurementDate())
                                .length(dto.getLength())
                                .breadth(dto.getBreadth())
                                .depth(dto.getDepth())
                                .quantity(dto.getQuantity())
                                .unit(dto.getUnit())
                                .rate(dto.getRate())
                                .totalAmount(dto.getTotalAmount())
                                .build();

                MeasurementBook savedMb = mbRepository.save(mb);
                return mapToMBDTO(java.util.Objects.requireNonNull(savedMb));
        }

        public List<MeasurementBookDTO> getMBEntriesByProject(Long projectId) {
                return mbRepository.findByProjectId(projectId).stream()
                                .map(this::mapToMBDTO)
                                .collect(Collectors.toList());
        }

        @Transactional
        public com.wd.api.model.WageSheet generateWageSheet(Long projectId, java.time.LocalDate start,
                        java.time.LocalDate end) {
                var project = projectRepository.findById(projectId)
                                .orElseThrow(() -> new RuntimeException("Project not found"));

                List<com.wd.api.model.LabourAttendance> attendances = attendanceRepository
                                .findByProjectIdAndAttendanceDateBetween(projectId, start, end);

                // Group by Labour
                java.util.Map<Labour, Double> labourDays = attendances.stream()
                                .collect(Collectors.groupingBy(com.wd.api.model.LabourAttendance::getLabour,
                                                Collectors.summingDouble(a -> a.getHoursWorked() != null
                                                                && a.getHoursWorked() > 0 ? (a.getHoursWorked() / 8.0)
                                                                                : 1.0))); // Assuming 8hr day or simple
                                                                                          // count

                com.wd.api.model.WageSheet sheet = com.wd.api.model.WageSheet.builder()
                                .project(project)
                                .periodStart(start)
                                .periodEnd(end)
                                .sheetNumber("WS-" + System.currentTimeMillis())
                                .status(com.wd.api.model.WageSheet.SheetStatus.DRAFT)
                                .totalAmount(java.math.BigDecimal.ZERO)
                                .entries(new java.util.ArrayList<>())
                                .build();

                java.math.BigDecimal totalSheetAmount = java.math.BigDecimal.ZERO;

                for (java.util.Map.Entry<Labour, Double> entry : labourDays.entrySet()) {
                        Labour l = entry.getKey();
                        Double days = entry.getValue();
                        java.math.BigDecimal dailyWage = l.getDailyWage();
                        java.math.BigDecimal totalWage = dailyWage.multiply(java.math.BigDecimal.valueOf(days));

                        com.wd.api.model.WageSheetEntry wsEntry = com.wd.api.model.WageSheetEntry.builder()
                                        .wageSheet(sheet)
                                        .labour(l)
                                        .daysWorked(java.math.BigDecimal.valueOf(days))
                                        .dailyWage(dailyWage)
                                        .totalWage(totalWage)
                                        .advancesDeducted(java.math.BigDecimal.ZERO) // Logic for deduction can be added
                                        .netPayable(totalWage)
                                        .build();

                        sheet.getEntries().add(wsEntry);
                        totalSheetAmount = totalSheetAmount.add(totalWage);
                }
                sheet.setTotalAmount(totalSheetAmount);

                return wageSheetRepository.save(sheet);
        }

        @Transactional
        public com.wd.api.model.LabourAdvance createAdvance(Long labourId, java.math.BigDecimal amount, String notes) {
                Labour labour = labourRepository.findById(labourId)
                                .orElseThrow(() -> new RuntimeException("Labour not found"));
                com.wd.api.model.LabourAdvance advance = com.wd.api.model.LabourAdvance.builder()
                                .labour(labour)
                                .amount(amount)
                                .advanceDate(java.time.LocalDate.now())
                                .notes(notes)
                                .build();
                return labourAdvanceRepository.save(advance);
        }

        private LabourDTO mapToLabourDTO(Labour l) {
                return LabourDTO.builder()
                                .id(l.getId())
                                .name(l.getName())
                                .phone(l.getPhone())
                                .tradeType(l.getTradeType())
                                .idProofType(l.getIdProofType())
                                .idProofNumber(l.getIdProofNumber())
                                .dailyWage(l.getDailyWage())
                                .emergencyContact(l.getEmergencyContact())
                                .active(l.isActive())
                                .build();
        }

        private LabourAttendanceDTO mapToAttendanceDTO(LabourAttendance a) {
                return LabourAttendanceDTO.builder()
                                .id(a.getId())
                                .projectId(a.getProject().getId())
                                .labourId(a.getLabour().getId())
                                .labourName(a.getLabour().getName())
                                .attendanceDate(a.getAttendanceDate())
                                .status(a.getStatus())
                                .hoursWorked(a.getHoursWorked())
                                .build();
        }

        private MeasurementBookDTO mapToMBDTO(MeasurementBook m) {
                return MeasurementBookDTO.builder()
                                .id(m.getId())
                                .projectId(m.getProject().getId())
                                .labourId(m.getLabour() != null ? m.getLabour().getId() : null)
                                .boqItemId(m.getBoqItem() != null ? m.getBoqItem().getId() : null)
                                .description(m.getDescription())
                                .measurementDate(m.getMeasurementDate())
                                .length(m.getLength())
                                .breadth(m.getBreadth())
                                .depth(m.getDepth())
                                .quantity(m.getQuantity())
                                .unit(m.getUnit())
                                .rate(m.getRate())
                                .totalAmount(m.getTotalAmount())
                                .build();
        }
}
