import React, { useEffect, useState, useRef } from "react";
import { useDispatch, useSelector } from "react-redux";
import {
  useNavigate,
  useSearchParams,
  createSearchParams,
} from "react-router-dom";
import { fetchExpenses } from "../../slices/expenseSlice";
import ExpenseList from "../../components/finance/ExpenseList";
import "./FinanceListPage.css";
import AppLayout from "../../components/layout/AppLayout";

/**
 * 지출 내역 목록 페이지 컴포넌트
 *
 * 사용자의 지출 내역을 조회하고 필터링할 수 있는 페이지입니다.
 * Redux를 통해 상태를 관리하며, URL 쿼리 파라미터와 동기화됩니다.
 *
 * @component
 */
const FinanceListPage = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { expenses, pageResponse, loading } = useSelector(
    (state) => state.expense
  );

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
  // const [expenseType, setExpenseType] = useState("corporate");

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
    dispatch(
      fetchExpenses({
        page: currentPage,
        size: 15,
        status: statusFilter || undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
      })
    );
  }, [dispatch, currentPage, statusFilter, startDate, endDate]);

  useEffect(() => {
    const handleFocus = () => {
      dispatch(
        fetchExpenses({
          page: currentPage,
          size: 15,
          status: statusFilter || undefined,
          startDate: startDate || undefined,
          endDate: endDate || undefined,
        })
      );
    };
    window.addEventListener("focus", handleFocus);
    return () => window.removeEventListener("focus", handleFocus);
  }, [dispatch, currentPage, statusFilter, startDate, endDate]);

  /**
   * 지출 내역 클릭 핸들러
   *
   * @param {Object} expense - 클릭된 지출 내역 객체
   */
  const handleExpenseClick = (expense) => {
    const params = createSearchParams({
      page: currentPage.toString(),
      ...(statusFilter && { status: statusFilter }),
      ...(startDate && { startDate: startDate }),
      ...(endDate && { endDate: endDate }),
    });
    navigate(`/receipt/expenses/${expense.id}?${params.toString()}`);
  };

  /**
   * 페이지 변경 핸들러
   *
   * @param {number} page - 이동할 페이지 번호
   */
  const handlePageChange = (page) => {
    setCurrentPage(page);
  };

  return (
    <AppLayout>
      <div className="finance-list-page">
        <div className="page-header-with-tab">
          <div className="page-title-section">
            <h1 className="page-title">내 지출 내역</h1>
            <div className="header-actions">
              <button
                className="btn btn-primary"
                onClick={() => navigate("/receipt/expenses/new")}
              >
                지출 등록
              </button>
            </div>
          </div>
          <p className="page-description">
            내가 등록한 지출 내역과 전자결재 진행 상태를 확인할 수 있습니다.
          </p>
        </div>

        {/* <div className="expense-type-tabs">
                    <button
                        className={`tab-btn ${expenseType === "corporate" ? "active" : ""}`}
                        onClick={() => setExpenseType("corporate")}
                    >
                        회사 법인카드
                    </button>
                    <button
                        className={`tab-btn ${expenseType === "personal" ? "active" : ""}`}
                        onClick={() => setExpenseType("personal")}
                    >
                        개인경비, 일반영수증
                    </button>
                    <button
                        className={`tab-btn ${expenseType === "fuel" ? "active" : ""}`}
                        onClick={() => setExpenseType("fuel")}
                    >
                        유류비
                    </button>
                </div> */}

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
                <option value="DRAFT">임시저장</option>
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
          <ExpenseList
            expenses={expenses}
            pageResponse={pageResponse}
            loading={loading}
            onExpenseClick={handleExpenseClick}
            onPageChange={handlePageChange}
          />
        </div>
      </div>
    </AppLayout>
  );
};

export default FinanceListPage;
