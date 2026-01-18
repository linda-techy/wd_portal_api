package com.wd.api.service;

import com.wd.api.dto.LeadSearchFilter;
import com.wd.api.model.Lead;
import com.wd.api.repository.LeadRepository;
import com.wd.api.util.SpecificationBuilder;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for exporting leads to Excel format
 * Enterprise-grade Excel generation with professional formatting
 */
@Service
public class LeadExportService {

    private static final Logger logger = LoggerFactory.getLogger(LeadExportService.class);

    @Autowired
    private LeadRepository leadRepository;

    /**
     * Generate Excel file for leads matching the search filter
     * Exports ALL matching leads (not paginated) with essential fields
     * 
     * @param filter Search and filter criteria
     * @return Excel file as byte array
     */
    @Transactional(readOnly = true)
    public byte[] generateLeadsExcel(LeadSearchFilter filter) {
        try {
            // Get all matching leads (no pagination for export)
            Specification<Lead> spec = buildSearchSpecification(filter);
            List<Lead> leads = leadRepository.findAll(spec);

            // Create Excel workbook
            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Leads");

                // Create header style
                CellStyle headerStyle = createHeaderStyle(workbook);
                CellStyle dataStyle = createDataStyle(workbook);
                CellStyle dateStyle = createDateStyle(workbook);
                CellStyle currencyStyle = createCurrencyStyle(workbook);

                // Create header row
                int rowNum = 0;
                Row headerRow = sheet.createRow(rowNum++);
                String[] headers = {
                    "Lead ID", "Name", "Email", "Phone", "WhatsApp", "Source", "Status", 
                    "Priority", "Customer Type", "Project Type", "Budget (₹)", 
                    "State", "District", "Location", "Assigned To", 
                    "Created Date", "Last Contact", "Next Follow Up", 
                    "Score", "Score Category"
                };

                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerStyle);
                }

                // Freeze header row
                sheet.createFreezePane(0, 1);

                // Write data rows
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

                for (Lead lead : leads) {
                    Row row = sheet.createRow(rowNum++);
                    int colNum = 0;

                    // Lead ID
                    createCell(row, colNum++, lead.getId() != null ? lead.getId().toString() : "", dataStyle);

                    // Name
                    createCell(row, colNum++, lead.getName() != null ? lead.getName() : "", dataStyle);

                    // Email
                    createCell(row, colNum++, lead.getEmail() != null ? lead.getEmail() : "", dataStyle);

                    // Phone
                    createCell(row, colNum++, lead.getPhone() != null ? lead.getPhone() : "", dataStyle);

                    // WhatsApp
                    createCell(row, colNum++, lead.getWhatsappNumber() != null ? lead.getWhatsappNumber() : "", dataStyle);

                    // Source
                    createCell(row, colNum++, lead.getLeadSource() != null ? lead.getLeadSource() : "", dataStyle);

                    // Status
                    createCell(row, colNum++, lead.getLeadStatus() != null ? lead.getLeadStatus() : "", dataStyle);

                    // Priority
                    createCell(row, colNum++, lead.getPriority() != null ? lead.getPriority() : "", dataStyle);

                    // Customer Type
                    createCell(row, colNum++, lead.getCustomerType() != null ? lead.getCustomerType() : "", dataStyle);

                    // Project Type
                    createCell(row, colNum++, lead.getProjectType() != null ? lead.getProjectType() : "", dataStyle);

                    // Budget
                    if (lead.getBudget() != null) {
                        Cell budgetCell = row.createCell(colNum++);
                        budgetCell.setCellValue(lead.getBudget().doubleValue());
                        budgetCell.setCellStyle(currencyStyle);
                    } else {
                        createCell(row, colNum++, "", dataStyle);
                    }

                    // State
                    createCell(row, colNum++, lead.getState() != null ? lead.getState() : "", dataStyle);

                    // District
                    createCell(row, colNum++, lead.getDistrict() != null ? lead.getDistrict() : "", dataStyle);

                    // Location
                    createCell(row, colNum++, lead.getLocation() != null ? lead.getLocation() : "", dataStyle);

                    // Assigned To
                    String assignedTo = lead.getAssignedTo() != null 
                        ? (lead.getAssignedTo().getFirstName() + " " + lead.getAssignedTo().getLastName()).trim()
                        : (lead.getAssignedTeam() != null ? lead.getAssignedTeam() : "");
                    createCell(row, colNum++, assignedTo, dataStyle);

                    // Created Date
                    if (lead.getCreatedAt() != null) {
                        createCell(row, colNum++, lead.getCreatedAt().format(dateTimeFormatter), dateStyle);
                    } else {
                        createCell(row, colNum++, "", dataStyle);
                    }

                    // Last Contact Date
                    if (lead.getLastContactDate() != null) {
                        createCell(row, colNum++, lead.getLastContactDate().format(dateTimeFormatter), dateStyle);
                    } else {
                        createCell(row, colNum++, "", dataStyle);
                    }

                    // Next Follow Up
                    if (lead.getNextFollowUp() != null) {
                        createCell(row, colNum++, lead.getNextFollowUp().format(dateTimeFormatter), dateStyle);
                    } else {
                        createCell(row, colNum++, "", dataStyle);
                    }

                    // Score
                    if (lead.getScore() != null) {
                        Cell scoreCell = row.createCell(colNum++);
                        scoreCell.setCellValue(lead.getScore());
                        scoreCell.setCellStyle(dataStyle);
                    } else {
                        createCell(row, colNum++, "", dataStyle);
                    }

                    // Score Category
                    createCell(row, colNum++, lead.getScoreCategory() != null ? lead.getScoreCategory() : "", dataStyle);
                }

