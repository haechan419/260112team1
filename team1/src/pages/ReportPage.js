import React from "react";
import AppLayout from "../components/layout/AppLayout";
import "../styles/report.css"; // 방금 만든 CSS 연결

export default function ReportPage() {
    return (
        <AppLayout>
            <div className="report-container mono-font">
                {/* 1. 타이틀 */}
                <h1 className="report-title">Report & Analytics</h1>

                {/* 2. 상단 필터 ([] 괄호 스타일) */}
                <div className="filter-row">
                    <div className="filter-group">
                        <div className="filter-label">[ Period ]</div>
                        <div className="filter-value">
                            2025-03 <span>▼</span>
                        </div>
                    </div>

                    <div className="filter-group">
                        <div className="filter-label">[ Data Scope ]</div>
                        <div className="filter-value">
                            My Data <span>▼</span>
                        </div>
                    </div>

                    <div className="filter-group">
                        <div className="filter-label">[ Category ]</div>
                        <div className="filter-value">
                            All <span>▼</span>
                        </div>
                    </div>

                    <div className="filter-group">
                        <div className="filter-label">[ Format ]</div>
                        <div className="filter-value">
                            PDF <span>▼</span>
                        </div>
                    </div>
                </div>

                {/* 3. 리포트 타입 선택 */}
                <h2 className="section-label">Report Type Selection</h2>
                <div className="type-section">
                    <div className="radio-option">
                        <span className="radio-circle">(●)</span>
                        <span>Personal Detailed Records (Excel)</span>
                    </div>
                    <div className="radio-option" style={{opacity: 0.5}}>
                        <span className="radio-circle">( )</span>
                        <span>
              Personal <span style={{color: "#e57373"}}>Summary</span> Report
              (PDF)
            </span>
                    </div>
                </div>

                {/* 4. 프리뷰 및 버튼 */}
                <h2 className="section-label">Report Summary Preview</h2>

                <div className="preview-section">
                    {/* 왼쪽: 초록색 요약 박스 */}
                    <div className="summary-box">
                        <div className="summary-header">Report Summary</div>

                        <div className="summary-row">
                            <span className="s-key">• Records Included :</span>
                            <span className="s-val">42</span>
                        </div>
                        <div className="summary-row">
                            <span className="s-key">• Total Amount :</span>
                            <span className="s-val">₩1,240,000</span>
                        </div>
                        <div className="summary-row">
                            <span className="s-key">• Period :</span>
                            <span className="s-val">2025-03</span>
                        </div>
                        <div className="summary-row">
                            <span className="s-key">• Scope :</span>
                            <span className="s-val">My Data / Marketing Team</span>
                        </div>
                        <div className="summary-row">
                            <span className="s-key">• Output Format :</span>
                            <span className="s-val highlight">PDF</span>
                        </div>
                    </div>

                    {/* 오른쪽: 검은색 버튼들 */}
                    <div className="button-group">
                        <button className="black-btn">[ Generate Report ]</button>
                        <button className="black-btn">[ Download ]</button>
                    </div>
                </div>
            </div>
        </AppLayout>
    );
}
