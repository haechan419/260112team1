package com.Team1_Back.generator;

import com.Team1_Back.domain.enums.DataScope;
import com.Team1_Back.report.entity.ReportJob;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.Locale;

@Component
public class PdfReportGenerator {

    private String displayScope(DataScope scope) {
        if (scope == null) return "";
        return switch (scope) {
            case MY -> "My Data";
            case DEPT -> "Department";
            case ALL -> "All";
        };
    }

    private String moneyKrw(Long v) {
        long n = (v == null) ? 0L : v;
        return "₩" + NumberFormat.getNumberInstance(Locale.KOREA).format(n);
    }

    public void generate(Path outputFile, ReportJob job) throws Exception {
        Document doc = new Document(PageSize.A4, 48, 48, 56, 56);
        PdfWriter.getInstance(doc, new FileOutputStream(outputFile.toFile()));
        doc.open();

        Paragraph title = new Paragraph("Report Summary", new Font(Font.HELVETICA, 16, Font.BOLD));
        doc.add(title);

        doc.add(new Paragraph("Confidential", new Font(Font.HELVETICA, 10, Font.ITALIC)));
        doc.add(new Paragraph(" "));

        doc.add(kv("Report Type", job.getReportTypeId()));
        doc.add(kv("Report ID", String.valueOf(job.getId())));
        doc.add(kv("Dept(snapshot)", job.getDepartmentSnapshot()));
        doc.add(kv("Scope", displayScopeWithDept(job)));

        doc.add(kv("Period", job.getPeriod()));

        doc.add(kv("Category", job.getCategoryJson()));
        doc.add(kv("Requested By", String.valueOf(job.getRequestedBy())));

        doc.add(new Paragraph(" "));

        // ✅ EXPENSE 승인합계용 값(없으면 0/0)
        doc.add(new Paragraph("• Records Included : " + (job.getApprovedCount() == null ? 0 : job.getApprovedCount())));
        doc.add(new Paragraph("• Total Amount     : " + moneyKrw(job.getApprovedTotal())));

        doc.close();
    }

    private Paragraph kv(String k, String v) {
        String val = (v == null || v.isBlank()) ? "-" : v;
        return new Paragraph(k + " : " + val, new Font(Font.HELVETICA, 11));
    }

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

}

