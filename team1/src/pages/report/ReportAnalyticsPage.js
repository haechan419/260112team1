
import { useEffect, useMemo, useState } from "react";
import { useSelector } from "react-redux";

import AppLayout from "../../components/layout/AppLayout";
import "../../styles/report.css";
import { REPORT_TYPES } from "../../constants/reportTypes";
import jwtAxios  from "../../util/jwtUtil";

export default function ReportAnalyticsPage() {
    // ✅ 링크(a href)용: 파일 다운로드를 새 탭으로 열 때만 사용
    // axiosInstance는 baseURL이 /api까지 포함이므로, 앵커는 origin을 별도로 둠
    const API_ORIGIN = "http://localhost:8080";
    const loginState = useSelector((state) => state.loginSlice);

    const role = useMemo(() => {
        const roles = loginState?.roleNames ?? loginState?.roles ?? [];
        const single = loginState?.role ?? loginState?.roleName;

        const upperRoles = (Array.isArray(roles) ? roles : [])
            .map((r) => String(r).toUpperCase());
        const upperSingle = single ? String(single).toUpperCase() : "";

        if (upperRoles.includes("ADMIN") || upperSingle === "ADMIN") return "ADMIN";
        if (upperRoles.includes("EMPLOYEE") || upperSingle === "EMPLOYEE") return "EMPLOYEE";

        // ✅ 혹시 서버가 USER로 주면 EMPLOYEE로 치환(프로젝트 정책상 USER=EMPLOYEE라면)
        if (upperRoles.includes("USER") || upperSingle === "USER") return "EMPLOYEE";

        return "EMPLOYEE"; // 기본값
    }, [loginState]);

    // "누가 요청했는지"는 프론트에서 박지 말고 보통 서버가 JWT/세션에서 뽑는 게 정석
    const requester = useMemo(() => {
        return loginState?.employeeNo ?? loginState?.id ?? null;
    }, [loginState]);

    // ADMIN: /admin/report-schedules
// USER : /report-schedules
    const SCHEDULE_BASE = role === "ADMIN" ? "/admin/report-schedules" : "/report-schedules";
    const [departments, setDepartments] = useState([]);
    const [dept, setDept] = useState(""); // 선택된 부서명


    const [approvedTotal, setApprovedTotal] = useState(null);
    const [approvedCount, setApprovedCount] = useState(null);


    // ✅ 실제로는 로그인 훅/스토어에서 role 받아오면 됨
    // const [role] = useState("ADMIN");

    const SCOPE_MAP = { "My Data": "MY", Department: "DEPT", All: "ALL" };
    const FORMAT_MAP = { PDF: "PDF", Excel: "EXCEL", EXCEL: "EXCEL" };

    const pad2 = (n) => String(n).padStart(2, "0");
    const currentYm = () => {
        const d = new Date();
        return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}`;
    };

    const buildMonthOptions = (monthsBack = 24) => {
        const now = new Date();
        const arr = [];
        for (let i = 0; i < monthsBack; i++) {
            const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
            arr.push(`${d.getFullYear()}-${pad2(d.getMonth() + 1)}`);
        }
        return arr;
    };
    const PERIOD_OPTIONS = buildMonthOptions(24);

    const getDefaultSelected = () => ({
        period: [currentYm()],
        scope: [role === "ADMIN" ? "Department" : "My Data"],
        category: ["All"],
        format: ["PDF"],
    });

    // -------------------------
    // Report state
    // -------------------------
    const [generatedReportId, setGeneratedReportId] = useState(null);
    const [isGenerated, setIsGenerated] = useState(false);
    const [isGenerating, setIsGenerating] = useState(false);

    const [files, setFiles] = useState([]);
    const [isFilesLoading, setIsFilesLoading] = useState(false);

    const [downloadLogs, setDownloadLogs] = useState([]);
    const [isLogsLoading, setIsLogsLoading] = useState(false);
    const [logsMode, setLogsMode] = useState({ type: "REPORT", fileId: null }); // REPORT | FILE

    // -------------------------
    // Schedules (ADMIN)
    // -------------------------
    const [schedules, setSchedules] = useState([]);
    const [isSchedulesLoading, setIsSchedulesLoading] = useState(false);


    const fetchSchedules = async () => {
        try {
            setIsSchedulesLoading(true);
            const res = await jwtAxios.get(SCHEDULE_BASE);
            const data = res.data;
            setSchedules(data?.items ?? (Array.isArray(data) ? data : []));
        } catch (e) {
            console.error(e);
            setSchedules([]);
        } finally {
            setIsSchedulesLoading(false);
        }
    };

    const createSchedule = async (payload) => {
        const res = await jwtAxios.post(SCHEDULE_BASE, payload);
        return res.data;
    };

    const updateSchedule = async (id, payload) => {
        const res = await jwtAxios.put(`${SCHEDULE_BASE}/${id}`, payload);
        return res.data;
    };

    const runScheduleNow = async (id) => {
        const res = await jwtAxios.post(`${SCHEDULE_BASE}/${id}/run`);
        return res.data;
    };


    // -------------------------
    // RBAC options
    // -------------------------
    const visibleReportTypes = useMemo(
        () => REPORT_TYPES.filter((rt) => rt.roles.includes(role)),
        [role]
    );

    const scopeOptions = useMemo(
        () => (role === "ADMIN" ? ["Department", "All"] : ["My Data"]),
        [role]
    );

    useEffect(() => {
        if (role === "EMPLOYEE") setSelected((p) => ({ ...p, scope: ["My Data"] }));
    }, [role]);


    // -------------------------
    // Filter defs (CSS 구조 고정)
    // -------------------------
    const FILTERS = useMemo(
        () => [
            { key: "period", label: "기간", type: "single", options: PERIOD_OPTIONS },
            { key: "scope", label: "범위", type: "single", options: scopeOptions },
            {
                key: "category",
                label: "카데고리",
                type: "multi",
                options: ["All", "Meals", "Supplies", "Taxi", "Other"],
            },
            { key: "format", label: "형식", type: "single", options: ["PDF", "EXCEL"] },
        ],
        [scopeOptions]
    );

    const DEFAULT_SELECTED = {
        period: [currentYm()],
        scope: [role === "ADMIN" ? "Department" : "My Data"],
        category: ["All"],
        format: ["PDF"],
    };

    const [activeKey, setActiveKey] = useState(null);
    const [filterSearch, setFilterSearch] = useState({
        period: "",
        scope: "",
        category: "",
        format: "",
    });

    const [selected, setSelected] = useState(getDefaultSelected());

    useEffect(() => {
        if (role === "ADMIN")   fetchSchedules();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [role]);

    useEffect(() => {
        const isDeptMode = role === "ADMIN" && selected.scope?.[0] === "Department";

        if (!isDeptMode) {
            setDept("");          // ✅ Department 모드 아니면 선택값 제거
            return;
        }

        (async () => {
            try {
                const res = await jwtAxios.get("/admin/departments");
                const list = Array.isArray(res.data) ? res.data : (res.data?.items ?? []);
                setDepartments(list);
            } catch (e) {
                console.error(e);
                setDepartments([]);
            }
        })();
    }, [role, selected.scope]);


    useEffect(() => {
        if (role !== "ADMIN") setSelected((p) => ({ ...p, scope: ["My Data"] }));
    }, [role]);

    // ReportType
    const [reportTypeId, setReportTypeId] = useState(() => visibleReportTypes[0]?.id ?? "");
    useEffect(() => {
        if (!visibleReportTypes.some((r) => r.id === reportTypeId)) {
            setReportTypeId(visibleReportTypes[0]?.id ?? "");
        }
    }, [visibleReportTypes, reportTypeId]);

    // -------------------------
    // Schedule Modal state
    // -------------------------
    const [isScheduleModalOpen, setIsScheduleModalOpen] = useState(false);
    const [editingSchedule, setEditingSchedule] = useState(null); // null=create, object=edit

    const openCreateScheduleModal = () => {
        setEditingSchedule(null);
        setIsScheduleModalOpen(true);
    };
    const openEditScheduleModal = (s) => {
        setEditingSchedule(s);
        setIsScheduleModalOpen(true);
    };
    const closeScheduleModal = () => {
        setIsScheduleModalOpen(false);
        setEditingSchedule(null);
    };

    const handleSubmitSchedule = async (form) => {
        try {
            if (!editingSchedule) {
                await createSchedule(form); // ✅ 사용자 입력 기반 CREATE
            } else {
                await updateSchedule(editingSchedule.id, {
                    name: form.name,
                    reportTypeId: form.reportTypeId,
                    dataScope: form.dataScope,
                    outputFormat: form.outputFormat,
                    periodRule: form.periodRule ?? "CURRENT_MONTH",
                    cronExpr: form.cronExpr,
                    enabled: form.enabled,
                });
            }
            await fetchSchedules();
            closeScheduleModal();
        } catch (e) {
            console.error(e);
            alert(String(e?.response?.data?.message ?? e?.message ?? e));
        }
    };

    const currentType = useMemo(
        () => REPORT_TYPES.find((t) => t.id === reportTypeId),
        [reportTypeId]
    );

    const effectiveFormat = currentType?.outputFormat ?? "PDF"; // fallback


    const handleRunNow = async (id) => {
        try {
            await runScheduleNow(id);
            await fetchSchedules();
            alert("Run Now OK");
        } catch (e) {
            console.error(e);
            alert(String(e?.response?.data?.message ?? e?.message ?? e));
        }
    };

    const handleToggleEnabled = async (s) => {
        try {
            await updateSchedule(s.id, {
                name: s.name,
                reportTypeId: s.reportTypeId,
                dataScope: s.dataScope,
                outputFormat: s.outputFormat,
                periodRule: s.periodRule ?? "CURRENT_MONTH",
                cronExpr: s.cronExpr,
                enabled: !s.isEnabled,
            });
            await fetchSchedules();
        } catch (e) {
            console.error(e);
            alert(String(e?.response?.data?.message ?? e?.message ?? e));
        }
    };

    // -------------------------
    // helpers
    // -------------------------
    const toggleActive = (key) => setActiveKey((p) => (p === key ? null : key));

    const onPickSingle = (key, value) => {
        setSelected((p) => ({ ...p, [key]: [value] }));
        setActiveKey(null);
    };

    const onToggleMulti = (key, value) => {
        setSelected((p) => {
            const cur = p[key] ?? [];
            let next = cur.includes(value) ? cur.filter((v) => v !== value) : [...cur, value];

            if (key === "category") {
                if (value === "All") {
                    next = ["All"];
                } else {
                    next = next.filter((x) => x !== "All");
                    if (next.length === 0) next = ["All"];
                }
            }
            return { ...p, [key]: next };
        });
    };

    const removeChip = (key, value) => {
        setSelected((p) => {
            const cur = p[key] ?? [];
            const next = cur.filter((x) => x !== value);
            const def = FILTERS.find((f) => f.key === key);

            if (def?.type === "single") {
                if (next.length === 0) return { ...p, [key]: DEFAULT_SELECTED[key] ?? [] };
                return { ...p, [key]: next };
            }

            if (def?.type === "multi") {
                if (next.length === 0) return { ...p, [key]: DEFAULT_SELECTED[key] ?? ["All"] };

                if (key === "category") {
                    const cleaned = next.includes("All") && next.length > 1 ? next.filter((x) => x !== "All") : next;
                    return { ...p, [key]: cleaned };
                }
                return { ...p, [key]: next };
            }

            return { ...p, [key]: next };
        });
    };

    const formatBytes = (bytes) => {
        const n = Number(bytes ?? 0);
        if (!n) return "0 B";
        if (n < 1024) return `${n} B`;
        if (n < 1024 * 1024) return `${Math.round(n / 1024)} KB`;
        return `${(n / (1024 * 1024)).toFixed(1)} MB`;
    };

    // -------------------------
    // Preview data (메타만)
    // -------------------------
    const preview = useMemo(() => {
        const rt = visibleReportTypes.find((r) => r.id === reportTypeId);
        const period = selected.period?.[0] ?? "-";
        const scope = selected.scope?.[0] ?? "-";
        const format = rt?.outputFormat ?? selected.format?.[0] ?? "-";
        return {
            reportTypeLabel: rt?.label ?? "-",
            recordsIncluded: rt?.preview?.recordsIncluded ?? "-",
            totalAmount: rt?.preview?.totalAmount ?? "-",
            period,
            scope,
            outputFormat: format,
            viewMode: (FORMAT_MAP[format] ?? "PDF") === "EXCEL" ? "EXCEL" : "PDF",
        };
    }, [visibleReportTypes, reportTypeId, selected]);

    // -------------------------
    // API: Generate
    // -------------------------
    const handleGenerate = async () => {
        try {
            setIsGenerating(true);

            setApprovedTotal(null);
            setApprovedCount(null);

            const uiScope = selected.scope?.[0] ?? (role === "ADMIN" ? "Department" : "My Data");
            const dataScope = SCOPE_MAP[uiScope] ?? (role === "ADMIN" ? "DEPT" : "MY");

            // ✅ ADMIN + DEPT면 부서 선택 강제
            if (role === "ADMIN" && dataScope === "DEPT" && !dept) {
                return alert("부서를 선택하세요.");
            }

            const payload = {
                reportTypeId,
                filters: {
                    period: selected.period?.[0] ?? null,
                    dataScope,

                    category: selected.category?.length ? selected.category : ["ALL"],
                    format: effectiveFormat,

                    // ✅ 여기 박는거임
                    department: role === "ADMIN" && dataScope === "DEPT" ? dept : null,
                },
            };

            console.log("[GEN payload]", payload);

            const res = await jwtAxios.post("/reports/generate", payload);
            const data = res.data;

            setGeneratedReportId(data.reportId);
            setIsGenerated(true);
            setLogsMode({ type: "REPORT", fileId: null });

            // ✅ 추가: 백엔드가 내려준 승인 합계 표시용
            setApprovedTotal(data.approvedTotal ?? null);
            setApprovedCount(data.approvedCount ?? null);
        } catch (e) {
            console.error(e);
            alert("Generate failed");
            setGeneratedReportId(null);
            setIsGenerated(false);
        } finally {
            setIsGenerating(false);
        }
    };

    // -------------------------
    // API: Fetch files
    // -------------------------
    useEffect(() => {
        if (!generatedReportId) {
            setFiles([]);
            return;
        }

        (async () => {
            try {
                setIsFilesLoading(true);
                const res = await jwtAxios.get(`/reports/${generatedReportId}/files`);
                const data = res.data;
                const list = Array.isArray(data) ? data : data.files;
                setFiles(list ?? []);
            } catch (e) {
                console.error(e);
                setFiles([]);
            } finally {
                setIsFilesLoading(false);
            }
        })();
    }, [generatedReportId]);

    // -------------------------
    // Download (대표 / 개별) - axios blob
    // -------------------------
    const downloadBlobFromAxios = (axiosRes, fallbackName) => {
        const cd = axiosRes.headers?.["content-disposition"] || "";
        let filename = fallbackName;

        // Content-Disposition: attachment; filename*=UTF-8''Report_2025-03.pdf
        const m = cd.match(/filename\*\=UTF-8''([^;]+)/i);
        if (m?.[1]) filename = decodeURIComponent(m[1]);
        else {
            const m2 = cd.match(/filename="?([^"]+)"?/i);
            if (m2?.[1]) filename = m2[1];
        }

        const blob = new Blob([axiosRes.data]);
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        a.remove();
        window.URL.revokeObjectURL(url);
    };

    const handleDownload = async () => {
        if (!generatedReportId) return;
        try {
            const res = await jwtAxios.get(`/reports/${generatedReportId}/download`, {
                responseType: "blob",
            });
            downloadBlobFromAxios(res, "report");
            if (role === "ADMIN") fetchLogsByReport(generatedReportId);
        } catch (e) {
            console.error(e);
            alert("Download failed");
        }
    };

    const handleDownloadFile = async (fileId, fileName) => {
        try {
            const res = await jwtAxios.get(`/report-files/${fileId}/download`, {
                responseType: "blob",
            });
            downloadBlobFromAxios(res, fileName || "report");
            if (role === "ADMIN") {
                if (logsMode.type === "FILE") fetchLogsByFile(fileId);
                else if (generatedReportId) fetchLogsByReport(generatedReportId);
            }
        } catch (e) {
            console.error(e);
            alert("Download failed");
        }
    };

    // -------------------------
    // Logs (A/B)
    // -------------------------
    const fetchLogsByReport = async (reportId) => {
        if (!reportId) return;
        try {
            setIsLogsLoading(true);
            setLogsMode({ type: "REPORT", fileId: null });

            const res = await jwtAxios.get(`/reports/${reportId}/downloads`);
            const data = res.data;
            const list = Array.isArray(data)
                ? data
                : data.items ?? data.logs ?? data.result ?? data.data ?? [];
            setDownloadLogs(list ?? []);
        } catch (e) {
            console.error(e);
            setDownloadLogs([]);
        } finally {
            setIsLogsLoading(false);
        }
    };

    const fetchLogsByFile = async (fileId) => {
        if (!fileId) return;
        try {
            setIsLogsLoading(true);
            setLogsMode({ type: "FILE", fileId });

            const res = await jwtAxios.get(`/report-files/${fileId}/downloads`);
            const data = res.data;
            const list = Array.isArray(data)
                ? data
                : data.logs ?? data.items ?? data.result ?? data.data ?? [];
            setDownloadLogs(list ?? []);
        } catch (e) {
            console.error(e);
            setDownloadLogs([]);
        } finally {
            setIsLogsLoading(false);
        }
    };

    // Generate 후 ADMIN이면 기본 로그 로드
    useEffect(() => {
        if (role === "ADMIN" && generatedReportId) fetchLogsByReport(generatedReportId);
        if (!generatedReportId) setDownloadLogs([]);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [generatedReportId, role]);

    // -------------------------
    // render
    // -------------------------
    return (
        <AppLayout>
            <div className="report-page">
                <div className="page-title"> 업무보드 &amp; 분석</div>

                {/* 1) Filter Panel */}
                <section className="section">
                    <div className="section-title">필터 선택</div>

                    <div className="panel panel-filter2">
                        <div className="filter-tabs">
                            {FILTERS.map((f) => (
                                <button
                                    key={f.key}
                                    type="button"
                                    className={`filter-tab ${activeKey === f.key ? "is-active" : ""}`}
                                    onClick={() => toggleActive(f.key)}
                                >
                                    {f.label} <span className="caret">▾</span>
                                </button>
                            ))}
                        </div>

                        {/* chips */}
                        <div className="chips-row" style={{ display: "flex", alignItems: "center", gap: 12 }}>
                            <div className="filter-chips" style={{ display: "flex", gap: 10, flexWrap: "wrap", flex: 1 }}>
                                {FILTERS.flatMap((f) =>
                                        (selected[f.key] ?? []).map((v) => (
                                            <span className="chip" key={`${f.key}-${v}`}>
                      {f.label}: {v}
                                                <button className="chip-x" type="button" onClick={() => removeChip(f.key, v)}>
                        ×
                      </button>
                    </span>
                                        ))
                                )}
                            </div>

                            <button
                                type="button"
                                className="action-btn action-secondary"
                                onClick={() => setSelected(getDefaultSelected())}
                            >
                                초기화하기
                            </button>
                        </div>

                        {/* dropdown */}
                        {activeKey && (
                            <div className="filter-dropdown">
                                <div className="dropdown-inner">
                                    <div className="dropdown-search-row">
                                        <input
                                            className="dropdown-search"
                                            placeholder="Search..."
                                            value={filterSearch[activeKey] || ""}
                                            onChange={(e) => setFilterSearch((p) => ({ ...p, [activeKey]: e.target.value }))}
                                        />
                                    </div>

                                    <div className="dropdown-options">
                                        {(() => {
                                            const def = FILTERS.find((x) => x.key === activeKey);
                                            const keyword = (filterSearch[activeKey] || "").toLowerCase();
                                            const list = (def?.options ?? []).filter((opt) => opt.toLowerCase().includes(keyword));

                                            if (list.length === 0) return <div className="dropdown-empty">No results</div>;

                                            if (def?.type === "single") {
                                                return list.map((opt) => {
                                                    const checked = (selected[def.key]?.[0] ?? "") === opt;
                                                    const isDisabled = def.key === "scope" && role !== "ADMIN" && opt !== "My Data";
                                                    return (
                                                        <label
                                                            key={opt}
                                                            className={`dropdown-item ${isDisabled ? "is-disabled" : ""}`}
                                                            title={isDisabled ? "Employee scope is fixed" : ""}
                                                        >
                                                            <input
                                                                type="radio"
                                                                name={`dd-${def.key}`}
                                                                checked={checked}
                                                                disabled={isDisabled}
                                                                onChange={() => onPickSingle(def.key, opt)}
                                                            />
                                                            {opt}
                                                        </label>
                                                    );
                                                });
                                            }

                                            return list.map((opt) => {
                                                const checked = (selected[def.key] ?? []).includes(opt);
                                                return (
                                                    <label key={opt} className="dropdown-item">
                                                        <input type="checkbox" checked={checked} onChange={() => onToggleMulti(def.key, opt)} />
                                                        {opt}
                                                    </label>
                                                );
                                            });
                                        })()}
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>
                </section>

                {role === "ADMIN" && selected.scope?.[0] === "Department" && (
                    <div className="panel" style={{ marginTop: 10 }}>
                        <div className="hint" style={{ marginBottom: 6 }}>Department</div>

                        <select
                            value={dept}
                            onChange={(e) => setDept(e.target.value)}
                            style={{ width: "100%" }}
                        >
                            <option value="">-- Select Department --</option>
                            {departments.map((d) => (
                                <option key={d} value={d}>{d}</option>
                            ))}
                        </select>
                    </div>
                )}


                {/* 2) Report Type Selection */}
                <section className="section">
                    <div className="section-title">리포트 타입 선택</div>

                    <div className="panel">
                        <div className="radio-list">
                            {visibleReportTypes.map((rt) => (
                                <label className="radio-row" key={rt.id}>
                                    <input
                                        type="radio"
                                        name="reportType"
                                        checked={reportTypeId === rt.id}
                                        onChange={() => setReportTypeId(rt.id)}
                                    />
                                    <span>{rt.label}</span>
                                </label>
                            ))}
                        </div>
                    </div>
                </section>

                {/* 3) Preview */}
                <section className="section">
                    <div className="section-title">보고서 요약  Preview</div>

                    <div className="panel preview-box">
                        <div className="doc-viewer">
                            {preview.viewMode === "PDF" ? (
                                <div className="doc-pdf">
                                    <div className="pdf-page">
                                        <div className="pdf-header">
                                            <div className="pdf-title">Report Summary</div>
                                            <div className="pdf-sub">Preview</div>
                                        </div>

                                        <div className="pdf-body">
                                            {[
                                                ["Report Type", preview.reportTypeLabel],
                                                ["Records Included", preview.recordsIncluded],
                                                ["Total Amount", preview.totalAmount],
                                                ["Period", preview.period],
                                                ["Scope", preview.scope],
                                                ["Output Format", preview.outputFormat],
                                            ].map(([k, v]) => (
                                                <div className="pdf-row" key={k}>
                                                    <div className="k">{k}</div>
                                                    <div className="v">{v}</div>
                                                </div>
                                            ))}

                                            <div className="pdf-hr" />
                                            <div className="pdf-paragraph">
                                                This is a metadata preview (ERP-style verification) before generating the actual document.
                                            </div>
                                        </div>

                                        <div className="pdf-footer">
                                            <span>Confidential</span>
                                            <span>Page 1</span>
                                        </div>
                                    </div>
                                </div>
                            ) : (
                                <div className="doc-excel">
                                    <div className="excel-topbar">
                                        <span className="excel-pill">EXCEL</span>
                                        <span className="excel-meta">Preview grid</span>
                                    </div>

                                    <div className="excel-grid">
                                        <div className="excel-corner" />
                                        {["A", "B", "C", "D", "E", "F"].map((c) => (
                                            <div key={c} className="excel-colhead">
                                                {c}
                                            </div>
                                        ))}

                                        {[1, 2, 3, 4, 5, 6].map((r) => (
                                            <div key={r} className="excel-row">
                                                <div className="excel-rowhead">{r}</div>
                                                <div className="excel-cell keycell">{r === 1 ? "Key" : ""}</div>
                                                <div className="excel-cell">{r === 1 ? "Value" : ""}</div>
                                                <div className="excel-cell muted" />
                                                <div className="excel-cell muted" />
                                                <div className="excel-cell muted" />
                                                <div className="excel-cell muted" />
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>
                </section>

                {/* 4) Actions */}
                <section className="section section-actions">
                    <div className="actions-row">
                        <button type="button" className="action-btn action-secondary">
                            Export
                        </button>

                        <button
                            type="button"
                            className="action-btn action-primary"
                            onClick={handleGenerate}
                            disabled={isGenerating}
                        >
                            {isGenerating ? "Generating..." : "Generate Report"}

                        </button>

                        <button
                            type="button"
                            className={`action-btn action-primary2 ${!isGenerated ? "is-disabled" : ""}`}
                            onClick={handleDownload}
                            disabled={!isGenerated}
                        >
                            Download
                        </button>
                    </div>

                    <div className="actions-hint">{isGenerated ? "Generate 완료. Download 가능." : "Generate 이후 Download 활성화."}</div>
                </section>

                {/* Files */}
                {generatedReportId && (
                    <div className="panel" style={{ marginTop: 12 }}>
                        <div className="hint" style={{ marginBottom: 8 }}>
                            Available files
                        </div>

                        {isFilesLoading ? (
                            <div className="hint">Loading files...</div>
                        ) : files.length === 0 ? (
                            <div className="hint">No files generated yet.</div>
                        ) : (
                            <ul className="fileList">
                                {files.map((f) => (
                                    <li key={f.fileId ?? f.id} className="fileRow">
                                        <div className="fileMeta">
                                            <div className="fileName">{f.fileName}</div>
                                            <div className="fileSub">
                                                {f.fileType} · {formatBytes(f.fileSize)} ·{" "}
                                                {f.createdAt ? new Date(f.createdAt).toLocaleString() : ""}
                                            </div>
                                        </div>



                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>
                )}

                {/* 5) Automation / Schedules (ADMIN only) */}
                {role === "ADMIN" && (
                    <section className="section">
                        <div className="section-title"> 자동화 및 스케줄러</div>

                        <ScheduleModal
                            open={isScheduleModalOpen}
                            onClose={closeScheduleModal}
                            onSubmit={handleSubmitSchedule}
                            initial={editingSchedule}
                            reportTypes={visibleReportTypes}
                        />

                        <div className="panel">
                            <div style={{ display: "flex", justifyContent: "flex-end", gap: 10, marginBottom: 10 }}>
                                <button type="button" className="action-btn action-secondary" onClick={openCreateScheduleModal}>
                                    + 스케줄러 추가하기
                                </button>

                                <button
                                    type="button"
                                    className="action-btn action-secondary"
                                    onClick={fetchSchedules}
                                    disabled={isSchedulesLoading}
                                >
                                    {isSchedulesLoading ? "Refreshing..." : "Refresh"}
                                </button>
                            </div>

                            {isSchedulesLoading ? (
                                <div className="hint">Loading schedules...</div>
                            ) : schedules.length === 0 ? (
                                <div className="hint">No schedules.</div>
                            ) : (
                                <table style={{ width: "100%", borderCollapse: "collapse" }}>
                                    <thead>
                                    <tr style={{ textAlign: "left", opacity: 0.75 }}>
                                        <th style={{ padding: "8px 6px" }}>Name</th>
                                        <th style={{ padding: "8px 6px" }}>Type</th>
                                        <th style={{ padding: "8px 6px" }}>Scope</th>
                                        <th style={{ padding: "8px 6px" }}>Format</th>
                                        <th style={{ padding: "8px 6px" }}>Enabled</th>
                                        <th style={{ padding: "8px 6px" }}>Next Run</th>
                                        <th style={{ padding: "8px 6px" }}>Last Run</th>
                                        <th style={{ padding: "8px 6px" }}>Last Job</th>
                                        <th style={{ padding: "8px 6px" }}>Fail</th>
                                        <th style={{ padding: "8px 6px" }}>Last Error</th>
                                        <th style={{ padding: "8px 6px" }}>Actions</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {schedules.map((s) => (
                                        <tr key={s.id}>
                                            <td style={{ padding: "8px 6px" }}>{s.name}</td>
                                            <td style={{ padding: "8px 6px" }}>{s.reportTypeId}</td>
                                            <td style={{ padding: "8px 6px" }}>{s.dataScope}</td>
                                            <td style={{ padding: "8px 6px" }}>{s.outputFormat}</td>
                                            <td style={{ padding: "8px 6px" }}>{s.isEnabled ? "ON" : "OFF"}</td>
                                            <td style={{ padding: "8px 6px" }}>
                                                {s.nextRunAt ? new Date(s.nextRunAt).toLocaleString() : "-"}
                                            </td>
                                            <td style={{ padding: "8px 6px" }}>
                                                {s.lastRunAt ? new Date(s.lastRunAt).toLocaleString() : "-"}
                                            </td>
                                            <td style={{ padding: "8px 6px" }}>
                                                {s.lastJobId ? (
                                                    <a
                                                        href={`${API_ORIGIN}/api/reports/${s.lastJobId}/download`}
                                                        target="_blank"
                                                        rel="noreferrer"
                                                    >
                                                        download
                                                    </a>
                                                ) : (
                                                    "-"
                                                )}
                                            </td>
                                            <td style={{ padding: "8px 6px" }}>{s.failCount ?? 0}</td>
                                            <td style={{ padding: "8px 6px" }}>{s.lastError ? String(s.lastError).slice(0, 120) : "-"}</td>
                                            <td style={{ padding: "8px 6px", whiteSpace: "nowrap" }}>
                                                <button type="button" className="action-btn action-secondary" onClick={() => handleRunNow(s.id)}>
                                                    Run Now
                                                </button>

                                                <button
                                                    type="button"
                                                    className="action-btn action-secondary"
                                                    style={{ marginLeft: 8 }}
                                                    onClick={() => openEditScheduleModal(s)}
                                                >
                                                    Edit
                                                </button>

                                                <button
                                                    type="button"
                                                    className="action-btn action-secondary"
                                                    style={{ marginLeft: 8 }}
                                                    onClick={() => handleToggleEnabled(s)}
                                                >
                                                    {s.isEnabled ? "Disable" : "Enable"}
                                                </button>
                                            </td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            )}
                        </div>
                    </section>
                )}

                {/* 6) Download History (ADMIN only) */}
                {role === "ADMIN" && (
                    <section className="section">
                        <div className="section-title">Download History</div>

                        <div className="panel">
                            {!generatedReportId ? (
                                <div className="hint">Generate 후 확인 가능</div>
                            ) : isLogsLoading ? (
                                <div className="hint">Loading logs...</div>
                            ) : downloadLogs.length === 0 ? (
                                <div className="hint">No logs yet.</div>
                            ) : (
                                <table style={{ width: "100%", borderCollapse: "collapse" }}>
                                    <thead>
                                    <tr style={{ textAlign: "left", opacity: 0.75 }}>
                                        <th style={{ padding: "8px 6px" }}>ID</th>
                                        <th style={{ padding: "8px 6px" }}>ReportFile ID</th>
                                        <th style={{ padding: "8px 6px" }}>Downloaded By</th>
                                        <th style={{ padding: "8px 6px" }}>Downloaded At</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {downloadLogs.map((log) => {
                                        const id = log.logId ?? log.id;
                                        const fileId = log.fileId ?? log.reportFileId;
                                        const who = log.downloadedBy ?? log.download_by;
                                        const at = log.downloadedAt ?? log.downloaded_at;

                                        return (
                                            <tr key={id ?? `${fileId}-${at}`}>
                                                <td style={{ padding: "8px 6px" }}>{id ?? "-"}</td>
                                                <td style={{ padding: "8px 6px" }}>{fileId ?? "-"}</td>
                                                <td style={{ padding: "8px 6px" }}>{who ?? "-"}</td>
                                                <td style={{ padding: "8px 6px" }}>{at ? new Date(at).toLocaleString() : "-"}</td>
                                            </tr>
                                        );
                                    })}
                                    </tbody>
                                </table>
                            )}

                            {generatedReportId && (
                                <div style={{ marginTop: 12, textAlign: "right" }}>
                                    <button
                                        type="button"
                                        className="action-btn action-secondary"
                                        onClick={() => fetchLogsByReport(generatedReportId)}
                                        disabled={!generatedReportId}
                                    >
                                        View All Logs
                                    </button>

                                    {logsMode.type === "FILE" && (
                                        <button
                                            type="button"
                                            className="action-btn action-secondary"
                                            style={{ marginLeft: 10 }}
                                            onClick={() => fetchLogsByReport(generatedReportId)}
                                        >
                                            Back to Report Logs
                                        </button>
                                    )}
                                </div>
                            )}
                        </div>
                    </section>
                )}
            </div>
        </AppLayout>
    );
}

/**
 * ✅ ScheduleModal (기존 그대로)
 * - CREATE: repeatType/time/daysOfWeek/dayOfMonth 입력
 * - EDIT: cronExpr 직접 편집 + enabled + periodRule
 */
function ScheduleModal({ open, onClose, onSubmit, initial, reportTypes }) {
    const isEdit = !!initial;

    const [name, setName] = useState(initial?.name ?? "");
    const [reportTypeId, setReportTypeId] = useState(
        initial?.reportTypeId ?? (reportTypes?.[0]?.id ?? "DEPT_SUMMARY_PDF")
    );
    const [dataScope, setDataScope] = useState(initial?.dataScope ?? "DEPT");
    const [outputFormat, setOutputFormat] = useState(initial?.outputFormat ?? "PDF");
    const [enabled, setEnabled] = useState(initial?.isEnabled ?? true);

    const [periodRule, setPeriodRule] = useState(initial?.periodRule ?? "CURRENT_MONTH");

    // create 전용
    const [repeatType, setRepeatType] = useState("DAILY");
    const [time, setTime] = useState("09:00");
    const [daysOfWeek, setDaysOfWeek] = useState(["MON"]);
    const [dayOfMonth, setDayOfMonth] = useState(1);

    // edit 전용
    const [cronExpr, setCronExpr] = useState(initial?.cronExpr ?? "0 0 9 * * *");

    useEffect(() => {
        setName(initial?.name ?? "");
        setReportTypeId(initial?.reportTypeId ?? (reportTypes?.[0]?.id ?? "DEPT_SUMMARY_PDF"));
        setDataScope(initial?.dataScope ?? "DEPT");
        setOutputFormat(initial?.outputFormat ?? "PDF");
        setEnabled(initial?.isEnabled ?? true);
        setPeriodRule(initial?.periodRule ?? "CURRENT_MONTH");
        setCronExpr(initial?.cronExpr ?? "0 0 9 * * *");
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [initial, open]);

    const DOW = ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"];

    const toggleDow = (d) => {
        setDaysOfWeek((prev) => (prev.includes(d) ? prev.filter((x) => x !== d) : [...prev, d]));
    };

    const pad2 = (n) => String(n).padStart(2, "0");

    const buildCronFromRepeat = () => {
        const [hh, mm] = time.split(":").map((x) => Number(x));
        const hour = hh;
        const min = mm;

        if (repeatType === "DAILY") return `0 ${min} ${hour} * * *`;
        if (repeatType === "WEEKLY") {
            const dow = daysOfWeek && daysOfWeek.length ? daysOfWeek.join(",") : "MON";
            return `0 ${min} ${hour} * * ${dow}`;
        }
        const dom = Math.min(28, Math.max(1, Number(dayOfMonth || 1)));
        return `0 ${min} ${hour} ${dom} * *`;
    };

    const onClickSubmit = () => {
        if (!name.trim()) return alert("name required");
        if (!reportTypeId) return alert("reportTypeId required");

        if (!isEdit) {
            const payload = {
                name: name.trim(),
                reportTypeId,
                dataScope,
                outputFormat,
                repeatType,
                time: `${pad2(Number(time.split(":")[0]))}:${pad2(Number(time.split(":")[1]))}`,
                daysOfWeek: repeatType === "WEEKLY" ? daysOfWeek : null,
                dayOfMonth: repeatType === "MONTHLY" ? Number(dayOfMonth) : null,
                //  requestedBy: "ADMIN",
            };
            onSubmit(payload);
            return;
        }

        const payload = {
            name: name.trim(),
            reportTypeId,
            dataScope,
            outputFormat,
            periodRule,
            cronExpr: cronExpr.trim(),
            enabled: !!enabled,
        };
        onSubmit(payload);
    };

    if (!open) return null;

    return (
        <div
            style={{
                position: "fixed",
                inset: 0,
                background: "rgba(0,0,0,0.35)",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                zIndex: 9999,
            }}
        >
            <div
                style={{
                    width: 560,
                    background: "#fff",
                    borderRadius: 14,
                    padding: 16,
                    boxShadow: "0 10px 24px rgba(0,0,0,0.12)",
                }}
            >
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                    <div style={{ fontWeight: 800 }}>{isEdit ? "Edit Schedule" : "Create Schedule"}</div>
                    <button className="action-btn action-secondary" onClick={onClose}>
                        Close
                    </button>
                </div>

                <div style={{ display: "grid", gap: 10, marginTop: 12 }}>
                    <label>
                        Name
                        <input value={name} onChange={(e) => setName(e.target.value)} style={{ width: "100%" }} />
                    </label>

                    <label>
                        Report Type (code)
                        <select value={reportTypeId} onChange={(e) => setReportTypeId(e.target.value)} style={{ width: "100%" }}>
                            {(reportTypes ?? []).map((rt) => (
                                <option key={rt.id} value={rt.id}>
                                    {rt.label} ({rt.id})
                                </option>
                            ))}
                        </select>
                    </label>

                    <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
                        <label>
                            Data Scope
                            <select value={dataScope} onChange={(e) => setDataScope(e.target.value)} style={{ width: "100%" }}>
                                <option value="DEPT">DEPT</option>
                                <option value="ALL">ALL</option>
                                <option value="MY">MY</option>
                            </select>
                        </label>

                        <label>
                            Output Format
                            <select value={outputFormat} onChange={(e) => setOutputFormat(e.target.value)} style={{ width: "100%" }}>
                                <option value="PDF">PDF</option>
                                <option value="EXCEL">EXCEL</option>
                            </select>
                        </label>
                    </div>

                    {isEdit ? (
                        <>
                            <label>
                                Period Rule
                                <select value={periodRule} onChange={(e) => setPeriodRule(e.target.value)} style={{ width: "100%" }}>
                                    <option value="CURRENT_MONTH">CURRENT_MONTH</option>
                                    <option value="PREV_MONTH">PREV_MONTH</option>
                                    <option value="YESTERDAY">YESTERDAY</option>
                                    <option value="LAST_7_DAYS">LAST_7_DAYS</option>
                                </select>
                            </label>

                            <label>
                                Cron Expr (sec min hour day month dow)
                                <input value={cronExpr} onChange={(e) => setCronExpr(e.target.value)} style={{ width: "100%" }} />
                            </label>

                            <label style={{ display: "flex", alignItems: "center", gap: 8 }}>
                                <input type="checkbox" checked={enabled} onChange={(e) => setEnabled(e.target.checked)} />
                                Enabled
                            </label>
                        </>
                    ) : (
                        <>
                            <div>
                                Repeat Type
                                <div style={{ display: "flex", gap: 10, marginTop: 6 }}>
                                    {["DAILY", "WEEKLY", "MONTHLY"].map((rt) => (
                                        <label key={rt} style={{ display: "flex", gap: 6, alignItems: "center" }}>
                                            <input type="radio" checked={repeatType === rt} onChange={() => setRepeatType(rt)} />
                                            {rt}
                                        </label>
                                    ))}
                                </div>
                            </div>

                            <label>
                                Time (HH:mm)
                                <input type="time" value={time} onChange={(e) => setTime(e.target.value)} />
                            </label>

                            {repeatType === "WEEKLY" && (
                                <div>
                                    Days of Week
                                    <div style={{ display: "flex", gap: 8, flexWrap: "wrap", marginTop: 6 }}>
                                        {DOW.map((d) => (
                                            <label key={d} style={{ border: "1px solid #ddd", padding: "6px 10px", borderRadius: 12 }}>
                                                <input type="checkbox" checked={daysOfWeek.includes(d)} onChange={() => toggleDow(d)} /> {d}
                                            </label>
                                        ))}
                                    </div>
                                </div>
                            )}

                            {repeatType === "MONTHLY" && (
                                <label>
                                    Day of Month (1~28)
                                    <input
                                        type="number"
                                        min={1}
                                        max={28}
                                        value={dayOfMonth}
                                        onChange={(e) => setDayOfMonth(Number(e.target.value))}
                                    />
                                </label>
                            )}

                            <div style={{ opacity: 0.7, fontSize: 12 }}>
                                Preview cron: <b>{buildCronFromRepeat()}</b>
                            </div>
                        </>
                    )}

                    <div style={{ display: "flex", justifyContent: "flex-end", gap: 10, marginTop: 4 }}>
                        <button className="action-btn action-secondary" onClick={onClose}>
                            Cancel
                        </button>
                        <button className="action-btn action-primary" onClick={onClickSubmit}>
                            {isEdit ? "Save" : "Create"}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}
