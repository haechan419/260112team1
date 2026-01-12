import { useEffect, useMemo, useState } from "react";
import AppLayout from "../components/layout/AppLayout";
import "../styles/report.css";
import { REPORT_TYPES } from "../constants/reportTypes";

export default function ReportAnalyticsPage() {
  // ✅ TODO: 나중에 로그인 유저 정보에서 role 받아오면 됨
  // "EMPLOYEE" | "ADMIN"
  const [role] = useState("ADMIN");
  // Generate 이후에만 Download 활성화
  const [isGenerated, setIsGenerated] = useState(false);

  // ✅ RBAC: role에 따라 보고서 옵션만 다르게
  const visibleReportTypes = useMemo(() => {
    return REPORT_TYPES.filter((rt) => rt.roles.includes(role));
  }, [role]);

  // ✅ RBAC: Data Scope 옵션도 role에 따라 다르게
  const scopeOptions = useMemo(() => {
    return role === "ADMIN" ? ["Department", "All"] : ["My Data"];
  }, [role]);

  const FILTERS = useMemo(
    () => [
      {
        key: "period",
        label: "Period",
        type: "single",
        options: ["2025-01", "2025-02", "2025-03", "2025-04"],
      },
      {
        key: "scope",
        label: "Data Scope",
        type: "single",
        options: scopeOptions,
      },
      {
        key: "category",
        label: "Category",
        type: "multi",
        options: ["All", "Meals", "Supplies", "Taxi", "Other"],
      },
      {
        key: "format",
        label: "Format",
        type: "single",
        options: ["PDF", "Excel"],
      },
    ],
    [scopeOptions]
  );

  // 드롭다운(탭) 오픈 상태
  const [activeKey, setActiveKey] = useState(null);

  // 각 드롭다운 검색어
  const [filterSearch, setFilterSearch] = useState({
    period: "",
    scope: "",
    category: "",
    format: "",
  });

  // 선택값
  const [selected, setSelected] = useState({
    period: ["2025-03"],
    scope: role === "ADMIN" ? ["Department"] : ["My Data"],
    category: ["All"],
    format: ["PDF"],
  });

  // ✅ role 바뀌면 scope 강제 고정(사원)
  useEffect(() => {
    if (role === "EMPLOYEE") {
      setSelected((prev) => ({ ...prev, scope: ["My Data"] }));
    } else {
      // ADMIN 기본값
      setSelected((prev) => ({
        ...prev,
        scope: prev.scope?.length ? prev.scope : ["Department"],
      }));
    }
  }, [role]);

  // 보고서 타입 선택
  const [reportTypeId, setReportTypeId] = useState(
    visibleReportTypes[0]?.id || ""
  );

  // ✅ role/visible list 바뀌면 기본 reportTypeId 재설정
  useEffect(() => {
    if (!visibleReportTypes.length) return;
    setReportTypeId(visibleReportTypes[0].id);
  }, [visibleReportTypes]);

  // B안: 필터 바뀌면 preview 비우기 (Report Type 다시 선택 유도)
  const [applied, setApplied] = useState(null);

  const invalidatePreview = () => {
    setApplied(null);
    setIsGenerated(false); // ✅ Generate 결과 무효화
  };

  const toggleActive = (key) => {
    setActiveKey((prev) => (prev === key ? null : key));
  };

  const selectOption = (key, type, value) => {
    // EMPLOYEE는 scope 고정
    if (role === "EMPLOYEE" && key === "scope") {
      setActiveKey(null);
      return;
    }

    setSelected((prev) => {
      let next;
      if (type === "single") {
        next = { ...prev, [key]: [value] };
      } else {
        const exists = (prev[key] || []).includes(value);
        const nextArr = exists
          ? prev[key].filter((v) => v !== value)
          : [...(prev[key] || []), value];
        next = { ...prev, [key]: nextArr };
      }
      return next;
    });

    invalidatePreview();
    setActiveKey(null); // ✅ 선택 시 사라짐
  };

  const removeChip = (key, value) => {
    // EMPLOYEE는 scope 고정
    if (role === "EMPLOYEE" && key === "scope") return;

    setSelected((prev) => ({
      ...prev,
      [key]: (prev[key] || []).filter((v) => v !== value),
    }));
    invalidatePreview();
  };

  const clearAll = () => {
    setSelected({
      period: [],
      scope: role === "ADMIN" ? ["Department"] : ["My Data"],
      category: [],
      format: [],
    });
    setActiveKey(null);
    setFilterSearch({ period: "", scope: "", category: "", format: "" });
    invalidatePreview();
  };

  const activeFilter = activeKey
    ? FILTERS.find((f) => f.key === activeKey)
    : null;

  const filteredOptions = useMemo(() => {
    if (!activeFilter) return [];
    const q = (filterSearch[activeFilter.key] || "").trim().toLowerCase();
    if (!q) return activeFilter.options;
    return activeFilter.options.filter((opt) =>
      String(opt).toLowerCase().includes(q)
    );
  }, [activeFilter, filterSearch]);

  const handleSelectReportType = (nextReportTypeId) => {
    setReportTypeId(nextReportTypeId);

    // ✅ "이제 저 기반으로 preview 보이게"
    setApplied({
      reportTypeId: nextReportTypeId,
      filtersSnapshot: selected,
    });
  };

  // Preview 데이터
  const previewData = useMemo(() => {
    if (!applied) {
      return {
        isReady: false,
        message: "Report Type 다시 선택하세요",
        reportTypeLabel: "-",
        recordsIncluded: "-",
        totalAmount: "-",
        period: "-",
        scope: "-",
        outputFormat: "-",
      };
    }

    const report =
      visibleReportTypes.find((r) => r.id === applied.reportTypeId) ||
      visibleReportTypes[0];

    const f = applied.filtersSnapshot || {};
    const period = f.period?.[0] || "-";
    const scope = f.scope?.[0] || "-";

    return {
      isReady: true,
      message: "",
      reportTypeLabel: report?.label || "-",
      recordsIncluded: report?.preview?.recordsIncluded ?? "-",
      totalAmount: report?.preview?.totalAmount ?? "-",
      period,
      scope,
      outputFormat: report?.outputFormat || "-",
    };
  }, [applied, visibleReportTypes]);

  const handleGenerate = () => {
    // 여기서 API 호출
    setIsGenerated(true);
  };

  return (
    <AppLayout>
      <div className="report-page">
        <h1 className="page-title">Report &amp; Analytics</h1>

        {/* ===================== Filter Panel ===================== */}
        <section className="section">
          <h2 className="section-title">Filter Panel</h2>

          <div className="panel panel-filter2">
            <div className="filter-tabs">
              {FILTERS.map((f) => {
                const isActive = activeKey === f.key;

                return (
                  <button
                    key={f.key}
                    type="button"
                    className={`filter-tab ${isActive ? "is-active" : ""}`}
                    onClick={() => toggleActive(f.key)}
                  >
                    {f.label} <span className="caret">▾</span>
                  </button>
                );
              })}

              <button type="button" className="filter-clear" onClick={clearAll}>
                Clear Filters
              </button>
            </div>

            {/* Dropdown */}
            {activeFilter && (
              <div className="filter-dropdown">
                <div className="dropdown-inner">
                  <div className="dropdown-search-row">
                    <input
                      className="dropdown-search"
                      placeholder={`Search ${activeFilter.label}...`}
                      value={filterSearch[activeFilter.key] || ""}
                      onChange={(e) =>
                        setFilterSearch((prev) => ({
                          ...prev,
                          [activeFilter.key]: e.target.value,
                        }))
                      }
                    />
                  </div>

                  <div className="dropdown-options">
                    {filteredOptions.length === 0 ? (
                      <div className="dropdown-empty">No results</div>
                    ) : (
                      filteredOptions.map((opt) => {
                        const checked = (selected[activeKey] || []).includes(
                          opt
                        );
                        const inputType =
                          activeFilter.type === "single" ? "radio" : "checkbox";
                        const disabled =
                          role === "EMPLOYEE" && activeKey === "scope";

                        return (
                          <label
                            key={opt}
                            className={`dropdown-item ${
                              disabled ? "is-disabled" : ""
                            }`}
                          >
                            <input
                              type={inputType}
                              name={`dd-${activeKey}`}
                              checked={checked}
                              disabled={disabled}
                              onChange={() =>
                                selectOption(activeKey, activeFilter.type, opt)
                              }
                            />
                            <span className="dropdown-text">{opt}</span>
                          </label>
                        );
                      })
                    )}
                  </div>
                </div>
              </div>
            )}

            {/* Chips */}
            <div className="filter-chips">
              {FILTERS.flatMap((f) =>
                (selected[f.key] || []).map((v) => (
                  <div className="chip" key={`${f.key}:${v}`}>
                    <span className="chip-text">{v}</span>
                    <button
                      type="button"
                      className="chip-x"
                      onClick={() => removeChip(f.key, v)}
                      aria-label="remove"
                    >
                      ×
                    </button>
                  </div>
                ))
              )}
            </div>
          </div>
        </section>

        {/* ===================== Report Type Selection ===================== */}
        <section className="section">
          <h2 className="section-title">Report Type Selection</h2>

          <div className="panel">
            <div className="radio-list">
              {visibleReportTypes.map((rt) => (
                <label key={rt.id} className="radio-row">
                  <input
                    type="radio"
                    name="rtype"
                    checked={reportTypeId === rt.id}
                    onChange={() => handleSelectReportType(rt.id)}
                  />
                  <span>{rt.label}</span>
                </label>
              ))}
            </div>
          </div>
        </section>

        {/* ===================== Report Summary Preview ===================== */}
        <section className="section">
          <h2 className="section-title">Report Summary Preview</h2>

          <div className="panel preview-box">
            <div
              className={`doc-viewer ${
                previewData.isReady ? "is-ready" : "is-empty"
              }`}
            >
              {!previewData.isReady ? (
                <div className="doc-empty">
                  <div>
                    <div className="doc-empty-title">
                      Report Type is not applied
                    </div>
                    <div className="doc-empty-desc">{previewData.message}</div>
                  </div>
                </div>
              ) : (
                <>
                  {previewData.outputFormat === "Excel" ? (
                    <div className="doc-excel">
                      <div className="excel-topbar">
                        <span className="excel-pill">Sheet1</span>
                        <span className="excel-meta">Preview Mode</span>
                      </div>

                      <div className="excel-grid">
                        <div className="excel-corner" />
                        {["A", "B", "C", "D", "E", "F"].map((c) => (
                          <div key={c} className="excel-colhead">
                            {c}
                          </div>
                        ))}

                        {Array.from({ length: 24 }).map((_, r) => (
                          <div key={`row-${r}`} className="excel-row">
                            <div className="excel-rowhead">{r + 1}</div>

                            <div className="excel-cell keycell">
                              {r === 0
                                ? "Report Type"
                                : r === 1
                                ? "Records Included"
                                : r === 2
                                ? "Total Amount"
                                : r === 3
                                ? "Period"
                                : r === 4
                                ? "Scope"
                                : r === 5
                                ? "Output Format"
                                : ""}
                            </div>

                            <div className="excel-cell">
                              {r === 0
                                ? previewData.reportTypeLabel
                                : r === 1
                                ? String(previewData.recordsIncluded)
                                : r === 2
                                ? previewData.totalAmount
                                : r === 3
                                ? previewData.period
                                : r === 4
                                ? previewData.scope
                                : r === 5
                                ? previewData.outputFormat
                                : ""}
                            </div>

                            <div className="excel-cell muted" />
                            <div className="excel-cell muted" />
                            <div className="excel-cell muted" />
                            <div className="excel-cell muted" />
                          </div>
                        ))}
                      </div>
                    </div>
                  ) : (
                    <div className="doc-pdf">
                      <div className="pdf-page">
                        <div className="pdf-header">
                          <div className="pdf-title">Report Summary</div>
                          <div className="pdf-sub">Generated Preview</div>
                        </div>

                        <div className="pdf-body">
                          {[
                            ["Report Type", previewData.reportTypeLabel],
                            ["Records Included", previewData.recordsIncluded],
                            ["Total Amount", previewData.totalAmount],
                            ["Period", previewData.period],
                            ["Scope", previewData.scope],
                            ["Output Format", previewData.outputFormat],
                          ].map(([k, v]) => (
                            <div key={k} className="pdf-row">
                              <span className="k">{k}</span>
                              <span className="v">{v}</span>
                            </div>
                          ))}

                          <div className="pdf-hr" />

                          <div className="pdf-paragraph">
                            This document is a preview representation of the
                            selected report settings. It simulates margins and
                            typography similar to a generated PDF output.
                          </div>
                        </div>

                        <div className="pdf-footer">
                          <span>Confidential</span>
                          <span>Page 1</span>
                        </div>
                      </div>
                    </div>
                  )}
                </>
              )}
            </div>
          </div>
        </section>

        {/* ===================== Actions: Preview 생성되면 등장 ===================== */}
        {previewData.isReady && (
          <section className="section section-actions">
            <div className="actions-row">
              <button type="button" className="action-btn action-secondary">
                Export
              </button>

              <button
                type="button"
                className="action-btn action-primary"
                onClick={handleGenerate}
              >
                Generate Report
              </button>

              <button
                type="button"
                className={`action-btn action-primary2 ${
                  !isGenerated ? "is-disabled" : ""
                }`}
                disabled={!isGenerated}
              >
                Download
              </button>
            </div>

            <div className="actions-hint">
              {isGenerated
                ? "Report has been generated. You can now download the file."
                : "Generate the report before downloading."}
            </div>
          </section>
        )}
      </div>
    </AppLayout>
  );
}
