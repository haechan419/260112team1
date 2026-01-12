package com.Team1_Back.constants;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReportTypes {

    public static final String FMT_PDF = "PDF";
    public static final String FMT_EXCEL = "EXCEL";

    public static final String EXPENSE_APPROVED_SUMMARY_PDF = "EXPENSE_APPROVED_SUMMARY_PDF";
    public static final String EXPENSE_APPROVED_SUMMARY_EXCEL = "EXPENSE_APPROVED_SUMMARY_EXCEL";


    // EMPLOYEE(USER)
    public static final String PERSONAL_DETAIL_EXCEL = "PERSONAL_DETAIL_EXCEL";
    public static final String PERSONAL_SUMMARY_PDF  = "PERSONAL_SUMMARY_PDF";

    // ADMIN
    public static final String DEPT_DETAIL_EXCEL     = "DEPT_DETAIL_EXCEL";
    public static final String DEPT_SUMMARY_PDF      = "DEPT_SUMMARY_PDF";
    public static final String AI_STRATEGY_PDF       = "AI_STRATEGY_PDF";

    public record TypeDef(String id, String label, String format, boolean adminOnly) {}

    public static final List<TypeDef> ALL = List.of(
            new TypeDef(PERSONAL_DETAIL_EXCEL, "Personal Detailed Records (Excel)", FMT_EXCEL, false),
            new TypeDef(PERSONAL_SUMMARY_PDF,  "Personal Summary Report (PDF)",     FMT_PDF,   false),

            new TypeDef(DEPT_DETAIL_EXCEL,     "Department Detailed Records (Excel)", FMT_EXCEL, true),
            new TypeDef(DEPT_SUMMARY_PDF,      "Department Summary Report (PDF)",     FMT_PDF,   true),
            new TypeDef(AI_STRATEGY_PDF,       "AI Strategy Insight Report (PDF)",    FMT_PDF,   true),

            new TypeDef(
                    EXPENSE_APPROVED_SUMMARY_PDF,
                    "Approved Expense Summary (PDF)",
                    FMT_PDF,
                    false
            ),
            new TypeDef(
                    EXPENSE_APPROVED_SUMMARY_EXCEL,
                    "Approved Expense Summary (Excel)",
                    FMT_EXCEL,
                    false
            )
    );

    public static TypeDef find(String id) {
        log.warn("[TYPES] find called with='{}'", id);

        return ALL.stream().filter(t -> t.id().equals(id)).findFirst().orElse(null);
    }
}
