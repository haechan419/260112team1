import React, { useState, useEffect } from "react";
import AppLayout from "../../components/layout/AppLayout";
import { getRequestList } from "../../api/requestApi";
import "../../styles/history.css";

export default function RequestHistoryPage() {
  const [serverData, setServerData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState("ALL");
  const [expandedId, setExpandedId] = useState(null);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      const data = await getRequestList();
      setServerData(data);
    } catch (error) {
      console.error("내역 조회 실패:", error);
    } finally {
      setLoading(false);
    }
  };

  // ✨ [추가] 상태별 디자인 테마 정의
  const getStatusTheme = (status) => {
    switch (status) {
      case "APPROVED": // 승인 완료 (초록)
        return { border: "#2ecc71", bg: "#f0fff4", badge: "#27ae60" };
      case "REJECTED": // 반려됨 (빨강)
        return { border: "#e74c3c", bg: "#fff5f5", badge: "#c0392b" };
      case "PENDING": // 대기 중 (노랑)
      default:
        return { border: "#f1c40f", bg: "#fffdf0", badge: "#f39c12" };
    }
  };

  const filteredRequests = serverData.filter((req) => {
    const status = req.status || "PENDING";
    return filter === "ALL" ? true : status === filter;
  });

  const stats = {
    total: serverData.length,
    pending: serverData.filter((r) => (r.status || "PENDING") === "PENDING")
      .length,
    approved: serverData.filter((r) => r.status === "APPROVED").length,
    rejected: serverData.filter((r) => r.status === "REJECTED").length,
  };

  const toggleExpand = (id) => {
    setExpandedId((prev) => (prev === id ? null : id));
  };

  return (
    <AppLayout>
      <div className="page-header">
        <h2 className="page-title">📂 구매 신청 내역</h2>
        <p className="text-gray">
          상신한 비품 구매 요청의 진행 상황을 상세하게 확인합니다.
        </p>
      </div>

      <div className="history-container">
        {/* 상단 통계 카드 (기존 유지) */}
        <div className="stats-row">
          <div className="stat-card">
            <div className="stat-label">총 신청 건수</div>
            <div className="stat-value">{stats.total}건</div>
          </div>
          <div className="stat-card pending">
            <div className="stat-label">대기 중</div>
            <div className="stat-value">{stats.pending}건</div>
          </div>
          <div className="stat-card approved">
            <div className="stat-label">승인 완료</div>
            <div className="stat-value">{stats.approved}건</div>
          </div>
          <div className="stat-card rejected">
            <div className="stat-label">반려됨</div>
            <div className="stat-value">{stats.rejected}건</div>
          </div>
        </div>

        {/* 필터 탭 (기존 유지) */}
        <div className="filter-tabs">
          {["ALL", "PENDING", "APPROVED", "REJECTED"].map((status) => (
            <button
              key={status}
              className={`tab-btn ${filter === status ? "active" : ""}`}
              onClick={() => setFilter(status)}
            >
              {status === "ALL"
                ? "전체 보기"
                : status === "PENDING"
                  ? "승인 대기"
                  : status === "APPROVED"
                    ? "승인 완료"
                    : "반려됨"}
            </button>
          ))}
        </div>

        <div className="history-list">
          {loading ? (
            <div
              style={{ textAlign: "center", padding: "50px", color: "#999" }}
            >
              ⏳ 데이터를 불러오는 중입니다...
            </div>
          ) : filteredRequests.length === 0 ? (
            <div className="empty-history">
              <span style={{ fontSize: "40px" }}>📭</span>
              <p>해당하는 요청 내역이 없습니다.</p>
            </div>
          ) : (
            filteredRequests.map((req, index) => {
              const reqId = req.rno || index;
              const reqStatus = req.status || "PENDING";
              const theme = getStatusTheme(reqStatus); // ✨ 테마 적용

              const reqDate = req.regDate ? req.regDate.substring(0, 10) : "-";
              const title =
                req.items && req.items.length > 0
                  ? req.items.length > 1
                    ? `${req.items[0].pname} 외 ${req.items.length - 1}건`
                    : req.items[0].pname
                  : "상품 정보 없음";

              return (
                <div
                  key={reqId}
                  className={`history-card-pro ${expandedId === reqId ? "expanded" : ""
                    }`}
                  // ✨ [핵심 수정] 카드 전체에 색상 입히기
                  style={{
                    borderLeft: `6px solid ${theme.border}`,
                    backgroundColor: theme.bg,
                    marginBottom: "15px",
                    borderRadius: "8px",
                    boxShadow: "0 2px 5px rgba(0,0,0,0.05)",
                  }}
                >
                  <div
                    className="card-header"
                    onClick={() => toggleExpand(reqId)}
                  >
                    <div className="header-left">
                      <div className={`status-dot ${reqStatus}`}></div>
                      <div className="req-date">{reqDate}</div>
                      <div className="req-title" style={{ fontWeight: "bold" }}>
                        {title}
                      </div>
                    </div>
                    <div className="header-right">
                      <div
                        className="req-amount"
                        style={{ fontWeight: "bold" }}
                      >
                        {req.totalAmount ? req.totalAmount.toLocaleString() : 0}
                        원
                      </div>
                      <div
                        className={`status-badge ${reqStatus}`}
                        style={{
                          backgroundColor: "#fff",
                          border: `1px solid ${theme.border}`,
                          color: theme.badge,
                        }}
                      >
                        {reqStatus === "PENDING"
                          ? "결재 대기"
                          : reqStatus === "APPROVED"
                            ? "승인됨"
                            : "반려됨"}
                      </div>
                      <div className="arrow-icon">
                        {expandedId === reqId ? "▲" : "▼"}
                      </div>
                    </div>
                  </div>

                  {expandedId === reqId && (
                    <div
                      className="card-detail"
                      style={{ borderTop: "1px solid rgba(0,0,0,0.05)" }}
                    >
                      {/* 스테퍼 (기존 코드) */}
                      <div className="progress-stepper">
                        <div className={`step completed`}>기안 상신</div>
                        <div className="line completed"></div>
                        <div
                          className={`step ${reqStatus !== "PENDING" ? "completed" : "active"
                            }`}
                        >
                          담당자 확인
                        </div>
                        <div
                          className={`line ${reqStatus !== "PENDING" ? "completed" : ""
                            }`}
                        ></div>
                        <div
                          className={`step ${reqStatus === "APPROVED"
                              ? "completed"
                              : reqStatus === "REJECTED"
                                ? "error"
                                : ""
                            }`}
                        >
                          {reqStatus === "APPROVED"
                            ? "최종 승인"
                            : reqStatus === "REJECTED"
                              ? "반려됨"
                              : "승인 대기"}
                        </div>
                      </div>

                      {/* ✨ 반려 사유 강조 (REJECTED일 때) */}
                      {reqStatus === "REJECTED" && (
                        <div
                          className="reject-alert"
                          style={{
                            backgroundColor: "#fff",
                            border: "1px dashed #e74c3c",
                            color: "#e74c3c",
                            padding: "10px",
                            borderRadius: "5px",
                            marginTop: "15px",
                          }}
                        >
                          <strong>🚨 반려 사유:</strong>{" "}
                          {req.rejectReason ||
                            "상세 사유는 관리자에게 문의하세요."}
                        </div>
                      )}

                      {/* 품목 테이블 및 메모 (기존 코드) */}
                      {req.items && req.items.length > 0 && (
                        <div
                          className="item-table-wrapper"
                          style={{ marginTop: "15px" }}
                        >
                          <table className="item-table">
                            <thead>
                              <tr>
                                <th>품목명</th>
                                <th>수량</th>
                                <th>금액</th>
                              </tr>
                            </thead>
                            <tbody>
                              {req.items.map((item, idx) => (
                                <tr key={idx}>
                                  <td
                                    style={{
                                      textAlign: "left",
                                      paddingLeft: "10px",
                                    }}
                                  >
                                    {item.pname}
                                  </td>
                                  <td>{item.quantity}개</td>
                                  <td>
                                    {(
                                      item.price * item.quantity
                                    ).toLocaleString()}
                                    원
                                  </td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      )}

                      <div
                        className="memo-box"
                        style={{ backgroundColor: "rgba(255,255,255,0.5)" }}
                      >
                        <span className="label">📝 기안 메모:</span>{" "}
                        {req.reason || "없음"}
                      </div>
                    </div>
                  )}
                </div>
              );
            })
          )}
        </div>
      </div>
    </AppLayout>
  );
}
