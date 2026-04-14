package com.wd.api.service;

import com.wd.api.dto.BoqFinancialSummary;
import com.wd.api.dto.BoqItemResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates an Excel workbook for a project's Bill of Quantities.
 * Sheet 1 — Item register (one row per BOQ item)
 * Sheet 2 — Financial summary (totals + category / work-type breakdown)
 */
@Service
public class BoqExportService {

    private static final Logger logger = LoggerFactory.getLogger(BoqExportService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final BoqService boqService;

    public BoqExportService(BoqService boqService) {
        this.boqService = boqService;
    }

    @Transactional(readOnly = true)
    public byte[] generateExcel(Long projectId, Long userId) {
        List<BoqItemResponse> items = boqService.getProjectBoq(projectId, userId);
        BoqFinancialSummary summary = boqService.getFinancialSummary(userId, projectId);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            buildItemSheet(wb, items);
            buildSummarySheet(wb, summary);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            logger.error("Error generating BOQ Excel for project {}", projectId, e);
            throw new RuntimeException("Failed to generate BOQ Excel", e);
        }
    }

    // ---- Sheet 1: Items -------------------------------------------------------

    private void buildItemSheet(XSSFWorkbook wb, List<BoqItemResponse> items) {
        Sheet sheet = wb.createSheet("BOQ Items");
        sheet.createFreezePane(0, 1);

        CellStyle header = headerStyle(wb);
        CellStyle data   = dataStyle(wb);
        CellStyle money  = moneyStyle(wb);
        CellStyle pct    = percentStyle(wb);

        String[] cols = {
            "#", "Item Code", "Category", "Work Type", "Description",
            "Unit", "Quantity", "Unit Rate (₹)", "Total Amount (₹)",
            "Executed Qty", "Billed Qty", "Remaining Qty",
            "Exec. Amount (₹)", "Billed Amount (₹)", "Cost to Complete (₹)",
            "Exec. %", "Billing %", "Status", "Created"
        };

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < cols.length; i++) {
            createCell(headerRow, i, cols[i], header);
        }

        int rowNum = 1;
        for (BoqItemResponse item : items) {
            Row row = sheet.createRow(rowNum++);
            int c = 0;
            createCell(row, c++, String.valueOf(rowNum - 1), data);
            createCell(row, c++, nvl(item.itemCode()), data);
            createCell(row, c++, nvl(item.categoryName()), data);
            createCell(row, c++, nvl(item.workTypeName()), data);
            createCell(row, c++, nvl(item.description()), data);
            createCell(row, c++, nvl(item.unit()), data);
            createNumericCell(row, c++, item.quantity(), data);
            createNumericCell(row, c++, item.unitRate(), money);
            createNumericCell(row, c++, item.totalAmount(), money);
            createNumericCell(row, c++, item.executedQuantity(), data);
            createNumericCell(row, c++, item.billedQuantity(), data);
            createNumericCell(row, c++, item.remainingQuantity(), data);
            createNumericCell(row, c++, item.totalExecutedAmount(), money);
            createNumericCell(row, c++, item.totalBilledAmount(), money);
            createNumericCell(row, c++, item.costToComplete(), money);
            createNumericCell(row, c++, item.executionPercentage(), pct);
            createNumericCell(row, c++, item.billingPercentage(), pct);
            createCell(row, c++, nvl(item.status()), data);
            createCell(row, c++,
                item.createdAt() != null ? item.createdAt().format(DATE_FMT) : "", data);
        }

