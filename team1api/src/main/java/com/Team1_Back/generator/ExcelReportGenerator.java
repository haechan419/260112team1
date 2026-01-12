package com.Team1_Back.generator;

import com.Team1_Back.domain.enums.DataScope;
import com.Team1_Back.report.entity.ReportJob;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.nio.file.Path;
@Component
public class ExcelReportGenerator {

    public void generate(Path outputFile, ReportJob job) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Report");

            // 헤더 스타일
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row r0 = sheet.createRow(0);
            Cell h0 = r0.createCell(0);
            h0.setCellValue("Key");
            h0.setCellStyle(headerStyle);

            Cell h1 = r0.createCell(1);
            h1.setCellValue("Value");
            h1.setCellStyle(headerStyle);

            int row = 1;

            row = kv(sheet, row, "Report Type", job.getReportTypeId());
            row = kv(sheet, row, "Report ID", String.valueOf(job.getId()));
            row = kv(sheet, row, "Period", job.getPeriod());

            // ✅ 여기 핵심
            row = kv(sheet, row, "Scope", displayScopeWithDept(job));

            row = kv(sheet, row, "Category", job.getCategoryJson());
            row = kv(sheet, row, "Requested By", String.valueOf(job.getRequestedBy()));

            // snapshot은 남겨도 되고, 싫으면 제거 가능
            row = kv(sheet, row, "Dept (snapshot)", job.getDepartmentSnapshot());

            // 승인 집계
            long approvedCount = job.getApprovedCount() == null ? 0 : job.getApprovedCount();
            long approvedTotal = job.getApprovedTotal() == null ? 0 : job.getApprovedTotal();

            row = kv(sheet, row, "Records Included", formatNumber(approvedCount));
            row = kv(sheet, row, "Total Amount (KRW)", formatNumber(approvedTotal));

            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

            try (FileOutputStream fos = new FileOutputStream(outputFile.toFile())) {
                wb.write(fos);
            }
        }
    }

    private int kv(Sheet sheet, int rowIdx, String key, String value) {
        Row r = sheet.createRow(rowIdx);
        r.createCell(0).setCellValue(key);
        r.createCell(1).setCellValue((value == null || value.isBlank()) ? "-" : value);
        return rowIdx + 1;
    }

    /**
     * Scope 표시: Department - 개발2팀
     */
    private String displayScopeWithDept(ReportJob job) {
        if (job.getDataScope() == null) return "";

        return switch (job.getDataScope()) {
            case MY -> "My Data";
            case ALL -> "All";
            case DEPT -> {
                String dept = job.getDepartmentSnapshot();
                yield (dept == null || dept.isBlank())
                        ? "Department"
                        : "Department - " + dept;
            }
        };
    }

    private String formatNumber(long n) {
        return String.format("%,d", n);
    }
}
