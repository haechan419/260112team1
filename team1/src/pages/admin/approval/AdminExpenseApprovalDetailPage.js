import React, { useEffect, useState } from "react";
import { useParams, useNavigate, useSearchParams } from "react-router-dom";
import {
  getApprovalRequest,
  getApprovalLogs,
  actionApproval,
} from "../../../api/approvalApi";
import { expenseApi } from "../../../api/expenseApi";
import {
  getReceiptImage,
  getReceiptExtraction,
} from "../../../api/adminReceiptApi";
import { getApprovalRecommendation } from "../../../api/aiApprovalApi";
import ApprovalTimeline from "../../../components/admin/approval/ApprovalTimeline";
import AppLayout from "../../../components/layout/AppLayout";
import "./AdminExpenseApprovalDetailPage.css";

/**
 * 지출 결재 상세 페이지 컴포넌트
 *
 * 관리자가 특정 지출 내역의 상세 정보를 확인하고 승인/반려 처리를 할 수 있는 페이지입니다.
 *
 * @component
 */
const AdminExpenseApprovalDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [approvalRequest, setApprovalRequest] = useState(null);
  const [expense, setExpense] = useState(null);
  const [receipt, setReceipt] = useState(null);
  const [receiptImage, setReceiptImage] = useState(null);
  const [receiptImageError, setReceiptImageError] = useState(null);
  const [extraction, setExtraction] = useState(null);
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showActionModal, setShowActionModal] = useState(false);
  const [actionType, setActionType] = useState(null);
  const [actionReason, setActionReason] = useState("");
  // AI 추천 관련 state
  const [showAiRecommendationModal, setShowAiRecommendationModal] =
    useState(false);
  const [aiRecommendation, setAiRecommendation] = useState(null);
  const [loadingAiRecommendation, setLoadingAiRecommendation] = useState(false);
  const [showRejectReasonInput, setShowRejectReasonInput] = useState(false);
  const [aiModalRejectReason, setAiModalRejectReason] = useState("");

  useEffect(() => {
    if (id) {
      loadApprovalDetail();
    }
  }, [id]);

  /**
   * AI 승인/반려 추천 로드
   * 상신/승인/반려 상태이고 지출 내역이 있을 때 호출
   * 저장된 결과가 있으면 먼저 조회, 없으면 새로 분석
   */
  useEffect(() => {
    if (
      approvalRequest &&
      expense &&
      !loading &&
      !loadingAiRecommendation &&
      (approvalRequest.statusSnapshot === "SUBMITTED" ||
        approvalRequest.statusSnapshot === "APPROVED" ||
        approvalRequest.statusSnapshot === "REJECTED")
    ) {
      // 저장된 AI 추천 결과가 있는지 먼저 확인 (나중에 구현)
      // 지금은 상신 상태일 때만 새로 분석
      if (approvalRequest.statusSnapshot === "SUBMITTED") {
        loadAiRecommendation();
      }
    }
  }, [approvalRequest, expense, extraction, loading]);

  /**
   * 영수증 이미지 로드
   *
   * @param {number} receiptId - 영수증 ID
   */
  const loadReceiptImage = async (receiptId) => {
    if (receiptImage) {
      URL.revokeObjectURL(receiptImage);
      setReceiptImage(null);
    }

    setReceiptImageError(null);

    try {
      const blobData = await getReceiptImage(receiptId);
      const url = URL.createObjectURL(blobData);
      setReceiptImage(url);
      setReceiptImageError(null);
    } catch (error) {
      console.error("영수증 이미지 로드 실패:", error);
      setReceiptImage(null);

      if (error.response?.status === 401) {
        setReceiptImageError("인증 오류가 발생했습니다. 다시 로그인해주세요.");
      } else if (error.response?.status === 403) {
        setReceiptImageError("관리자 권한이 필요합니다.");
      } else if (error.response?.status === 404) {
        setReceiptImageError("영수증 이미지를 찾을 수 없습니다.");
      } else {
        setReceiptImageError("이미지를 불러올 수 없습니다.");
      }
    }
  };

  /**
   * AI 승인/반려 추천 가져오기
   */
  const loadAiRecommendation = async () => {
    if (!expense) return;

    // 모달을 먼저 표시하고 로딩 상태 시작
    setShowAiRecommendationModal(true);
    setLoadingAiRecommendation(true);
    setAiRecommendation(null); // 이전 결과 초기화

    try {
      const expenseData = {
        receiptDate: expense.receiptDate,
        merchant: expense.merchant,
        amount: expense.amount,
        category: expense.category,
        description: expense.description || "",
      };

      const result = await getApprovalRecommendation(expenseData, extraction);
      console.log("[AI 추천] 결과:", result);
      setAiRecommendation(result);

      // 에러가 있어도 모달은 유지 (에러 메시지 표시)
    } catch (error) {
      console.error("[AI 추천] 로드 실패:", error);
      setAiRecommendation({
        error: "AI 분석을 불러올 수 없습니다.",
        recommendation: "REJECT_SUSPECTED",
        confidence: 0.0,
        reason: "AI 서버와의 통신 중 오류가 발생했습니다.",
        riskFactors: [],
        positiveFactors: [],
      });
    } finally {
      setLoadingAiRecommendation(false);
    }
  };

  /**
   * 결재 상세 정보 조회
   */
  const loadApprovalDetail = async () => {
    setLoading(true);
    try {
      const approvalData = await getApprovalRequest(id);
      setApprovalRequest(approvalData);

      if (approvalData.refId) {
        try {
          const expenseResponse = await expenseApi.getExpense(
            approvalData.refId
          );
          setExpense(expenseResponse.data);

          const expenseData = expenseResponse.data;
          if (expenseData.hasReceipt === true || expenseData.receiptId) {
            const receiptId = expenseData.receiptId
              ? parseInt(expenseData.receiptId)
              : null;
            if (receiptId && !isNaN(receiptId)) {
              loadReceiptImage(receiptId);

              try {
                const extractionData = await getReceiptExtraction(receiptId);
                setExtraction(extractionData);
              } catch (error) {
                console.error("OCR 결과 조회 실패:", error);
                setExtraction(null);
              }
            } else {
              setReceiptImage(null);
              setReceiptImageError("영수증 ID가 없습니다.");
            }
          } else {
            setReceiptImage(null);
            setReceiptImageError("영수증이 업로드되지 않았습니다.");
          }
        } catch (error) {
          console.error("지출 내역 조회 실패:", error);
        }
      }

      try {
        const logsData = await getApprovalLogs(id);
        setLogs(logsData || []);
      } catch (error) {
        console.error("결재 로그 조회 실패:", error);
        setLogs([]);
      }
    } catch (error) {
      console.error("결재 상세 조회 실패:", error);
      alert("결재 정보를 불러올 수 없습니다.");
      navigate("/admin/approval");
    } finally {
      setLoading(false);
    }
  };

  /**
   * 결재 처리 확인 핸들러
   */
  const handleActionConfirm = async () => {
    if (!approvalRequest || !actionType) {
      alert("결재 처리 유형을 선택해주세요.");
      return;
    }

    if (!approvalRequest.id) {
      alert(
        "임시저장 상태의 지출 내역은 결재 처리할 수 없습니다. 먼저 제출해주세요."
      );
      return;
    }

    if (actionType === "REJECT" && !actionReason.trim()) {
      alert("사유를 입력해주세요.");
      return;
    }

    try {
      await actionApproval(approvalRequest.id, {
        action: actionType,
        message: actionReason || undefined,
      });

      handleCloseActionModal();

      setReceiptImage(null);
      setReceiptImageError(null);
      setExtraction(null);
      await loadApprovalDetail();
    } catch (error) {
      console.error("액션 처리 실패:", error);
      const errorMessage =
        error.response?.data?.message ||
        error.message ||
        "알 수 없는 오류가 발생했습니다.";
      alert(
        `${
          actionType === "APPROVE" ? "승인" : "반려"
        } 처리에 실패했습니다.\n${errorMessage}`
      );
    }
  };

  /**
   * AI 추천 모달에서 직접 승인/반려 처리
   *
   * @param {string} type - 처리 유형 (APPROVE, REJECT)
   * @param {string} reason - 처리 사유 (선택)
   */
  const handleAiModalAction = async (type, reason = "") => {
    if (!approvalRequest || !approvalRequest.id) {
      alert("결재 처리할 수 없는 상태입니다.");
      return;
    }

    // 반려인 경우 사유 확인
    if (type === "REJECT") {
      // 사유 입력 필드가 표시되지 않은 경우, 표시만 하고 종료
      if (!showRejectReasonInput) {
        setShowRejectReasonInput(true);
        return;
      }

      // 사유 입력 필드가 표시된 상태에서 사유가 없으면 처리 중단
      if (!aiModalRejectReason.trim()) {
        alert("반려 사유를 입력해주세요.");
        return;
      }

      reason = aiModalRejectReason.trim();
    }

    try {
      await actionApproval(approvalRequest.id, {
        action: type,
        message: reason || undefined,
      });

      // 모달 닫기 및 상태 초기화
      setShowAiRecommendationModal(false);
      setShowRejectReasonInput(false);
      setAiModalRejectReason("");
      setReceiptImage(null);
      setReceiptImageError(null);
      setExtraction(null);
      await loadApprovalDetail();
    } catch (error) {
      console.error("액션 처리 실패:", error);
      const errorMessage =
        error.response?.data?.message ||
        error.message ||
        "알 수 없는 오류가 발생했습니다.";
      alert(
        `${
          type === "APPROVE" ? "승인" : "반려"
        } 처리에 실패했습니다.\n${errorMessage}`
      );
    }
  };

  /**
   * AI 추천 모달 닫기 (상태 초기화 포함)
   */
  const handleCloseAiModal = () => {
    setShowAiRecommendationModal(false);
    setShowRejectReasonInput(false);
    setAiModalRejectReason("");
  };

  /**
   * 결재 처리 모달 열기
   *
   * @param {string|null} type - 처리 유형 (APPROVE, REJECT 등)
   */
  const handleOpenActionModal = (type) => {
    setActionType(type || null);
    setActionReason("");
    setShowActionModal(true);
  };

  const handleCloseActionModal = () => {
    setShowActionModal(false);
    setActionType(null);
    setActionReason("");
  };

  /**
   * 상태 라벨 반환
   *
   * @param {string} status - 승인 상태
   * @returns {string} 상태 라벨
   */
  const getStatusLabel = (status) => {
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

  if (loading && !approvalRequest) {
    return (
      <AppLayout>
        <div className="admin-expense-approval-detail-page">
          <div className="page-loading-container">
            <div className="page-loading-spinner"></div>
            <p className="page-loading-text">결재 정보를 불러오는 중입니다</p>
          </div>
        </div>
      </AppLayout>
    );
  }

  if (!approvalRequest) {
    return (
      <AppLayout>
        <div className="admin-expense-approval-detail-page">
          <div className="empty-state">결재 정보를 찾을 수 없습니다.</div>
        </div>
      </AppLayout>
    );
  }

  return (
    <AppLayout>
      <div className="admin-expense-approval-detail-page">
        <div className="page-header-with-tab">
          <div className="page-title-section">
            <h1 className="page-title">지출 결재 상세</h1>
            <button
              className="close-tab-btn"
              onClick={() => {
                const queryString = searchParams.toString();
                navigate(
                  `/admin/approval${queryString ? `?${queryString}` : ""}`
                );
              }}
            >
              ×
            </button>
          </div>
        </div>

        <div className="detail-content">
          <div className="detail-left">
            <div className="detail-card">
              <h2 className="card-title">지출 내역</h2>
              <div className="detail-grid">
                <div className="detail-item">
                  <label>전자결재 상태</label>
                  <span
                    className={`status-badge ${getStatusClass(
                      approvalRequest.statusSnapshot
                    )}`}
                  >
                    {getStatusLabel(approvalRequest.statusSnapshot)}
                  </span>
                </div>
                {expense && (
                  <>
                    <div className="detail-item">
                      <label>지출 일자</label>
                      <span>{expense.receiptDate || "-"}</span>
                    </div>
                    <div className="detail-item">
                      <label>가맹점명</label>
                      <span>{expense.merchant || "-"}</span>
                    </div>
                    <div className="detail-item">
                      <label>이용금액</label>
                      <span className="amount-value">
                        {expense.amount
                          ? expense.amount.toLocaleString() + "원"
                          : "-"}
                      </span>
                    </div>
                    <div className="detail-item">
                      <label>사용용도</label>
                      <span>{expense.category || "-"}</span>
                    </div>
                    <div className="detail-item full-width">
                      <label>상세내용</label>
                      <span>{expense.description || "-"}</span>
                    </div>
                  </>
                )}
                <div className="detail-item">
                  <label>요청자</label>
                  <span>{approvalRequest.requesterName || "-"}</span>
                </div>
                <div className="detail-item">
                  <label>상신일</label>
                  <span>
                    {approvalRequest.createdAt
                      ? approvalRequest.createdAt.split("T")[0]
                      : "-"}
                  </span>
                </div>
              </div>
            </div>

            {expense?.hasReceipt === true ||
            expense?.receiptId ||
            receiptImageError ? (
              <div className="detail-card">
                <h2 className="card-title">영수증 원본</h2>
                <div className="receipt-image-container">
                  {receiptImage ? (
                    <img
                      src={receiptImage}
                      alt="영수증 원본"
                      className="receipt-image"
                      style={{
                        maxWidth: "100%",
                        maxHeight: "none",
                        width: "auto",
                        height: "auto",
                        display: "block",
                      }}
                    />
                  ) : (
                    <div className="no-image">
                      {receiptImageError || "영수증 이미지를 불러오는 중..."}
                    </div>
                  )}
                </div>
              </div>
            ) : null}
          </div>

          <div className="detail-right">
            <div className="detail-card">
              <h2 className="card-title">결재 이력</h2>
              <ApprovalTimeline logs={logs} approvalRequest={approvalRequest} />
            </div>

            {approvalRequest.id &&
            approvalRequest.statusSnapshot === "SUBMITTED" ? (
              <div className="detail-card">
                <h2 className="card-title">결재 처리</h2>
                <button
                  className="btn btn-primary btn-block"
                  onClick={() => handleOpenActionModal(null)}
                >
                  결재 처리하기
                </button>
              </div>
            ) : approvalRequest.statusSnapshot === "DRAFT" ? (
              <div className="detail-card">
                <h2 className="card-title">결재 처리</h2>
                <div className="info-message">
                  <p>임시저장 상태의 지출 내역입니다.</p>
                  <p>결재 처리를 하려면 먼저 제출해주세요.</p>
                </div>
              </div>
            ) : null}
          </div>
        </div>

        {showActionModal && (
          <div className="modal-overlay" onClick={handleCloseActionModal}>
            <div
              className="modal-content approval-modal"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="modal-header">
                <h3 className="modal-title">결재 처리</h3>
                <button
                  className="modal-close-btn"
                  onClick={handleCloseActionModal}
                >
                  ×
                </button>
              </div>

              <div className="modal-body">
                <div className="approval-document-info">
                  <div className="info-row">
                    <span className="info-label">결재 문서명:</span>
                    <span className="info-value">
                      {expense?.merchant || "지출 내역"}
                    </span>
                  </div>
                  <div className="info-row">
                    <span className="info-label">요청자:</span>
                    <span className="info-value">
                      {approvalRequest?.requesterName || "-"}
                    </span>
                  </div>
                  <div className="info-row">
                    <span className="info-label">상신일:</span>
                    <span className="info-value">
                      {approvalRequest?.createdAt
                        ? approvalRequest.createdAt.split("T")[0]
                        : "-"}
                    </span>
                  </div>
                </div>

                <div className="form-group">
                  <label className="form-label">결재 처리 유형</label>
                  <div className="radio-group">
                    <label className="radio-label">
                      <input
                        type="radio"
                        name="actionType"
                        value="APPROVE"
                        checked={actionType === "APPROVE"}
                        onChange={(e) => setActionType(e.target.value)}
                      />
                      <span className="radio-text">승인</span>
                    </label>
                    <label className="radio-label">
                      <input
                        type="radio"
                        name="actionType"
                        value="REJECT"
                        checked={actionType === "REJECT"}
                        onChange={(e) => setActionType(e.target.value)}
                      />
                      <span className="radio-text">반려</span>
                    </label>
                  </div>

                  <div className="approval-guideline">
                    <button
                      type="button"
                      className="guideline-toggle"
                      onClick={() => {
                        const guideline =
                          document.querySelector(".guideline-content");
                        if (guideline) {
                          guideline.style.display =
                            guideline.style.display === "none"
                              ? "block"
                              : "none";
                        }
                      }}
                    >
                      📋 기준 가이드라인 보기
                    </button>
                    <div
                      className="guideline-content"
                      style={{ display: "none" }}
                    >
                      <div className="guideline-section">
                        <h4>❌ 반려 (REJECTED)</h4>
                        <ul>
                          <li>명백한 규정 위반 (개인 용도 지출 등)</li>
                          <li>허위/조작 의심이 명확한 경우</li>
                          <li>예산 초과로 인한 불가피한 반려</li>
                          <li>회사 정책상 승인 불가능한 지출</li>
                        </ul>
                      </div>
                    </div>
                  </div>
                </div>

                <div className="form-group">
                  <label className="form-label">
                    결재 의견
                    {actionType === "REJECT" && (
                      <span className="required"> *</span>
                    )}
                  </label>
                  <textarea
                    className="form-textarea"
                    value={actionReason}
                    onChange={(e) => setActionReason(e.target.value)}
                    placeholder={
                      actionType === "APPROVE"
                        ? "의견을 입력하세요 (선택사항)"
                        : "반려 사유를 입력하세요 (필수)"
                    }
                    rows={5}
                  />
                  {actionType === "REJECT" && (
                    <div className="form-hint">
                      * 반려 시 사유 입력이 필수입니다.
                    </div>
                  )}
                </div>
              </div>

              <div className="modal-actions">
                <button
                  className="btn btn-secondary"
                  onClick={handleCloseActionModal}
                >
                  취소
                </button>
                <button
                  className={`btn ${
                    !actionType
                      ? "btn-secondary"
                      : actionType === "APPROVE"
                      ? "btn-success"
                      : actionType === "REJECT"
                      ? "btn-danger"
                      : "btn-warning"
                  }`}
                  onClick={handleActionConfirm}
                  disabled={
                    !actionType ||
                    (actionType === "REJECT" && !actionReason.trim())
                  }
                >
                  처리하기
                </button>
              </div>
            </div>
          </div>
        )}

        {/* AI 결재 추천 모달 */}
        {showAiRecommendationModal && (
          <div
            className="modal-overlay ai-recommendation-overlay"
            onClick={handleCloseAiModal}
          >
            <div
              className="modal-content ai-recommendation-modal"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="modal-header">
                <div className="modal-title-with-icon">
                  <h3 className="modal-title">AI 결재 분석</h3>
                </div>
                <button
                  className="modal-close-btn"
                  onClick={handleCloseAiModal}
                  aria-label="닫기"
                >
                  ×
                </button>
              </div>

              <div className="modal-body">
                {loadingAiRecommendation ? (
                  <div className="ai-loading-container">
                    <div className="ai-loading-spinner"></div>
                    <p className="ai-loading-text">
                      AI가 지출 내역을 분석하고 있습니다...
                    </p>
                    <p className="ai-loading-subtext">잠시만 기다려주세요</p>
                    {/* 진행 단계 표시 */}
                    <div className="ai-loading-steps">
                      <div className="loading-step active">
                        <span className="step-icon"></span>
                        <span>지출 정보 분석 중...</span>
                      </div>
                      <div className="loading-step">
                        <span className="step-icon"></span>
                        <span>영수증 검증 중...</span>
                      </div>
                      <div className="loading-step">
                        <span className="step-icon"></span>
                        <span>AI 판단 생성 중...</span>
                      </div>
                    </div>
                  </div>
                ) : aiRecommendation?.error ? (
                  <div className="ai-error-container">
                    <div className="ai-error-icon"></div>
                    <p className="ai-error-message">{aiRecommendation.error}</p>
                  </div>
                ) : aiRecommendation ? (
                  <div className="ai-recommendation-content">
                    {/* 추천 결과 배지 */}
                    {(() => {
                      const rec =
                        aiRecommendation.recommendation?.toUpperCase();
                      const recLower =
                        aiRecommendation.recommendation?.toLowerCase() ||
                        "unknown";

                      let badgeContent;
                      if (rec === "APPROVE") {
                        badgeContent = (
                          <span className="badge-text">승인 권장</span>
                        );
                      } else if (rec === "REJECT_CLEAR" || rec === "REJECT") {
                        // REJECT는 REJECT_CLEAR로 처리 (하위 호환성)
                        badgeContent = (
                          <span className="badge-text">반려 권장</span>
                        );
                      } else if (rec === "REJECT_SUSPECTED") {
                        badgeContent = (
                          <span className="badge-text">반려 검토 필요</span>
                        );
                      } else {
                        badgeContent = (
                          <span className="badge-text">
                            {aiRecommendation.recommendation || "알 수 없음"}
                          </span>
                        );
                      }

                      // REJECT는 reject_clear로 매핑
                      const className =
                        rec === "REJECT" ? "reject_clear" : recLower;
                      return (
                        <div
                          className={`recommendation-badge recommendation-${className}`}
                        >
                          {badgeContent}
                        </div>
                      );
                    })()}

                    {/* 신뢰도 표시 */}
                    <div className="recommendation-confidence">
                      <div className="confidence-label">AI 분석 신뢰도</div>
                      <div className="confidence-bar-container">
                        <div
                          className="confidence-bar"
                          style={{
                            width: `${
                              (aiRecommendation.confidence || 0) * 100
                            }%`,
                          }}
                        ></div>
                      </div>
                      <div className="confidence-value">
                        {((aiRecommendation.confidence || 0) * 100).toFixed(1)}%
                      </div>
                    </div>

                    {/* 검토 근거 */}
                    <div className="recommendation-reason">
                      <h4 className="section-title">검토 근거</h4>
                      <p className="reason-text">{aiRecommendation.reason}</p>
                    </div>

                    {/* 긍정 요인 */}
                    {aiRecommendation.positiveFactors?.length > 0 && (
                      <div className="positive-factors">
                        <h4 className="section-title">긍정 요인</h4>
                        <ul className="factors-list">
                          {aiRecommendation.positiveFactors.map(
                            (factor, idx) => (
                              <li key={idx} className="factor-item positive">
                                {factor}
                              </li>
                            )
                          )}
                        </ul>
                      </div>
                    )}

                    {/* 위험 요인 */}
                    {aiRecommendation.riskFactors?.length > 0 && (
                      <div className="risk-factors">
                        <h4 className="section-title">위험 요인</h4>
                        <ul className="factors-list">
                          {aiRecommendation.riskFactors.map((factor, idx) => (
                            <li key={idx} className="factor-item risk">
                              {factor}
                            </li>
                          ))}
                        </ul>
                      </div>
                    )}

                    {/* 안내 메시지 */}
                    <div className="ai-recommendation-notice">
                      <p>
                        AI 추천은 참고용이며, 최종 결정은 관리자의 판단에
                        따릅니다.
                      </p>
                    </div>
                  </div>
                ) : (
                  <div className="ai-placeholder">
                    <p>AI 분석 결과를 불러올 수 없습니다.</p>
                  </div>
                )}
              </div>

              <div className="modal-actions">
                {/* 상신 상태일 때만 승인/반려 버튼 표시 */}
                {approvalRequest?.statusSnapshot === "SUBMITTED" &&
                !loadingAiRecommendation ? (
                  <div className="ai-modal-action-buttons">
                    {/* 반려 사유 입력 필드 (반려하기 클릭 시 표시) */}
                    {showRejectReasonInput && (
                      <div className="ai-reject-reason-input-container">
                        <label htmlFor="ai-reject-reason">반려 사유</label>
                        <textarea
                          id="ai-reject-reason"
                          className="ai-reject-reason-textarea"
                          placeholder="반려 사유를 입력해주세요."
                          value={aiModalRejectReason}
                          onChange={(e) =>
                            setAiModalRejectReason(e.target.value)
                          }
                          rows={3}
                        />
                        <div className="ai-reject-reason-actions">
                          <button
                            className="btn btn-reject"
                            onClick={() => handleAiModalAction("REJECT")}
                            disabled={!aiModalRejectReason.trim()}
                          >
                            반려 처리
                          </button>
                          <button
                            className="btn btn-close"
                            onClick={() => {
                              setShowRejectReasonInput(false);
                              setAiModalRejectReason("");
                            }}
                          >
                            취소
                          </button>
                        </div>
                      </div>
                    )}

                    {/* 기본 버튼들 (반려 사유 입력 필드가 표시되지 않았을 때만) */}
                    {!showRejectReasonInput && (
                      <>
                        <button
                          className="btn btn-approve"
                          onClick={() => handleAiModalAction("APPROVE")}
                        >
                          승인
                        </button>
                        <button
                          className="btn btn-reject"
                          onClick={() => handleAiModalAction("REJECT")}
                        >
                          반려
                        </button>
                        <button
                          className="btn btn-close"
                          onClick={handleCloseAiModal}
                        >
                          닫기
                        </button>
                      </>
                    )}
                  </div>
                ) : !loadingAiRecommendation ? (
                  <button
                    className="btn btn-primary"
                    onClick={handleCloseAiModal}
                  >
                    확인
                  </button>
                ) : null}
              </div>
            </div>
          </div>
        )}
      </div>
    </AppLayout>
  );
};

export default AdminExpenseApprovalDetailPage;
