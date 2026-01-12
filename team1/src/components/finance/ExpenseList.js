import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./ExpenseList.css";

/**
 * 지출 내역 목록 컴포넌트
 *
 * @param {Object} props - 컴포넌트 props
 * @param {Array} props.expenses - 지출 내역 배열
 * @param {Object} props.pageResponse - 페이지네이션 응답
 * @param {boolean} props.loading - 로딩 상태
 * @param {Function} props.onExpenseClick - 지출 내역 클릭 핸들러
 * @param {Function} props.onPageChange - 페이지 변경 핸들러
 * @component
 */
const ExpenseList = ({
  expenses,
  pageResponse,
  loading,
  onExpenseClick,
  onPageChange,
}) => {
  const navigate = useNavigate();

  /**
   * 상태 라벨 반환
   *
   * @param {string} status - 승인 상태
   * @returns {string} 상태 라벨
   */
  const getStatusLabel = (status) => {
    if (status === "REQUEST_MORE_INFO") {
      return "";
    }
    const statusMap = {
      DRAFT: "임시저장",
      SUBMITTED: "상신",
      APPROVED: "승인",
      REJECTED: "반려",
    };
    return statusMap[status || ""] || status;
  };

  /**
   * 상태 CSS 클래스 반환
   *
   * @param {string} status - 승인 상태
   * @returns {string} CSS 클래스명
   */
  const getStatusClass = (status) => {
    const classMap = {
      DRAFT: "status-draft",
      SUBMITTED: "status-submitted",
      APPROVED: "status-approved",
      REJECTED: "status-rejected",
    };
    return classMap[status || ""] || "";
  };

  /**
   * 행 클릭 핸들러
   *
   * @param {Object} expense - 클릭된 지출 내역 객체
   */
  const handleRowClick = (expense) => {
    if (onExpenseClick) {
      onExpenseClick(expense);
    }
  };

  if (loading) {
    return (
      <div className="loading-container">
        <div className="loading">로딩 중...</div>
      </div>
    );
  }

  const expensesList = Array.isArray(expenses)
    ? expenses.filter((expense) => expense.status !== "REQUEST_MORE_INFO")
    : [];

  if (expensesList.length === 0) {
    return (
      <div className="empty-state">
        <p>지출 내역이 없습니다.</p>
      </div>
    );
  }

  return (
    <div className="expense-list-container">
      <div className="expense-table-wrapper">
        <table className="expense-table">
          <thead>
            <tr>
              <th>상태</th>
              <th>상신일</th>
              <th>요청자</th>
              <th>지출 일자</th>
              <th>가맹점명</th>
              <th>금액</th>
              <th>영수증</th>
            </tr>
          </thead>
          <tbody>
            {(expensesList || []).map((expense, index) => (
              <tr
                key={expense.id || `expense-${index}`}
                onClick={() => handleRowClick(expense)}
                className="clickable-row"
              >
                <td>
                  <span
                    className={`status-badge ${getStatusClass(expense.status)}`}
                  >
                    {getStatusLabel(expense.status)}
                  </span>
                </td>
                <td>
                  {expense.createdAt ? expense.createdAt.split("T")[0] : "-"}
                </td>
                <td>{expense.userName || "-"}</td>
                <td>{expense.receiptDate || "-"}</td>
                <td>{expense.merchant || "-"}</td>
                <td className="amount-cell">
                  {expense.amount && expense.amount > 0
                    ? `${expense.amount.toLocaleString()}원`
                    : "-"}
                </td>
                <td>
                  {expense.hasReceipt === true ? (
                    <span className="receipt-badge has-receipt">있음</span>
                  ) : expense.hasReceipt === false ? (
                    <span className="receipt-badge no-receipt">없음</span>
                  ) : expense.status === "DRAFT" ? (
                    <button
                      className="btn-upload-receipt"
                      onClick={(e) => {
                        e.stopPropagation();
                        navigate(`/receipt/expenses/${expense.id}`);
                      }}
                    >
                      영수증 업로드
                    </button>
                  ) : (
                    <span className="receipt-badge">-</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {pageResponse &&
        pageResponse.pageNumList &&
        Array.isArray(pageResponse.pageNumList) &&
        pageResponse.pageNumList.length > 0 && (
          <div className="pagination">
            {pageResponse.pageNumList.map((page) => (
              <button
                key={page}
                className={`pagination-btn ${
                  page === pageResponse.page ? "active" : ""
                }`}
                onClick={() => onPageChange(page)}
              >
                {page}
              </button>
            ))}
          </div>
        )}
    </div>
  );
};

export default ExpenseList;
