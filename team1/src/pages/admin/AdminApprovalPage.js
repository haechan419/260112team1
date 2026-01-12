import React, { useState, useEffect } from "react";
import AppLayout from "../../components/layout/AppLayout";
import { getRequestList, putRequestStatus } from "../../api/requestApi";
import "../../styles/history.css";

export default function AdminApprovalPage() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [expandedId, setExpandedId] = useState(null);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const data = await getRequestList();
      setRequests(data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleStatusChange = async (rno, newStatus) => {
    let rejectReason = "";

    if (newStatus === "REJECTED") {
      const input = window.prompt("반려 사유를 입력해주세요:");
      if (input === null) return;
      if (!input.trim()) return alert("반려 사유는 필수입니다!");
      rejectReason = input;
    } else {
      if (!window.confirm("정말 승인 처리하시겠습니까?")) return;
    }

    try {
      await putRequestStatus(rno, newStatus, rejectReason);
      alert("처리되었습니다.");
      fetchData();
    } catch (err) {
      console.error(err);
      alert("오류가 발생했습니다.");
    }
  };

  const toggleExpand = (id) => {
    setExpandedId((prev) => (prev === id ? null : id));
  };

  const getStatusText = (status) => {
    switch (status) {
      case "PENDING":
        return "승인 대기";
      case "APPROVED":
        return "승인 완료";
      case "REJECTED":
        return "반려됨";
      default:
        return status;
    }
  };

  // ✨ [추가] 상태별 색상 디자인 정의 함수
  const getStatusStyle = (status) => {
    switch (status) {
      case "APPROVED": // 승인 완료 (초록)
        return {
          border: "#2ecc71",
          bg: "#eafaf1",
          badgeColor: "#27ae60",
        };
      case "REJECTED": // 반려됨 (빨강)
        return {
          border: "#e74c3c",
          bg: "#fdedec",
          badgeColor: "#c0392b",
        };
      case "PENDING": // 승인 대기 (주황/노랑)
      default:
        return {
          border: "#f1c40f",
          bg: "#fef9e7",
          badgeColor: "#f39c12",
        };
    }
  };

  return (
    <AppLayout>
      <div className="page-header" style={{ backgroundColor: "#fff0f0" }}>
        <h2 className="page-title" style={{ color: "#d63031" }}>
          🛡️ 관리자 결재 관리
        </h2>
        <p className="text-gray">
          요청된 비품 구매 건을 검토하고 승인하거나 반려합니다.
        </p>
      </div>

      <div className="history-container">
        <div className="history-list">
          {requests.map((req, index) => {
            const reqId = req.rno || index;
            const reqStatus = req.status || "PENDING";
            const reqDate = req.regDate ? req.regDate.substring(0, 10) : "-";

            // ✨ 현재 아이템의 색상 정보 가져오기
            const statusStyle = getStatusStyle(reqStatus);

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
                // ✨ [핵심 수정] 여기에 스타일 적용 (왼쪽 띠 + 배경색)
                style={{
                  borderLeft: `6px solid ${statusStyle.border}`,
                  backgroundColor: statusStyle.bg,
                  marginBottom: "15px", // 카드 간 간격
                  borderRadius: "8px", // 둥근 모서리 보완
                  boxShadow: "0 2px 5px rgba(0,0,0,0.05)", // 살짝 그림자
                }}
              >
                <div
                  className="card-header"
                  onClick={() => toggleExpand(reqId)}
                  style={{ padding: "15px" }} // 패딩 보정
                >
                  <div className="header-left">
                    <span
                      style={{
                        fontWeight: "bold",
                        marginRight: "10px",
                        color: "#555",
                      }}
                    >
                      #{reqId}
                    </span>
                    {/* 기존 점(dot) 대신 텍스트 색상으로 포인트 줘도 됨 */}
                    <div className="req-date" style={{ color: "#888" }}>
                      {reqDate}
                    </div>
                    <div className="req-title" style={{ fontWeight: "bold" }}>
                      {title}
                    </div>
                  </div>

                  <div className="header-right">
                    <div className="req-amount" style={{ fontWeight: "bold" }}>
                      {req.totalAmount?.toLocaleString()}원
                    </div>
                    {/* ✨ 뱃지 스타일도 색상 맞춰서 강화 */}
                    <div
                      className={`status-badge`}
                      style={{
                        backgroundColor: "white",
                        border: `1px solid ${statusStyle.border}`,
                        color: statusStyle.badgeColor,
                        padding: "5px 10px",
                        borderRadius: "20px",
                        fontSize: "12px",
                        fontWeight: "bold",
                        marginLeft: "10px",
                      }}
                    >
                      {getStatusText(reqStatus)}
                    </div>
                  </div>
                </div>

                {expandedId === reqId && (
                  <div
                    className="card-detail"
                    style={{ borderTop: "1px solid rgba(0,0,0,0.05)" }}
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
                            <td>{item.pname}</td>
                            <td>{item.quantity}</td>
                            <td>
                              {(item.price * item.quantity).toLocaleString()}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>

                    <div className="memo-box">
                      <span className="label">📝 기안 메모:</span> {req.reason}
                    </div>

                    {reqStatus === "REJECTED" && (
                      <div className="reject-alert">
                        <strong>🚨 반려 사유:</strong> {req.rejectReason}
                      </div>
                    )}

                    {reqStatus === "PENDING" && (
                      <div
                        style={{
                          marginTop: "20px",
                          display: "flex",
                          gap: "10px",
                          justifyContent: "flex-end",
                        }}
                      >
                        <button
                          onClick={() => handleStatusChange(reqId, "APPROVED")}
                          style={{
                            padding: "10px 20px",
                            backgroundColor: "#2ecc71", // 초록색 좀 더 예쁜걸로 변경
                            color: "white",
                            border: "none",
                            borderRadius: "5px",
                            cursor: "pointer",
                            fontWeight: "bold",
                          }}
                        >
                          ✅ 승인하기
                        </button>
                        <button
                          onClick={() => handleStatusChange(reqId, "REJECTED")}
                          style={{
                            padding: "10px 20px",
                            backgroundColor: "#e74c3c", // 빨간색 좀 더 예쁜걸로 변경
                            color: "white",
                            border: "none",
                            borderRadius: "5px",
                            cursor: "pointer",
                            fontWeight: "bold",
                          }}
                        >
                          ⛔ 반려하기
                        </button>
                      </div>
                    )}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </AppLayout>
  );
}