                // Auto-size columns
                for (int i = 0; i < headers.length; i++) {
                    sheet.autoSizeColumn(i);
                    // Set minimum width
                    if (sheet.getColumnWidth(i) < 2000) {
                        sheet.setColumnWidth(i, 2000);
                    }
                    // Set maximum width to prevent extremely wide columns
                    if (sheet.getColumnWidth(i) > 15000) {
                        sheet.setColumnWidth(i, 15000);
                    }
                }

                // Write to byte array
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                workbook.write(outputStream);
                return outputStream.toByteArray();
            }
        } catch (IOException e) {
            logger.error("Error generating Excel file for leads", e);
            throw new RuntimeException("Failed to generate Excel file", e);
        }
    }

    /**
     * Build search specification from filter (reuse LeadService logic)
     */
    private Specification<Lead> buildSearchSpecification(LeadSearchFilter filter) {
        SpecificationBuilder<Lead> builder = new SpecificationBuilder<>();
        
        // Search across multiple fields
        Specification<Lead> searchSpec = builder.buildSearch(
            filter.getSearchQuery(),
            "name", "email", "phone", "whatsappNumber", "projectDescription"
        );
        
        // Apply filters with status normalization
        Specification<Lead> statusSpec = null;
        if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
            final String normalizedStatus = normalizeStatusForComparison(filter.getStatus());
            statusSpec = (root, query, cb) -> {
                jakarta.persistence.criteria.Expression<String> dbStatusLower = cb.lower(root.get("leadStatus"));
                jakarta.persistence.criteria.Expression<String> dbStatusNoSpaces = cb.function(
                    "REPLACE", String.class, dbStatusLower, cb.literal(" "), cb.literal("")
                );
                jakarta.persistence.criteria.Expression<String> dbStatusCleaned = cb.function(
                    "REPLACE", String.class, dbStatusNoSpaces, cb.literal("_"), cb.literal("")
                );
                
                List<Predicate> statusPredicates = new ArrayList<>();
                if ("new".equals(normalizedStatus)) {
                    statusPredicates.add(cb.equal(dbStatusCleaned, "newinquiry"));
                    statusPredicates.add(cb.equal(dbStatusCleaned, "new"));
                } else if ("qualified".equals(normalizedStatus)) {
                    statusPredicates.add(cb.equal(dbStatusCleaned, "qualifiedlead"));
                    statusPredicates.add(cb.equal(dbStatusCleaned, "qualified"));
                } else if ("proposal_sent".equals(normalizedStatus)) {
                    statusPredicates.add(cb.equal(dbStatusCleaned, "proposalsent"));
                } else if ("won".equals(normalizedStatus)) {
                    statusPredicates.add(cb.equal(dbStatusCleaned, "projectwon"));
                    statusPredicates.add(cb.equal(dbStatusCleaned, "won"));
                    statusPredicates.add(cb.equal(dbStatusCleaned, "converted"));
                } else {
                    statusPredicates.add(cb.equal(dbStatusCleaned, normalizedStatus));
                }
                
                return cb.or(statusPredicates.toArray(new Predicate[0]));
            };
        }
        
        Specification<Lead> sourceSpec = builder.buildEquals("leadSource", filter.getSource());
        Specification<Lead> prioritySpec = builder.buildEquals("priority", filter.getPriority());
        Specification<Lead> customerTypeSpec = builder.buildEquals("customerType", filter.getCustomerType());
        Specification<Lead> projectTypeSpec = builder.buildEquals("projectType", filter.getProjectType());
        Specification<Lead> stateSpec = builder.buildEquals("state", filter.getState());
        Specification<Lead> districtSpec = builder.buildEquals("district", filter.getDistrict());
        
        // Assigned team filter
        Specification<Lead> assignedSpec = null;
        if (filter.getAssignedTeam() != null && !filter.getAssignedTeam().trim().isEmpty()) {
            try {
                Long assignedId = Long.parseLong(filter.getAssignedTeam());
                assignedSpec = (root, query, cb) -> 
                    cb.equal(root.get("assignedTo").get("id"), assignedId);
            } catch (NumberFormatException e) {
                assignedSpec = builder.buildEquals("assignedTeam", filter.getAssignedTeam());
            }
        }
        
        // Budget range
        Specification<Lead> budgetSpec = builder.buildNumericRange(
            "budget", 
            filter.getMinBudget() != null ? BigDecimal.valueOf(filter.getMinBudget()) : null,
            filter.getMaxBudget() != null ? BigDecimal.valueOf(filter.getMaxBudget()) : null
        );
        
        // Date range (on createdAt)
        Specification<Lead> dateRangeSpec = null;
        if (filter.getStartDate() != null || filter.getEndDate() != null) {
            dateRangeSpec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                if (filter.getStartDate() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(
                        root.get("createdAt"), 
                        filter.getStartDate().atStartOfDay()
                    ));
                }
                if (filter.getEndDate() != null) {
                    predicates.add(cb.lessThanOrEqualTo(
                        root.get("createdAt"), 
                        filter.getEndDate().plusDays(1).atStartOfDay()
                    ));
                }
                return cb.and(predicates.toArray(new Predicate[0]));
            };
        }
        
        // Combine all specifications with AND logic
        return builder.and(
            searchSpec,
            statusSpec,
            sourceSpec,
            prioritySpec,
            customerTypeSpec,
            projectTypeSpec,
            assignedSpec,
            stateSpec,
            districtSpec,
            budgetSpec,
            dateRangeSpec
        );
    }

    /**
     * Normalize status for comparison (reuse LeadService logic)
     */
    private String normalizeStatusForComparison(String status) {
        if (status == null || status.isEmpty()) {
            return status;
        }
        String cleaned = status.toLowerCase().trim().replaceAll("[\\s_]", "");
        
        if (cleaned.equals("newinquiry") || cleaned.equals("new")) {
            return "new";
        } else if (cleaned.equals("contacted")) {
            return "contacted";
        } else if (cleaned.equals("qualifiedlead") || cleaned.equals("qualified")) {
            return "qualified";
        } else if (cleaned.equals("proposalsent")) {
            return "proposal_sent";
        } else if (cleaned.equals("projectwon") || cleaned.equals("won") || cleaned.equals("converted")) {
            return "won";
        } else if (cleaned.equals("lost")) {
            return "lost";
        } else {
            return cleaned;
        }
    }

    /**
     * Create header cell style
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * Create data cell style
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * Create date cell style
     */
    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        DataFormat dataFormat = workbook.createDataFormat();
        style.setDataFormat(dataFormat.getFormat("dd/mm/yyyy hh:mm"));
        return style;
    }

    /**
     * Create currency cell style
     */
    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        DataFormat dataFormat = workbook.createDataFormat();
        style.setDataFormat(dataFormat.getFormat("#,##0.00"));
        return style;
    }

    /**
     * Helper method to create a cell with value and style
     */
    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }
}
