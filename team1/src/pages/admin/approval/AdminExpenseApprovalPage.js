import React, { useEffect, useState, useRef } from "react";
import {
  useNavigate,
  useSearchParams,
  createSearchParams,
} from "react-router-dom";
import { getExpenseApprovals } from "../../../api/approvalApi";
import "./AdminExpenseApprovalPage.css";
import AppLayout from "../../../components/layout/AppLayout";

/**
 * 지출 결재 관리 페이지 컴포넌트
 *
 * 관리자가 제출된 지출 내역을 검토하고 승인/반려 처리를 할 수 있는 페이지입니다.
 *
 * @component
 */
const AdminExpenseApprovalPage = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [approvalRequests, setApprovalRequests] = useState([]);
  const [expensesMap, setExpensesMap] = useState({});
  const [hasReceiptMap, setHasReceiptMap] = useState({});
  const [pageResponse, setPageResponse] = useState(null);
  const [loading, setLoading] = useState(false);

  const [currentPage, setCurrentPage] = useState(() => {
    const page = parseInt(searchParams.get("page") || "1", 10);
    return isNaN(page) || page < 1 ? 1 : page;
  });
  const [statusFilter, setStatusFilter] = useState(
    () => searchParams.get("status") || ""
  );
  const [startDate, setStartDate] = useState(
    () => searchParams.get("startDate") || ""
  );
  const [endDate, setEndDate] = useState(
    () => searchParams.get("endDate") || ""
  );

  useEffect(() => {
    const params = new URLSearchParams();
    if (currentPage > 1) params.set("page", currentPage.toString());
    if (statusFilter) params.set("status", statusFilter);
    if (startDate) params.set("startDate", startDate);
    if (endDate) params.set("endDate", endDate);

    const newSearch = params.toString();
    const currentSearch = searchParams.toString();
    if (newSearch !== currentSearch) {
      setSearchParams(params, { replace: true });
    }
  }, [currentPage, statusFilter, startDate, endDate, setSearchParams]);

  const prevFilters = useRef({ statusFilter, startDate, endDate });
  useEffect(() => {
    const prev = prevFilters.current;
    const filtersChanged =
      prev.statusFilter !== statusFilter ||
      prev.startDate !== startDate ||
      prev.endDate !== endDate;

    if (filtersChanged && (statusFilter || startDate || endDate)) {
      setCurrentPage(1);
      prevFilters.current = { statusFilter, startDate, endDate };
    }
  }, [statusFilter, startDate, endDate]);

  useEffect(() => {
    loadApprovalRequests();
  }, [currentPage, statusFilter, startDate, endDate]);

  /**
   * 결재 요청 목록 조회
   *
   * 백엔드에서 이미 expense 정보를 포함하여 반환하므로 개별 API 호출이 불필요합니다.
   * ApprovalServiceImpl에서 getByIds로 한번에 조회하여 포함시킵니다.
   */
  const loadApprovalRequests = async () => {
    setLoading(true);
    try {
      const params = {
        page: currentPage,
        size: 15,
        status: statusFilter || undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
      };
      const response = await getExpenseApprovals(params);
      const requests = (response.content || []).filter(
        (request) => request.statusSnapshot !== "REQUEST_MORE_INFO"
      );
      setApprovalRequests(requests);
      setPageResponse(response);

      const expenses = {};
      const hasReceipt = {};
      for (const request of requests) {
        if (request.refId && request.expense) {
          expenses[request.refId] = request.expense;

          const expenseData = request.expense;
          if (
            expenseData.hasReceipt === true ||
            (expenseData.receiptId && expenseData.receiptId > 0)
          ) {
            hasReceipt[request.refId] = true;
          } else {
            hasReceipt[request.refId] = false;
          }
        } else if (request.refId) {
          hasReceipt[request.refId] = false;
        }
      }
      setExpensesMap(expenses);
      setHasReceiptMap(hasReceipt);
    } catch (error) {
      console.error("결재 목록 조회 실패:", error);

      if (error.response?.status === 403) {
        alert("관리자 권한이 필요합니다.");
        navigate("/receipt/expenses");
        return;
      }

      setApprovalRequests([]);
      setPageResponse(null);
      setExpensesMap({});
      setHasReceiptMap({});
    } finally {
      setLoading(false);
    }
  };

  const handlePageChange = (page) => {
    setCurrentPage(page);
  };

  /**
   * 결재 요청 클릭 핸들러
   *
   * @param {Object} approvalRequest - 클릭된 결재 요청 객체
   */
  const handleApprovalClick = (approvalRequest) => {
    const params = createSearchParams({
      page: currentPage.toString(),
      ...(statusFilter && { status: statusFilter }),
      ...(startDate && { startDate: startDate }),
      ...(endDate && { endDate: endDate }),
    });
    navigate(`/admin/approval/${approvalRequest.id}?${params.toString()}`);
  };

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

  const getStatusClass = (status) => {
    const classMap = {
      DRAFT: "status-draft",
      SUBMITTED: "status-submitted",
      APPROVED: "status-approved",
      REJECTED: "status-rejected",
    };
    return classMap[status || ""] || "";
  };

  return (
    <AppLayout>
      <div className="admin-expense-approval-page">
        <div className="page-header-with-tab">
          <div className="page-title-section">
            <h1 className="page-title">지출 결재 관리</h1>
          </div>
          <p className="page-description">
            제출된 지출 내역을 검토하고 승인/반려 처리를 할 수 있습니다.
          </p>
        </div>

        <div className="filter-section">
          <div className="filter-row">
            <div className="filter-item">
              <label>전자결재 상태</label>
              <select
                className="form-select"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
              >
                <option value="">전체</option>
                <option value="SUBMITTED">상신</option>
                <option value="APPROVED">승인</option>
                <option value="REJECTED">반려</option>
              </select>
            </div>
            <div className="filter-item">
              <label>상신일</label>
              <div className="date-range">
                <input
                  type="date"
                  className="form-input"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                />
                <span className="date-separator">-</span>
                <input
                  type="date"
                  className="form-input"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                />
              </div>
            </div>
          </div>
        </div>

        <div className="table-container">
          {loading ? (
            <div className="loading">로딩 중...</div>
          ) : approvalRequests.length === 0 ? (
            <div className="empty-state">결재 요청이 없습니다.</div>
          ) : (
            <>
              <table className="approval-table">
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
                  {approvalRequests.map((request, index) => {
                    const expense = expensesMap[request.refId];
                    const hasReceipt = hasReceiptMap[request.refId];
                    const uniqueKey =
                      request.id ||
                      `draft-${request.refId}` ||
                      `index-${index}`;
                    return (
                      <tr
                        key={uniqueKey}
                        onClick={() => handleApprovalClick(request)}
                        className="table-row-clickable"
                      >
                        <td>
                          <span
                            className={`status-badge ${getStatusClass(
                              request.statusSnapshot
                            )}`}
                          >
                            {getStatusLabel(request.statusSnapshot)}
                          </span>
                        </td>
                        <td>
                          {request.createdAt
                            ? request.createdAt.split("T")[0]
                            : "-"}
                        </td>
                        <td>{request.requesterName || "-"}</td>
                        <td>{expense?.receiptDate || "-"}</td>
                        <td>{expense?.merchant || "-"}</td>
                        <td className="amount-cell">
                          {expense?.amount
                            ? `${expense.amount.toLocaleString()}원`
                            : "-"}
                        </td>
                        <td>
                          {hasReceipt === true ? (
                            <span className="receipt-badge has-receipt">
                              있음
                            </span>
                          ) : hasReceipt === false ? (
                            <span className="receipt-badge no-receipt">
                              없음
                            </span>
                          ) : (
                            <span className="receipt-badge">-</span>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>

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
                        onClick={() => handlePageChange(page)}
                      >
                        {page}
                      </button>
                    ))}
                  </div>
                )}
            </>
          )}
        </div>
      </div>
    </AppLayout>
  );
};

export default AdminExpenseApprovalPage;
