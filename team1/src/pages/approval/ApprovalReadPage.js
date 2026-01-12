import React from "react";
import {useNavigate, useParams} from "react-router-dom";
import AppLayout from "../../components/layout/AppLayout";
import "../../styles/layout.css";

export default function ApprovalReadPage() {
    const navigate = useNavigate();
    const {id} = useParams(); // URL에서 글 번호(id) 가져오기

    return (
        <AppLayout>
            <div className="report-page">
                <div className="page-meta">전자결재 &gt; 문서 조회</div>
                <h1 className="page-title">결재 상세 내역</h1>

                {/* 1. 상단 액션 버튼 (진수님 전용) */}
                <div
                    className="panel"
                    style={{
                        marginBottom: "24px",
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "center",
                    }}
                >
                    <button
                        className="action-btn action-secondary"
                        onClick={() => navigate(-1)}
                    >
                        ← 뒤로가기
                    </button>

                    <div style={{display: "flex", gap: "12px"}}>
                        <button
                            className="pill-btn"
                            style={{
                                backgroundColor: "#e74c3c",
                                color: "#fff",
                                border: "none",
                            }}
                            onClick={() => alert("반려 처리 하시겠습니까?")}
                        >
                            반려 (Reject)
                        </button>
                        <button
                            className="pill-btn"
                            style={{
                                backgroundColor: "var(--blue)",
                                color: "#fff",
                                border: "none",
                            }}
                            onClick={() => alert("승인 완료 처리!")}
                        >
                            승인 (Approve)
                        </button>
                    </div>
                </div>

                {/* 2. 문서 뷰어 (PDF 느낌) */}
                <div className="doc-pdf">
                    <div className="pdf-page">
                        {/* 문서 헤더 */}
                        <div className="pdf-header">
                            <div className="pdf-title">지출결의서</div>
                            <div className="pdf-sub">문서번호: 2024-EXP-{id}</div>
                        </div>

                        {/* 문서 내용 */}
                        <div className="pdf-body">
                            <div className="pdf-row">
                                <div className="k">기안자</div>
                                <div className="v">김유진 (개발팀)</div>
                            </div>
                            <div className="pdf-row">
                                <div className="k">기안일</div>
                                <div className="v">2024-12-22</div>
                            </div>
                            <div className="pdf-row">
                                <div className="k">제목</div>
                                <div className="v">12월 팀 회식비 결제 요청</div>
                            </div>

                            <div className="pdf-hr"></div>

                            <div className="pdf-paragraph">
                                아래와 같이 법인카드 사용 내역에 대한 지출 결의를 요청합니다.
                                <br/>
                                <br/>
                                1. 일시: 2024년 12월 22일 18:00
                                <br/>
                                2. 장소: 맛있는 고기집 (강남점)
                                <br/>
                                3. 금액: 120,000원
                                <br/>
                                4. 참석자: 개발팀 전원 (5명)
                                <br/>
                            </div>

                            <div className="pdf-hr"></div>

                            {/* 영수증 이미지 영역 (가짜 이미지) */}
                            <div
                                style={{
                                    marginTop: "20px",
                                    padding: "20px",
                                    background: "#f9fafb",
                                    borderRadius: "8px",
                                    textAlign: "center",
                                    border: "1px dashed #ccc",
                                }}
                            >
                                <div
                                    style={{
                                        fontWeight: 700,
                                        color: "var(--muted)",
                                        marginBottom: "10px",
                                    }}
                                >
                                    [영수증 첨부 이미지]
                                </div>
                                <div style={{fontSize: "12px", color: "var(--muted)"}}>
                                    merchant: 맛있는 고기집 / amount: 120,000
                                </div>
                            </div>
                        </div>

                        {/* 문서 푸터 */}
                        <div className="pdf-footer">
                            <div>SmartSpend ERP System</div>
                            <div>위와 같이 결재를 요청합니다.</div>
                        </div>
                    </div>
                </div>
            </div>
        </AppLayout>
    );
}
