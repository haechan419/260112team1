import React from "react";
import { useNavigate } from "react-router-dom";
import AppLayout from "../../components/layout/AppLayout";
import "../../styles/layout.css";

export default function ApprovalListPage() {
  const navigate = useNavigate();

  return (
    <AppLayout>
      <div className="report-page">
        <div className="page-meta">전자결재 &gt; 내 문서함</div>
        <h1 className="page-title">결재 요청 목록</h1>

        {/* 1. 상단 필터 & 글쓰기 버튼 */}
        <div
          className="panel"
          style={{
            marginBottom: "24px",
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
          }}
        >
          <div
            className="filter-tabs"
            style={{ borderBottom: "none", padding: 0 }}
          >
            <button className="filter-tab is-active">전체</button>
            <button className="filter-tab">
              대기중 <span style={{ color: "var(--gold)" }}>2</span>
            </button>
            <button className="filter-tab">승인됨</button>
            <button className="filter-tab">반려됨</button>
          </div>

          <button
            className="pill-btn"
            style={{
              backgroundColor: "var(--blue)",
              color: "#fff",
              border: "none",
            }}
            onClick={() => navigate("/approval/write")} // 👈 여기를 이렇게 수정!
          >
            + 새 결재 작성
          </button>
        </div>

        {/* 2. 결재 목록 테이블 */}
        <div className="panel">
          <table className="dashboard-table">
            <thead>
              <tr>
                <th style={{ width: "60px" }}>No</th>
                <th>문서 종류</th>
                <th>제목</th>
                <th>기안일</th>
                <th>상태</th>
                <th style={{ textAlign: "right" }}>관리</th>
              </tr>
            </thead>
            <tbody>
              {/* 임시 데이터 1 */}
              <tr>
                <td>102</td>
                <td>
                  <span className="excel-pill">지출결의서</span>
                </td>
                <td style={{ fontWeight: 700 }}>12월 팀 회식비 결제 요청</td>
                <td>2024-12-22</td>
                <td>
                  <span style={{ color: "var(--gold)", fontWeight: 800 }}>
                    ● 결재 대기
                  </span>
                </td>
                <td style={{ textAlign: "right" }}>
                  <button className="pill-btn small">상세보기</button>
                </td>
              </tr>

              {/* 임시 데이터 2 */}
              <tr>
                <td>101</td>
                <td>
                  <span
                    className="excel-pill"
                    style={{
                      color: "var(--text)",
                      borderColor: "var(--stroke)",
                    }}
                  >
                    비품구매
                  </span>
                </td>
                <td style={{ fontWeight: 700 }}>개발팀 모니터 추가 구매</td>
                <td>2024-12-20</td>
                <td>
                  <span style={{ color: "var(--green)", fontWeight: 800 }}>
                    ● 승인 완료
                  </span>
                </td>
                <td style={{ textAlign: "right" }}>
                  <button className="pill-btn small">상세보기</button>
                </td>
              </tr>

              {/* 임시 데이터 3 */}
              <tr>
                <td>100</td>
                <td>
                  <span className="excel-pill">지출결의서</span>
                </td>
                <td style={{ fontWeight: 700 }}>야근 식대 청구</td>
                <td>2024-12-18</td>
                <td>
                  <span style={{ color: "#e74c3c", fontWeight: 800 }}>
                    ● 반려됨
                  </span>
                </td>
                <td style={{ textAlign: "right" }}>
                  <button
                    className="pill-btn small"
                    onClick={() => navigate("/approval/102")}
                  >
                    상세보기
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </AppLayout>
  );
}
