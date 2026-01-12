export const REPORT_TYPES = [
    {
        id: "EXPENSE_APPROVED_SUMMARY_PDF",
        label: "Approved Expense Summary (PDF)",
        outputFormat: "PDF",
        roles: ["EMPLOYEE", "ADMIN"],
    },
    {
        id: "EXPENSE_APPROVED_SUMMARY_EXCEL",
        label: "Approved Expense Summary (Excel)",
        outputFormat: "EXCEL",
        roles: ["EMPLOYEE", "ADMIN"],
    },

    // ✅ ADMIN 전용(원하면 유지)
    // { id: "DEPT_DETAIL_EXCEL", label: "Department Detailed Records (Excel)", outputFormat: "EXCEL", roles: ["ADMIN"] },
    // { id: "DEPT_SUMMARY_PDF", label: "Department Summary Report (PDF)", outputFormat: "PDF", roles: ["ADMIN"] },
    { id: "AI_STRATEGY_PDF", label: "AI Strategy Insight Report (PDF)", outputFormat: "PDF", roles: ["ADMIN"] },
];