        // Auto-size columns (cap at 15 000 units ~100 chars)
        for (int i = 0; i < cols.length; i++) {
            sheet.autoSizeColumn(i);
            int w = sheet.getColumnWidth(i);
            if (w < 2_500) sheet.setColumnWidth(i, 2_500);
            if (w > 15_000) sheet.setColumnWidth(i, 15_000);
        }
    }

    // ---- Sheet 2: Summary -----------------------------------------------------

    private void buildSummarySheet(XSSFWorkbook wb, BoqFinancialSummary s) {
        Sheet sheet = wb.createSheet("Financial Summary");

        CellStyle title  = titleStyle(wb);
        CellStyle header = headerStyle(wb);
        CellStyle label  = labelStyle(wb);
        CellStyle data   = dataStyle(wb);
        CellStyle money  = moneyStyle(wb);
        CellStyle pct    = percentStyle(wb);

        int row = 0;

        // Project title
        Row titleRow = sheet.createRow(row++);
        Cell tc = titleRow.createCell(0);
        tc.setCellValue("BOQ Financial Summary — " + s.projectName());
        tc.setCellStyle(title);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));
        row++;

        // Overall totals
        row = addLabelValue(sheet, row, "Total Items",          String.valueOf(s.totalItems()), label, data);
        row = addLabelValue(sheet, row, "Active Items",         String.valueOf(s.activeItems()), label, data);
        row = addLabelMoney(sheet, row, "Planned Cost",         s.totalPlannedCost(), label, money);
        row = addLabelMoney(sheet, row, "Executed Cost",        s.totalExecutedCost(), label, money);
        row = addLabelMoney(sheet, row, "Billed Cost",          s.totalBilledCost(), label, money);
        row = addLabelMoney(sheet, row, "Cost to Complete",     s.totalCostToComplete(), label, money);
        row = addLabelPercent(sheet, row, "Execution %",        s.overallExecutionPercentage(), label, pct);
        row = addLabelPercent(sheet, row, "Billing %",          s.overallBillingPercentage(), label, pct);
        row++;

        // Category breakdown
        if (!s.categoryBreakdown().isEmpty()) {
            Row hdr = sheet.createRow(row++);
            for (int i = 0; i < 6; i++) hdr.createCell(i).setCellStyle(header);
            hdr.getCell(0).setCellValue("Category");
            hdr.getCell(1).setCellValue("Items");
            hdr.getCell(2).setCellValue("Planned (₹)");
            hdr.getCell(3).setCellValue("Executed (₹)");
            hdr.getCell(4).setCellValue("Billed (₹)");
            hdr.getCell(5).setCellValue("Cost to Complete (₹)");

            for (var c : s.categoryBreakdown()) {
                Row r = sheet.createRow(row++);
                createCell(r, 0, c.categoryName(), data);
                createNumericCell(r, 1, BigDecimal.valueOf(c.itemCount()), data);
                createNumericCell(r, 2, c.plannedCost(), money);
                createNumericCell(r, 3, c.executedCost(), money);
                createNumericCell(r, 4, c.billedCost(), money);
                createNumericCell(r, 5, c.costToComplete(), money);
            }
            row++;
        }

        // Work-type breakdown
        if (!s.workTypeBreakdown().isEmpty()) {
            Row hdr = sheet.createRow(row++);
            for (int i = 0; i < 6; i++) hdr.createCell(i).setCellStyle(header);
            hdr.getCell(0).setCellValue("Work Type");
            hdr.getCell(1).setCellValue("Items");
            hdr.getCell(2).setCellValue("Planned (₹)");
            hdr.getCell(3).setCellValue("Executed (₹)");
            hdr.getCell(4).setCellValue("Billed (₹)");
            hdr.getCell(5).setCellValue("Cost to Complete (₹)");

            for (var w : s.workTypeBreakdown()) {
                Row r = sheet.createRow(row++);
                createCell(r, 0, w.workTypeName(), data);
                createNumericCell(r, 1, BigDecimal.valueOf(w.itemCount()), data);
                createNumericCell(r, 2, w.plannedCost(), money);
                createNumericCell(r, 3, w.executedCost(), money);
                createNumericCell(r, 4, w.billedCost(), money);
                createNumericCell(r, 5, w.costToComplete(), money);
            }
        }

        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
            int w = sheet.getColumnWidth(i);
            if (w < 3_500) sheet.setColumnWidth(i, 3_500);
            if (w > 15_000) sheet.setColumnWidth(i, 15_000);
        }
    }

    // ---- Style helpers --------------------------------------------------------

    private CellStyle titleStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 14);
        s.setFont(f);
        return s;
    }

    private CellStyle headerStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true); f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        borders(s);
        return s;
    }

    private CellStyle labelStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true);
        s.setFont(f);
        borders(s);
        return s;
    }

    private CellStyle dataStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        borders(s);
        return s;
    }

    private CellStyle moneyStyle(Workbook wb) {
        CellStyle s = dataStyle(wb);
        DataFormat df = wb.createDataFormat();
        s.setDataFormat(df.getFormat("#,##0.00"));
        return s;
    }

    private CellStyle percentStyle(Workbook wb) {
        CellStyle s = dataStyle(wb);
        DataFormat df = wb.createDataFormat();
        s.setDataFormat(df.getFormat("0.00"));
        return s;
    }

    private void borders(CellStyle s) {
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
    }

    // ---- Cell creation helpers ------------------------------------------------

    private void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void createNumericCell(Row row, int col, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value != null) cell.setCellValue(value.doubleValue());
        else cell.setCellValue(0.0);
        cell.setCellStyle(style);
    }

    private int addLabelValue(Sheet sheet, int row, String label, String value,
                               CellStyle labelStyle, CellStyle dataStyle) {
        Row r = sheet.createRow(row);
        createCell(r, 0, label, labelStyle);
        createCell(r, 1, value, dataStyle);
        return row + 1;
    }

    private int addLabelMoney(Sheet sheet, int row, String label, BigDecimal value,
                               CellStyle labelStyle, CellStyle moneyStyle) {
        Row r = sheet.createRow(row);
        createCell(r, 0, label, labelStyle);
        createNumericCell(r, 1, value, moneyStyle);
        return row + 1;
    }

    private int addLabelPercent(Sheet sheet, int row, String label, BigDecimal value,
                                 CellStyle labelStyle, CellStyle pctStyle) {
        Row r = sheet.createRow(row);
        createCell(r, 0, label, labelStyle);
        createNumericCell(r, 1, value, pctStyle);
        return row + 1;
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
