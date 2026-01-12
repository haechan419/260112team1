import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useParams, useNavigate, useSearchParams } from "react-router-dom";
import {
    fetchExpense,
    submitExpense,
    deleteExpense,
} from "../../slices/expenseSlice";
import { getApprovalLogs } from "../../api/approvalApi";
import "./ExpenseDetailPage.css";
import AppLayout from "../../components/layout/AppLayout";
import jwtAxios from "../../util/jwtUtil";

const ExpenseDetailPage = () => {
    const dispatch = useDispatch();
    const navigate = useNavigate();
    const { id } = useParams();
    const [searchParams] = useSearchParams();
    const { currentExpense, loading } = useSelector((state) => state.expense);
    const [approvalLogs, setApprovalLogs] = useState([]);
    const [loadingLogs, setLoadingLogs] = useState(false);

    useEffect(() => {
        if (id) {
            dispatch(fetchExpense(parseInt(id)));
        }
    }, [dispatch, id]);

    useEffect(() => {
        if (id && currentExpense) {
            loadApprovalLogs();
        }
    }, [id, currentExpense]);

    const loadApprovalLogs = async () => {
        // DRAFT 상태는 ApprovalRequest가 없으므로 결재 이력이 없음
        if (currentExpense.status === "DRAFT") {
            setApprovalLogs([]);
            setLoadingLogs(false);
            return;
        }

        setLoadingLogs(true);
        try {
            // Expense ID로 ApprovalRequest 찾기 (list API에서 필터링)
            const res = await jwtAxios.get(`/approval-requests/list`, {
                params: { requestType: "EXPENSE", size: 100 },
            });

            // Spring Pageable 응답 구조: content 배열 사용
            const approvalRequests = res.data?.content || res.data?.dtoList || [];

            if (approvalRequests.length > 0) {
                const approvalRequest = approvalRequests.find(
                    (ar) => ar.refId === parseInt(id)
                );

                // ApprovalRequest가 있고 id가 유효한 경우에만 로그 조회
                if (approvalRequest && approvalRequest.id) {
                    const logs = await getApprovalLogs(approvalRequest.id);
                    // logs가 배열인지 확인 (API 응답이 배열로 직접 오는지 확인)
                    const logsArray = Array.isArray(logs)
                        ? logs
                        : logs?.dtoList || logs?.data || [];
                    setApprovalLogs(logsArray);
                } else {
                    setApprovalLogs([]);
                }
            } else {
                setApprovalLogs([]);
            }
        } catch (error) {
            console.error("결재 이력 로드 실패:", error);
            setApprovalLogs([]);
        } finally {
            setLoadingLogs(false);
        }
    };

    const handleSubmit = async () => {
        if (id) {
            try {
                await dispatch(submitExpense({ id: parseInt(id) })).unwrap();
                const queryString = searchParams.toString();
                navigate(`/receipt/expenses${queryString ? `?${queryString}` : ""}`);
            } catch (error) {
                console.error("제출 실패:", error);
                alert("제출에 실패했습니다. 다시 시도해주세요.");
            }
        }
    };

    const handleDelete = () => {
        if (id && window.confirm("정말 삭제하시겠습니까?")) {
            dispatch(deleteExpense(parseInt(id)));
            const queryString = searchParams.toString();
            navigate(`/receipt/expenses${queryString ? `?${queryString}` : ""}`);
        }
    };

    const handleUploadSuccess = () => {
        if (id) {
            dispatch(fetchExpense(parseInt(id)));
        }
    };

    if (loading) {
        return (
            <AppLayout>
                <div className="expense-detail-page">
                    <div className="page-loading-container">
                        <div className="page-loading-spinner"></div>
                        <p className="page-loading-text">지출 정보를 불러오는 중입니다</p>
                    </div>
                </div>
            </AppLayout>
        );
    }

    if (!currentExpense) {
        return (
            <div className="expense-detail-page">
                <div className="card">
                    <p>지출 내역을 찾을 수 없습니다.</p>
                </div>
            </div>
        );
    }

    const getStatusLabel = (status) => {
        const statusMap = {
            DRAFT: "임시저장",
            SUBMITTED: "상신",
            APPROVED: "승인",
            REJECTED: "반려",
        };
        return statusMap[status || ""] || status;
    };

    return (
        <AppLayout>
            <div className="expense-detail-page">
                <div className="page-header-with-tab">
                    <div className="page-title-section">
                        <h1 className="page-title">지출 내역 상세</h1>
                        <button
                            className="close-tab-btn"
                            onClick={() => {
                                const queryString = searchParams.toString();
                                navigate(
                                    `/receipt/expenses${queryString ? `?${queryString}` : ""}`
                                );
                            }}
                        >
                            ×
                        </button>
                    </div>
                </div>

                {currentExpense.status === "DRAFT" && (
                    <div className="detail-actions-bar">
                        <button
                            className="btn btn-outline"
                            onClick={() => {
                                const queryString = searchParams.toString();
                                navigate(
                                    `/receipt/expenses/${id}/edit${
                                        queryString ? `?${queryString}` : ""
                                    }`
                                );
                            }}
                        >
                            수정
                        </button>
                        <button className="btn btn-danger" onClick={handleDelete}>
                            삭제
                        </button>
                        <button className="btn btn-primary" onClick={handleSubmit}>
                            제출
                        </button>
                    </div>
                )}

                <div className="detail-card">
                    <div className="detail-grid">
                        <div className="detail-item">
                            <label>전자결재 상태</label>
                            <span
                                className={`status-badge status-${
                                    currentExpense.status?.toLowerCase().replace("_", "-") || ""
                                }`}
                            >
                {getStatusLabel(currentExpense.status)}
              </span>
                        </div>
                        <div className="detail-item">
                            <label>지출 일자</label>
                            <span>{currentExpense.receiptDate || "-"}</span>
                        </div>
                        <div className="detail-item">
                            <label>가맹점명</label>
                            <span>{currentExpense.merchant || "-"}</span>
                        </div>
                        <div className="detail-item">
                            <label>이용금액</label>
                            <span className="amount-value">
                {currentExpense.amount
                    ? currentExpense.amount.toLocaleString() + "원"
                    : "-"}
              </span>
                        </div>
                        <div className="detail-item">
                            <label>사용용도</label>
                            <span>{currentExpense.category || "-"}</span>
                        </div>
                        <div className="detail-item full-width">
                            <label>상세내용</label>
                            <span>{currentExpense.description || "-"}</span>
                        </div>
                        <div className="detail-item">
                            <label>전자결재 상신일</label>
                            <span>
                {currentExpense.createdAt
                    ? currentExpense.createdAt.split("T")[0]
                    : "-"}
              </span>
                        </div>
                        {currentExpense.status === "APPROVED" && (
                            <div className="detail-item">
                                <label>전자결재 승인일</label>
                                <span>
                  {currentExpense.updatedAt
                      ? currentExpense.updatedAt.split("T")[0]
                      : "-"}
                </span>
                            </div>
                        )}
                        {currentExpense.status === "REJECTED" && (
                            <>
                                <div className="detail-item">
                                    <label>전자결재 반려일</label>
                                    <span>
                    {loadingLogs ? (
                        <span className="loading-skeleton">로딩 중...</span>
                    ) : (
                        (() => {
                            const rejectLog = approvalLogs.find(
                                (log) =>
                                    log.action === "REJECT" || log.action === "reject"
                            );
                            if (rejectLog?.createdAt) {
                                return rejectLog.createdAt.split("T")[0];
                            }
                            if (currentExpense.updatedAt) {
                                return currentExpense.updatedAt.split("T")[0];
                            }
                            return "-";
                        })()
                    )}
                  </span>
                                </div>
                                <div className="detail-item full-width">
                                    <label>반려 사유</label>
                                    <div className="reject-reason-box">
                                        {loadingLogs ? (
                                            <span className="loading-skeleton">로딩 중...</span>
                                        ) : (
                                            (() => {
                                                const rejectLog = approvalLogs.find(
                                                    (log) =>
                                                        log.action === "REJECT" || log.action === "reject"
                                                );
                                                return rejectLog?.message || "-";
                                            })()
                                        )}
                                    </div>
                                </div>
                            </>
                        )}
                    </div>
                </div>

                {/* DRAFT 상태: 영수증이 있으면 보기 버튼만 표시 */}
                {currentExpense.status === "DRAFT" && currentExpense.hasReceipt && (
                    <div className="card">
                        <div style={{ display: "flex", justifyContent: "flex-end" }}>
                            <button
                                className="btn btn-primary"
                                onClick={() =>
                                    navigate(`/receipt/receipts/${currentExpense.receiptId}`)
                                }
                            >
                                영수증 보기
                            </button>
                        </div>
                    </div>
                )}

                {/* DRAFT가 아닌 상태 (상신/반려 등): 영수증이 있으면 보기 버튼만 표시 */}
                {currentExpense.status !== "DRAFT" && currentExpense.hasReceipt && (
                    <div className="card">
                        <div style={{ display: "flex", justifyContent: "flex-end" }}>
                            <button
                                className="btn btn-primary"
                                onClick={() =>
                                    navigate(`/receipt/receipts/${currentExpense.receiptId}`)
                                }
                            >
                                영수증 보기
                            </button>
                        </div>
                    </div>
                )}
            </div>
        </AppLayout>
    );
};

export default ExpenseDetailPage;
