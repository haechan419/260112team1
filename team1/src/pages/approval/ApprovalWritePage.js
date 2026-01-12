import React, {useState} from "react";
import {useNavigate} from "react-router-dom";
import AppLayout from "../../components/layout/AppLayout";
import "../../styles/layout.css";

export default function ApprovalWritePage() {
    const navigate = useNavigate();
    const [docType, setDocType] = useState("EXPENSE"); // 기본값: 지출결의서

    return (
        <AppLayout>
            <div className="report-page">
                <div className="page-meta">전자결재 &gt; 기안하기</div>
                <h1 className="page-title">새 결재 작성</h1>

                {/* 입력 폼 패널 */}
                <div className="panel" style={{maxWidth: "800px"}}>
                    {/* 1. 문서 종류 선택 */}
                    <div className="section-title">문서 종류</div>
                    <div style={{display: "flex", gap: "12px", marginBottom: "24px"}}>
                        <label
                            className={`radio-row ${docType === "EXPENSE" ? "active" : ""}`}
                            style={{
                                cursor: "pointer",
                                border:
                                    docType === "EXPENSE"
                                        ? "1px solid var(--blue)"
                                        : "1px solid transparent",
                            }}
                        >
                            <input
                                type="radio"
                                name="docType"
                                checked={docType === "EXPENSE"}
                                onChange={() => setDocType("EXPENSE")}
                            />
                            지출결의서 (법인카드/영수증)
                        </label>
                        <label
                            className={`radio-row ${docType === "STORE" ? "active" : ""}`}
                            style={{
                                cursor: "pointer",
                                border:
                                    docType === "STORE"
                                        ? "1px solid var(--blue)"
                                        : "1px solid transparent",
                            }}
                        >
                            <input
                                type="radio"
                                name="docType"
                                checked={docType === "STORE"}
                                onChange={() => setDocType("STORE")}
                            />
                            비품구매신청서
                        </label>
                    </div>

                    {/* 2. 기본 정보 입력 */}
                    <div className="section-title">기본 정보</div>
                    <div style={{display: "grid", gap: "16px", marginBottom: "24px"}}>
                        <input
                            type="text"
                            className="input-box"
                            placeholder="제목을 입력하세요 (예: 12월 팀 회식비 결제 요청)"
                        />
                        <textarea
                            className="input-box"
                            style={{height: "120px", paddingTop: "12px", resize: "none"}}
                            placeholder="결재 요청 사유나 상세 내용을 입력하세요."
                        />
                    </div>

                    {/* 3. 상세 내역 (선택에 따라 다르게 보임) */}
                    <div className="section-title">
                        {docType === "EXPENSE" ? "영수증 첨부 및 내역" : "구매 물품 목록"}
                    </div>
                    <div
                        className="preview-box"
                        style={{
                            background: "#f9fafb",
                            border: "1px dashed var(--stroke)",
                            textAlign: "center",
                            padding: "40px 0",
                            borderRadius: "12px",
                        }}
                    >
            <span style={{color: "var(--muted)", fontWeight: 700}}>
              {docType === "EXPENSE"
                  ? "+ 지출 내역 불러오기 (유진님 파트 연동 예정)"
                  : "+ 장바구니 불러오기 (비품 파트 연동 예정)"}
            </span>
                    </div>

                    {/* 4. 하단 버튼 */}
                    <div
                        className="actions-row"
                        style={{
                            marginTop: "32px",
                            borderTop: "1px solid var(--stroke)",
                            paddingTop: "20px",
                        }}
                    >
                        <button
                            className="action-btn action-secondary"
                            onClick={() => navigate(-1)} // 뒤로가기
                        >
                            취소
                        </button>
                        <button
                            className="action-btn action-primary2"
                            onClick={() => alert("나중에 백엔드로 데이터 전송!")}
                        >
                            결재 상신
                        </button>
                    </div>
                </div>
            </div>
        </AppLayout>
    );
}
