import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getReceipts, getReceiptImage, getReceiptExtraction, verifyReceipt } from "../../../api/adminReceiptApi";
import "./AdminFinancePage.css";

const AdminFinancePage = () => {
  const navigate = useNavigate();
  const [receipts, setReceipts] = useState([]);
  const [pageResponse, setPageResponse] = useState(null);
  const [loading, setLoading] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [statusFilter, setStatusFilter] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [selectedReceipt, setSelectedReceipt] = useState(null);
  const [receiptImage, setReceiptImage] = useState(null);
  const [receiptImageError, setReceiptImageError] = useState(null);
  const [extraction, setExtraction] = useState(null);
  const [expense, setExpense] = useState(null);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [showActionModal, setShowActionModal] = useState(false);
  const [actionType, setActionType] = useState(null); // "APPROVE", "REJECT"
  const [actionReason, setActionReason] = useState("");

  useEffect(() => {
    loadReceipts();
  }, [currentPage, statusFilter]);

  const loadReceipts = async () => {
    setLoading(true);
    try {
      const response = await getReceipts({
        page: currentPage,
        size: 15,
        status: statusFilter || undefined,
      });
      // REQUEST_MORE_INFO 상태는 프론트엔드에서 필터링하여 표시하지 않음
      const receiptsList = (response.dtoList || []).filter(
        (receipt) => receipt.status !== "REQUEST_MORE_INFO"
      );
      setReceipts(receiptsList);
      setPageResponse(response);
      if (receiptsList && receiptsList.length > 0) {
        setSelectedReceipt(receiptsList[0]);
      }
    } catch (error) {
      console.error("영수증 목록 조회 실패:", error);
      setReceipts([]);
      setPageResponse(null);
      setSelectedReceipt(null);
    } finally {
      setLoading(false);
    }
  };

  const handleReceiptClick = async (receipt) => {
    setSelectedReceipt(receipt);
    setLoadingDetail(true);
    setReceiptImage(null);
    setReceiptImageError(null);
    
    try {
      // 영수증 이미지 로드 (영수증이 있는 경우만)
      if (receipt.id && receipt.fileUrl) {
        try {
          const imageResponse = await getReceiptImage(receipt.id);
          const blob = new Blob([imageResponse.data], { type: "image/jpeg" });
          const url = URL.createObjectURL(blob);
          setReceiptImage(url);
          setReceiptImageError(null);
        } catch (error) {
          console.error("영수증 이미지 로드 실패:", error);
          setReceiptImage(null);
          setReceiptImageError("이미지를 불러올 수 없습니다.");
        }
      } else {
        // 영수증이 없는 경우
        setReceiptImage(null);
        setReceiptImageError("영수증이 업로드되지 않았습니다.");
      }

      // OCR 추출 결과 조회 (영수증이 있는 경우만)
      if (receipt.id) {
        try {
          const extractionData = await getReceiptExtraction(receipt.id);
          setExtraction(extractionData);
        } catch (error) {
          console.error("OCR 결과 조회 실패:", error);
          setExtraction(null);
        }
      } else {
        setExtraction(null);
      }

      // 지출 내역 조회 (expenseId가 있는 경우)
      if (receipt.expenseId) {
        try {
          // TODO: expenseApi를 사용하여 지출 내역 조회
          // const expenseResponse = await expenseApi.getExpense(receipt.expenseId);
          // setExpense(expenseResponse.data);
        } catch (error) {
          console.error("지출 내역 조회 실패:", error);
        }
      }
    } finally {
      setLoadingDetail(false);
    }
  };

  const handlePageChange = (page) => {
    setCurrentPage(page);
  };

  const handleSearch = () => {
    setCurrentPage(1);
    loadReceipts();
  };

  const getStatusLabel = (status) => {
    // REQUEST_MORE_INFO 상태는 표시하지 않음
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

  // 상태별 버튼 활성화 조건
  const canApprove = selectedReceipt?.status === "SUBMITTED";
  const canReject = selectedReceipt?.status === "SUBMITTED";

  // 액션 모달 열기
  const handleOpenActionModal = (type) => {
    setActionType(type);
    setActionReason("");
    setShowActionModal(true);
  };

  // 액션 모달 닫기
  const handleCloseActionModal = () => {
    setShowActionModal(false);
    setActionType(null);
    setActionReason("");
  };

  // 승인/반려 처리
  const handleActionConfirm = async () => {
    if (!selectedReceipt || !actionType) return;

    // 반려는 사유 필수
    if (actionType === "REJECT" && !actionReason.trim()) {
      alert("사유를 입력해주세요.");
      return;
    }

    try {
      await verifyReceipt(selectedReceipt.id, {
        action: actionType,
        expenseId: selectedReceipt.expenseId, // 영수증이 없는 경우를 위해 expenseId도 전달
        reason: actionReason || undefined,
      });

      // 성공 시 모달 닫기
      handleCloseActionModal();
      
      // 목록 새로고침
      const response = await getReceipts({
        page: currentPage,
        size: 15,
        status: statusFilter || undefined,
      });
      const updatedReceipts = response.data.dtoList || [];
      setReceipts(updatedReceipts);
      setPageResponse(response.data);
      
      // 선택된 영수증 다시 찾아서 로드
      const updatedReceipt = updatedReceipts.find(r => r.id === selectedReceipt.id);
      if (updatedReceipt) {
        await handleReceiptClick(updatedReceipt);
      } else if (updatedReceipts.length > 0) {
        // 선택된 항목이 없으면 첫 번째 항목 선택
        await handleReceiptClick(updatedReceipts[0]);
      }
    } catch (error) {
      console.error("액션 처리 실패:", error);
      alert(`${actionType === "APPROVE" ? "승인" : "반려"} 처리에 실패했습니다.`);
    }
  };

  if (loading) {
    return (
      <div className="admin-finance-page">
        <div className="loading-container">
          <div className="loading">로딩 중...</div>
        </div>
      </div>
    );
  }

  return (
    <div className="admin-finance-page">
      <div className="page-header-with-tab">
        <div className="page-title-section">
          <h1 className="page-title">영수증 관리</h1>
          <button className="close-tab-btn" onClick={() => navigate("/admin")}>
            ×
          </button>
        </div>
        <p className="page-description">
          업로드된 영수증을 검토하고 승인/반려 처리를 할 수 있습니다.
        </p>
      </div>

      {/* Filter Section */}
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
            <label>조회 기간</label>
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
          <div className="filter-actions">
            <button className="btn btn-primary" onClick={handleSearch}>
              조회
            </button>
          </div>
        </div>
      </div>

      {/* Table and Detail Section */}
      <div className="admin-content-wrapper">
        <div className="receipt-table-wrapper">
          {receipts.length === 0 ? (
            <div className="empty-state">
              <p>등록된 영수증이 없습니다.</p>
              <p className="hint">직원이 지출 내역을 등록하고 영수증을 업로드하면 여기에 표시됩니다.</p>
            </div>
          ) : (
            <table className="receipt-table">
              <thead>
                <tr>
                  <th>전자결재 상태</th>
                  <th>지출 ID</th>
                  <th>업로드자</th>
                  <th>업로드 일시</th>
                  <th>금액</th>
                </tr>
              </thead>
              <tbody>
                {receipts.map((receipt) => (
                  <tr
                    key={receipt.id}
                    onClick={() => handleReceiptClick(receipt)}
                    className={selectedReceipt?.id === receipt.id ? "selected" : ""}
                  >
                    <td>
                      <span className={`status-badge ${getStatusClass(receipt.status || "SUBMITTED")}`}>
                        {getStatusLabel(receipt.status || "SUBMITTED")}
                      </span>
                    </td>
                    <td>{receipt.expenseId}</td>
                    <td>{receipt.uploadedByName || "-"}</td>
                    <td>
                      {receipt.createdAt
                        ? new Date(receipt.createdAt).toLocaleString("ko-KR", {
                            year: "numeric",
                            month: "2-digit",
                            day: "2-digit",
                            hour: "2-digit",
                            minute: "2-digit",
                          })
                        : "-"}
                    </td>
                    <td className="amount-cell">-</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {/* Detail Section - 좌우 비교 UI */}
        {selectedReceipt && (
          <div className="receipt-detail-section">
            <div className="detail-header">영수증 검토</div>
            
            {loadingDetail ? (
              <div className="loading-detail">로딩 중...</div>
            ) : (
              <div className="comparison-container">
                {/* 좌측: 영수증 원본 이미지 */}
                <div className="comparison-left">
                  <div className="comparison-title">📄 영수증 원본</div>
                  <div className="receipt-image-container">
                    {receiptImage ? (
                      <img src={receiptImage} alt="영수증 원본" className="receipt-image" />
                    ) : (
                      <div className="no-image">
                        {receiptImageError || "이미지를 불러올 수 없습니다"}
                      </div>
                    )}
                  </div>
                </div>

                {/* 우측: OCR 추출 결과 및 지출 내역 */}
                <div className="comparison-right">
                  <div className="comparison-title">🤖 OCR 추출 결과</div>
                  
                  {extraction ? (
                    <div className="extraction-results">
                      <div className="extraction-info">
                        <span className="info-label">인식 모델:</span>
                        <span className="info-value">{extraction.modelName || "-"}</span>
                      </div>
                      <div className="extraction-info">
                        <span className="info-label">신뢰도:</span>
                        <span className="info-value">
                          {extraction.confidence 
                            ? `${(extraction.confidence * 100).toFixed(1)}%` 
                            : "-"}
                        </span>
                      </div>
                      
                      <div className="extraction-data">
                        <div className="extraction-item">
                          <label>지출 일자</label>
                          <div className={`extraction-value ${expense && expense.receiptDate !== extraction.extractedDate ? 'highlight-diff' : ''}`}>
                            {extraction.extractedDate || "-"}
                          </div>
                        </div>
                        <div className="extraction-item">
                          <label>가맹점명</label>
                          <div className={`extraction-value ${expense && expense.merchant !== extraction.extractedMerchant ? 'highlight-diff' : ''}`}>
                            {extraction.extractedMerchant || "-"}
                          </div>
                        </div>
                        <div className="extraction-item">
                          <label>금액</label>
                          <div className={`extraction-value amount ${expense && expense.amount !== extraction.extractedAmount ? 'highlight-diff' : ''}`}>
                            {extraction.extractedAmount 
                              ? `${extraction.extractedAmount.toLocaleString()}원` 
                              : "-"}
                          </div>
                        </div>
                        <div className="extraction-item">
                          <label>카테고리</label>
                          <div className={`extraction-value ${expense && expense.category !== extraction.extractedCategory ? 'highlight-diff' : ''}`}>
                            {extraction.extractedCategory || "-"}
                          </div>
                        </div>
                      </div>
                    </div>
                  ) : (
                    <div className="no-extraction">
                      OCR 결과가 없습니다.
                    </div>
                  )}

                  {/* 지출 내역 정보 (있는 경우) */}
                  {expense && (
                    <div className="expense-info-section">
                      <div className="comparison-title">📝 등록된 지출 내역</div>
                      <div className="expense-data">
                        <div className="expense-item">
                          <label>지출 일자</label>
                          <div className="expense-value">{expense.receiptDate || "-"}</div>
                        </div>
                        <div className="expense-item">
                          <label>가맹점명</label>
                          <div className="expense-value">{expense.merchant || "-"}</div>
                        </div>
                        <div className="expense-item">
                          <label>금액</label>
                          <div className="expense-value amount">
                            {expense.amount ? `${expense.amount.toLocaleString()}원` : "-"}
                          </div>
                        </div>
                        <div className="expense-item">
                          <label>카테고리</label>
                          <div className="expense-value">{expense.category || "-"}</div>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* 검증 액션 버튼 */}
            <div className="detail-actions">
              <button
                className="btn btn-success"
                disabled={!canApprove}
                onClick={() => handleOpenActionModal("APPROVE")}
                title={!canApprove ? (selectedReceipt?.status === "APPROVED" ? "이미 승인된 항목입니다" : selectedReceipt?.status === "REJECTED" ? "이미 반려된 항목입니다" : "처리할 수 없는 상태입니다") : ""}
              >
                승인
              </button>
              <button
                className="btn btn-danger"
                disabled={!canReject}
                onClick={() => handleOpenActionModal("REJECT")}
                title={!canReject ? (selectedReceipt?.status === "APPROVED" ? "이미 승인된 항목입니다" : selectedReceipt?.status === "REJECTED" ? "이미 반려된 항목입니다" : "처리할 수 없는 상태입니다") : ""}
              >
                반려
              </button>
              <button
                className="btn btn-secondary"
                onClick={() => navigate(`/receipt/receipts/${selectedReceipt.id}`)}
              >
                상세 보기
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Pagination */}
      {pageResponse && (
        <div className="pagination">
          <button
            disabled={!pageResponse.prev}
            onClick={() => handlePageChange(pageResponse.start - 1)}
            className="pagination-btn"
          >
            이전
          </button>
          {pageResponse.pageList.map((page) => (
            <button
              key={page}
              className={`pagination-btn ${page === pageResponse.page ? "active" : ""}`}
              onClick={() => handlePageChange(page)}
            >
              {page}
            </button>
          ))}
          <button
            disabled={!pageResponse.next}
            onClick={() => handlePageChange(pageResponse.end + 1)}
            className="pagination-btn"
          >
            다음
          </button>
        </div>
      )}

      {/* 액션 모달 (승인/반려) */}
      {showActionModal && (
        <div 
          className="fixed top-0 left-0 z-[1055] flex h-full w-full justify-center bg-black bg-opacity-20"
          onClick={handleCloseActionModal}
        >
          <div 
            className="absolute bg-white shadow dark:bg-gray-700 opacity-100 w-1/2 rounded mt-10 mb-10 px-6 min-w-[500px] max-h-[90vh] overflow-y-auto"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="justify-center mt-6 mb-6 text-2xl border-b-4 border-gray-500">
              {actionType === "APPROVE" && "승인 처리"}
              {actionType === "REJECT" && "반려 처리"}
            </div>

            <div className="pt-4 pb-4">
              <div className="mb-4">
                <label className="block text-sm font-medium mb-2">
                  {actionType === "APPROVE" && "승인 사유 (선택)"}
                  {actionType === "REJECT" && "반려 사유 (필수)"}
                </label>
                <textarea
                  className="w-full p-2 border border-gray-300 rounded"
                  rows={4}
                  value={actionReason}
                  onChange={(e) => setActionReason(e.target.value)}
                  placeholder={
                    actionType === "APPROVE" 
                      ? "승인 사유를 입력하세요 (선택사항)"
                      : "반려 사유를 입력하세요 (필수)"
                  }
                />
              </div>

              {actionType === "REJECT" && !actionReason.trim() && (
                <div className="mb-4 p-2 bg-yellow-100 text-yellow-800 rounded text-sm">
                  ⚠️ 사유를 입력해주세요.
                </div>
              )}
            </div>

            <div className="justify-end flex mb-4">
              <button 
                className="rounded bg-gray-500 mt-4 mb-4 px-6 pt-4 pb-4 text-lg text-white mr-2" 
                onClick={handleCloseActionModal}
              >
                취소
              </button>
              <button 
                className="rounded bg-blue-500 mt-4 mb-4 px-6 pt-4 pb-4 text-lg text-white" 
                onClick={handleActionConfirm}
              >
                확인
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminFinancePage;
