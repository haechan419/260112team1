import React, { useState, useEffect } from "react";
import {
  uploadReceipt,
  getExtraction,
  getReceiptImage,
} from "../../api/receiptApi";
import OcrResultModal from "./OcrResultModal";
import useCustomMove from "../../hooks/useCustomMove";
import "./ExpenseForm.css";

const ExpenseForm = ({ expense, onSubmit, onCancel, onSubmitComplete }) => {
  const [formData, setFormData] = useState({
    receiptDate: expense?.receiptDate || new Date().toISOString().split("T")[0],
    merchant: expense?.merchant || "",
    amount: expense?.amount || 0,
    category: expense?.category || "",
    description: expense?.description || "",
  });
  const [receiptFile, setReceiptFile] = useState(null);
  const [receiptPreview, setReceiptPreview] = useState(null);
  const [uploadingReceipt, setUploadingReceipt] = useState(false);
  const [ocrProcessing, setOcrProcessing] = useState(false);
  const [ocrResult, setOcrResult] = useState(null);
  const [showOcrModal, setShowOcrModal] = useState(false);
  const [uploadedReceiptId, setUploadedReceiptId] = useState(null);
  const [ocrApplied, setOcrApplied] = useState(false);
  const [fetching, setFetching] = useState(false);
  const [useReceipt, setUseReceipt] = useState(true); // 영수증 사용 여부 (기본값: true)
  // ✅ 추가: 영수증 OCR 통합 - 임시 지출 내역 ID state (파일 선택 시 즉시 OCR 실행을 위해)
  const [tempExpenseId, setTempExpenseId] = useState(null); // 임시 지출 내역 ID
  // ✅ 추가: OCR 경과 시간 표시용
  const [ocrElapsedTime, setOcrElapsedTime] = useState(0); // OCR 경과 시간 (초)
  // ✅ 추가: 수정 모드에서 기존 영수증 이미지 표시용
  const [existingReceiptImage, setExistingReceiptImage] = useState(null);

  const { moveToExpenseList } = useCustomMove();

  useEffect(() => {
    if (expense) {
      setFormData({
        receiptDate: expense.receiptDate || "",
        merchant: expense.merchant || "",
        amount: expense.amount || 0,
        category: expense.category || "",
        description: expense.description || "",
      });

      // ✅ 추가: 수정 모드에서 기존 영수증 이미지 로드
      if (expense.hasReceipt && expense.receiptId) {
        loadExistingReceiptImage(expense.receiptId);
      }
    }
  }, [expense]);

  // ✅ 추가: 기존 영수증 이미지 로드 함수
  const loadExistingReceiptImage = async (receiptId) => {
    try {
      const blobData = await getReceiptImage(receiptId);
      const url = URL.createObjectURL(blobData);
      setExistingReceiptImage(url);
    } catch (error) {
      console.error("기존 영수증 이미지 로드 실패:", error);
      setExistingReceiptImage(null);
    }
  };

  // ✅ 수정: 영수증 OCR 통합 - async 함수로 변경, 임시 지출 내역 생성 로직 추가
  const handleFileChange = async (e) => {
    if (e.target.files && e.target.files[0]) {
      const selectedFile = e.target.files[0];
      setReceiptFile(selectedFile);

      // 미리보기 생성
      const reader = new FileReader();
      reader.onloadend = () => {
        setReceiptPreview(reader.result);
      };
      reader.readAsDataURL(selectedFile);

      // ✅ 변경: 등록 모드이고 임시 지출 내역이 없으면 생성 후 OCR 진행
      // OCR 결과를 바로 formData에 반영 (모달 없이)
      if (!expense && !tempExpenseId) {
        try {
          setOcrProcessing(true);
          setOcrElapsedTime(0); // 경과 시간 초기화

          // 임시 지출 내역 생성 (기본값으로, DRAFT 상태)
          const tempExpenseData = {
            receiptDate: formData.receiptDate,
            merchant: "",
            amount: 0,
            category: "",
            description: "",
          };

          console.log("[ExpenseForm] 임시 지출 내역 생성 시작");
          const tempResult = await onSubmit(tempExpenseData);
          console.log("[ExpenseForm] 임시 지출 내역 생성 완료:", tempResult);

          // ✅ 수정: 백엔드가 {result: id} 형태로 반환하므로 result를 먼저 확인
          const tempId = tempResult?.result || tempResult?.id;
          console.log("[ExpenseForm] tempExpenseId:", tempId);

          // ✅ 추가: tempId 유효성 검사
          if (!tempId) {
            console.error(
              "[ExpenseForm] 임시 지출 내역 ID를 가져올 수 없습니다:",
              tempResult
            );
            setOcrProcessing(false);
            alert("지출 내역 생성에 실패했습니다. 다시 시도해주세요.");
            return;
          }

          setTempExpenseId(tempId);

          // ✅ 변경: OCR 실행 후 결과를 바로 formData에 반영 (모달 없이)
          await handleReceiptUploadForPreview(tempId, selectedFile);
        } catch (error) {
          console.error("임시 지출 내역 생성 실패:", error);
          setOcrProcessing(false);
          alert("영수증 처리 중 오류가 발생했습니다.");
        }
      } else if (expense || tempExpenseId) {
        // 수정 모드이거나 이미 임시 지출 내역이 있는 경우
        const expenseId = expense?.id || tempExpenseId;

        // ✅ 추가: expenseId 유효성 검사
        if (!expenseId) {
          console.error("[ExpenseForm] expenseId가 유효하지 않습니다:", {
            expense,
            tempExpenseId,
          });
          alert("지출 내역 ID를 찾을 수 없습니다. 페이지를 새로고침해주세요.");
          return;
        }

        // ✅ 변경: OCR 실행 후 결과를 바로 formData에 반영 (모달 없이)
        await handleReceiptUploadForPreview(expenseId, selectedFile);
      }
    }
  };

  const handleRemoveFile = () => {
    setReceiptFile(null);
    setReceiptPreview(null);
    const fileInput = document.querySelector('input[type="file"]');
    if (fileInput) {
      fileInput.value = "";
    }
  };

  // ✅ 추가: OCR 결과를 바로 formData에 반영하는 메서드 (모달 없이)
  // 파일 선택 시 즉시 OCR 처리하고 결과를 입력 필드에 표시
  const handleReceiptUploadForPreview = async (expenseId, file = null) => {
    const fileToUpload = file || receiptFile;

    if (!fileToUpload || !expenseId) return;

    setUploadingReceipt(true);
    setOcrElapsedTime(0); // 경과 시간 초기화

    try {
      // 영수증 업로드
      const receiptData = await uploadReceipt(expenseId, fileToUpload);
      const receiptId = receiptData.result || receiptData.id;
      setUploadedReceiptId(receiptId);

      // OCR 처리 중 상태 표시
      setOcrProcessing(true);

      // 경과 시간 표시를 위한 타이머 시작
      const timerInterval = setInterval(() => {
        setOcrElapsedTime((prev) => prev + 1);
      }, 1000);

      // OCR 결과 조회 (최대 30번 재시도, 10초 간격 = 총 5분 대기)
      let extraction = null;
      let retryCount = 0;
      const maxRetries = 30; // 30회
      const pollInterval = 10000; // 10초 간격

      while (retryCount < maxRetries && !extraction) {
        try {
          await new Promise((resolve) => setTimeout(resolve, pollInterval));
          extraction = await getExtraction(receiptId);
          if (extraction) break;
        } catch (error) {
          // OCR 결과가 아직 없을 수 있음 (404 등)
          const elapsedSeconds = (retryCount + 1) * (pollInterval / 1000);
          console.log(
            `[ExpenseForm] OCR 결과 조회 시도 ${
              retryCount + 1
            }/${maxRetries} (${elapsedSeconds}초 경과)`
          );
        }
        retryCount++;
      }

      clearInterval(timerInterval); // 타이머 정리
      setOcrProcessing(false);
      setUploadingReceipt(false);

      if (extraction) {
        // ✅ OCR 결과를 formData에 반영 (UI 입력 필드에 표시)
        setFormData((prevFormData) => ({
          ...prevFormData,
          receiptDate: extraction.extractedDate || prevFormData.receiptDate,
          merchant: extraction.extractedMerchant || prevFormData.merchant,
          amount: extraction.extractedAmount || prevFormData.amount,
          category: extraction.extractedCategory || prevFormData.category,
          description:
            extraction.extractedDescription || prevFormData.description,
        }));

        setOcrResult(extraction);
        setOcrApplied(true);
        // ✅ 모달 표시하지 않음 - UI 입력 필드에 바로 표시됨
      } else {
        alert(
          "영수증이 업로드되었습니다.\n\nOCR 처리가 완료되면 자동으로 표시됩니다.\n(처리 시간: 약 2-3분 소요)"
        );
      }
    } catch (error) {
      console.error("영수증 업로드 실패:", error);
      setOcrProcessing(false);
      setUploadingReceipt(false);
      alert("영수증 업로드에 실패했습니다. 나중에 다시 시도해주세요.");
    }
  };

  // ✅ 수정: 영수증 OCR 통합 - file 파라미터 추가, OCR 결과 자동 적용 로직 추가
  // 영수증 업로드 및 OCR 결과 조회 (기존 메서드 - 모달 표시용)
  const handleReceiptUpload = async (expenseId, file = null) => {
    const fileToUpload = file || receiptFile;

    // ✅ 수정: 더 엄격한 유효성 검사
    if (!fileToUpload) {
      console.error("[ExpenseForm] 업로드할 파일이 없습니다.");
      alert("파일을 선택해주세요.");
      return;
    }

    if (!expenseId || expenseId === "undefined" || expenseId === "null") {
      console.error("[ExpenseForm] expenseId가 유효하지 않습니다:", expenseId);
      alert("지출 내역 ID가 필요합니다. 페이지를 새로고침해주세요.");
      return;
    }

    setUploadingReceipt(true);
    setFetching(true);
    try {
      // 영수증 업로드
      const receiptData = await uploadReceipt(expenseId, fileToUpload);
      const receiptId = receiptData.result || receiptData.id;
      setUploadedReceiptId(receiptId);

      // OCR 처리 중 상태 표시 (즉시 표시)
      setOcrProcessing(true);

      // OCR 결과 조회 (최대 40번 재시도, 3초 간격 = 총 약 2분 대기)
      let extraction = null;
      let retryCount = 0;
      const maxRetries = 40; // 40회
      const pollInterval = 3000; // 3초 간격 (더 빠른 반응)

      while (retryCount < maxRetries && !extraction) {
        try {
          // 첫 번째 시도는 즉시, 이후는 3초 간격
          if (retryCount > 0) {
            await new Promise((resolve) => setTimeout(resolve, pollInterval));
          }
          extraction = await getExtraction(receiptId);
          if (extraction) break;
        } catch (error) {
          // OCR 결과가 아직 없을 수 있음
          const elapsedSeconds = retryCount * (pollInterval / 1000);
          console.log(
            `[ExpenseForm] OCR 결과 조회 시도 ${
              retryCount + 1
            }/${maxRetries} (${elapsedSeconds}초 경과)`
          );
        }
        retryCount++;
      }

      setOcrProcessing(false);
      setFetching(false);

      if (extraction) {
        setOcrResult(extraction);

        // ✅ 추가: 영수증 OCR 통합 - OCR 결과를 자동으로 formData에 적용
        // OCR 결과를 자동으로 formData에 적용
        setFormData((prevFormData) => ({
          ...prevFormData,
          receiptDate: extraction.extractedDate || prevFormData.receiptDate,
          merchant: extraction.extractedMerchant || prevFormData.merchant,
          amount: extraction.extractedAmount || prevFormData.amount,
          category: extraction.extractedCategory || prevFormData.category,
        }));

        setOcrApplied(true); // OCR 적용됨 표시
        setShowOcrModal(true);
      } else {
        alert("영수증이 업로드되었습니다. OCR 처리는 진행 중입니다.");
      }
    } catch (error) {
      console.error("영수증 업로드 실패:", error);
      setOcrProcessing(false);
      setFetching(false);
      alert("영수증 업로드에 실패했습니다. 나중에 다시 시도해주세요.");
    } finally {
      setUploadingReceipt(false);
    }
  };

  // OCR 결과를 폼에 적용
  const handleApplyOcrResult = (extraction) => {
    if (!extraction) return;

    setFormData({
      ...formData,
      receiptDate: extraction.extractedDate || formData.receiptDate,
      merchant: extraction.extractedMerchant || formData.merchant,
      amount: extraction.extractedAmount || formData.amount,
      category: extraction.extractedCategory || formData.category,
      description: extraction.extractedDescription || formData.description, // ✅ 추가: extractedDescription 포함
    });

    setOcrApplied(true);
    setShowOcrModal(false);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    // ✅ 변경: 영수증 모드에서 필수 필드 검증
    if (useReceipt) {
      if (
        !formData.receiptDate ||
        !formData.merchant ||
        !formData.amount ||
        !formData.category
      ) {
        alert("필수 항목을 모두 입력해주세요.");
        return;
      }
    }

    // 직접 입력 모드에서 필수 필드 검증
    if (!useReceipt) {
      if (
        !formData.receiptDate ||
        !formData.merchant ||
        !formData.amount ||
        !formData.category
      ) {
        alert("필수 항목을 모두 입력해주세요.");
        return;
      }
    }

    setFetching(true);
    try {
      // ✅ 수정: 수정 모드와 등록 모드를 명확히 구분
      if (expense) {
        // 수정 모드: 기존 expense 업데이트 (ExpenseAddPage의 handleSubmit이 id를 알아서 처리)
        await onSubmit(formData);
      } else if (tempExpenseId) {
        // 등록 모드 + 이미 임시 지출 내역이 있음: 기존 임시 지출 내역 업데이트
        // ExpenseAddPage의 handleSubmit에 tempExpenseId를 전달하여 updateExpense 호출
        await onSubmit(formData, tempExpenseId);
      } else {
        // 등록 모드 + 임시 지출 내역 없음: 새로 생성
        const result = await onSubmit(formData);
        const newId = result?.id || result?.result;
        if (newId) {
          setTempExpenseId(newId);
        }
      }

      // ✅ 영수증은 이미 업로드되어 있으므로 추가 업로드 불필요
      // (파일 선택 시 이미 업로드됨)

      setFetching(false);
      if (onSubmitComplete) {
        onSubmitComplete();
      } else {
        moveToExpenseList();
      }
    } catch (error) {
      console.error(expense ? "수정 실패:" : "등록 실패:", error);
      setFetching(false);
      alert(
        expense ? "지출 수정에 실패했습니다." : "지출 등록에 실패했습니다."
      );
    }
  };

  // OCR 모달이 닫힐 때 완료 콜백 호출
  const handleOcrModalClose = () => {
    setShowOcrModal(false);
    setFetching(false);
    if (onSubmitComplete) {
      onSubmitComplete();
    } else {
      moveToExpenseList();
    }
  };

  return (
    <form onSubmit={handleSubmit} className="expense-form">
      {fetching && (
        <div className="form-loading-overlay">
          <div className="form-loading-container">
            <div className="form-loading-spinner"></div>
            <p className="form-loading-text">
              {expense
                ? "지출 내역을 수정하는 중입니다"
                : "지출 내역을 등록하는 중입니다"}
            </p>
          </div>
        </div>
      )}
      {/* 등록 모드일 때만 선택 옵션 표시 */}
      {!expense && (
        <div className="form-group">
          <div className="registration-mode-selector">
            <label className="mode-selector-label">
              <input
                type="checkbox"
                checked={useReceipt}
                onChange={(e) => {
                  setUseReceipt(e.target.checked);
                  // 체크 해제 시 영수증 관련 상태 초기화
                  if (!e.target.checked) {
                    setReceiptFile(null);
                    setReceiptPreview(null);
                    setOcrApplied(false);
                    setOcrResult(null);
                  }
                }}
                className="mode-checkbox"
              />
              <span className="mode-checkbox-label">
                📎 영수증이 있으신가요? (OCR 자동 인식)
              </span>
            </label>
            <p className="mode-hint">
              {useReceipt
                ? "영수증을 업로드하면 AI가 자동으로 정보를 인식합니다."
                : "영수증 없이 직접 입력하여 등록할 수 있습니다."}
            </p>
          </div>
        </div>
      )}

      {/* 영수증 업로드 모드: 영수증 업로드 영역만 표시 (등록 모드 또는 수정 모드) */}
      {useReceipt && (
        <>
          <div className="form-group receipt-upload-section">
            <div className="receipt-upload-header">
              <label className="form-label receipt-label">
                📎 영수증 업로드
                <span className="receipt-badge">OCR 자동 인식</span>
              </label>
              <span className="receipt-hint">
                {expense
                  ? "새 영수증을 업로드하면 정보가 자동으로 입력됩니다 (선택사항)"
                  : "영수증을 업로드하면 정보가 자동으로 입력됩니다"}
              </span>
            </div>
            <div className="receipt-upload-area">
              <label className="file-input-label-large">
                <input
                  type="file"
                  accept="image/*"
                  onChange={handleFileChange}
                  disabled={uploadingReceipt}
                  style={{ display: "none" }}
                />
                {!receiptFile && !existingReceiptImage ? (
                  <div className="file-drop-zone">
                    <div className="file-drop-icon">📄</div>
                    <div className="file-drop-text">
                      <strong>클릭하거나 드래그하여 영수증 업로드</strong>
                      <span>이미지 파일 (JPG, PNG 등)</span>
                    </div>
                  </div>
                ) : (
                  <div className="file-preview-container">
                    {receiptFile ? (
                      <>
                        <div className="file-preview-info">
                          <span className="file-name">{receiptFile.name}</span>
                          <span className="file-size">
                            ({(receiptFile.size / 1024).toFixed(2)} KB)
                          </span>
                          <button
                            type="button"
                            onClick={handleRemoveFile}
                            disabled={uploadingReceipt}
                            className="file-remove-btn"
                          >
                            ×
                          </button>
                        </div>
                        {receiptPreview && (
                          <div className="file-preview-image">
                            <img src={receiptPreview} alt="영수증 미리보기" />
                          </div>
                        )}
                      </>
                    ) : (
                      existingReceiptImage && (
                        <>
                          <div className="file-preview-info">
                            <span className="file-name">기존 영수증</span>
                            <span
                              className="file-size"
                              style={{ color: "#10b981" }}
                            >
                              ✓ 업로드됨
                            </span>
                          </div>
                          <div className="file-preview-image">
                            <img src={existingReceiptImage} alt="기존 영수증" />
                          </div>
                        </>
                      )
                    )}
                  </div>
                )}
              </label>
            </div>
          </div>

          <div className="form-divider"></div>

          {/* ✅ 변경: OCR 결과를 수정 가능한 입력 필드로 표시 (등록 모드에서만) */}
          {receiptFile && !expense && (
            <>
              <div className="form-group">
                <label className="form-label">
                  AI 인식 결과
                  {ocrProcessing && (
                    <span
                      className="ocr-applied-badge"
                      style={{ backgroundColor: "#0ea5e9" }}
                    >
                      ⏳ 처리 중...
                    </span>
                  )}
                  {ocrApplied && !ocrProcessing && (
                    <span className="ocr-applied-badge">✓ OCR 적용됨</span>
                  )}
                </label>

                {/* OCR 처리 중 표시 */}
                {ocrProcessing && (
                  <div className="ocr-processing-indicator">
                    <div className="ocr-processing-spinner"></div>
                    <div style={{ flex: 1 }}>
                      <strong>OCR 분석 중...</strong>
                    </div>
                  </div>
                )}

                {/* ✅ 수정 가능한 입력 필드 */}
                <div className="form-group">
                  <label className="form-label">지출 일자 *</label>
                  <input
                    className="form-input"
                    type="date"
                    value={formData.receiptDate}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        receiptDate: e.target.value,
                      })
                    }
                    disabled={ocrProcessing}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">가맹점명 *</label>
                  <input
                    className="form-input"
                    type="text"
                    value={formData.merchant}
                    onChange={(e) =>
                      setFormData({ ...formData, merchant: e.target.value })
                    }
                    placeholder="가맹점명을 입력하세요"
                    disabled={ocrProcessing}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">금액 *</label>
                  <input
                    className="form-input"
                    type="number"
                    value={formData.amount}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        amount: parseInt(e.target.value) || 0,
                      })
                    }
                    min="0"
                    placeholder="금액을 입력하세요"
                    disabled={ocrProcessing}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">카테고리 *</label>
                  <select
                    className="form-select"
                    value={formData.category}
                    onChange={(e) =>
                      setFormData({ ...formData, category: e.target.value })
                    }
                    disabled={ocrProcessing}
                  >
                    <option value="">선택하세요</option>
                    <option value="식비">식비</option>
                    <option value="교통비">교통비</option>
                    <option value="비품">비품</option>
                    <option value="기타">기타</option>
                  </select>
                </div>

                <div className="form-group">
                  <label className="form-label">상세내용</label>
                  <textarea
                    className="form-textarea"
                    value={formData.description}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        description: e.target.value,
                      })
                    }
                    rows={3}
                    placeholder="상세 내용을 입력하세요"
                    disabled={ocrProcessing}
                  />
                </div>

                {ocrApplied && !ocrProcessing && (
                  <p
                    className="ocr-summary-hint"
                    style={{ color: "#10b981", marginTop: "8px" }}
                  >
                    ✓ OCR 결과가 자동으로 입력되었습니다. 필요시 수정해주세요.
                  </p>
                )}
              </div>
            </>
          )}

          {/* 수정 모드: OCR 처리 중 표시만 (입력 필드는 기존 것 사용) */}
          {receiptFile && expense && (
            <div className="form-group">
              <label className="form-label">
                AI 인식 결과
                {ocrProcessing && (
                  <span
                    className="ocr-applied-badge"
                    style={{ backgroundColor: "#0ea5e9" }}
                  >
                    ⏳ 처리 중...
                  </span>
                )}
                {ocrApplied && !ocrProcessing && (
                  <span className="ocr-applied-badge">✓ OCR 적용됨</span>
                )}
              </label>

              {/* OCR 처리 중 표시 */}
              {ocrProcessing && (
                <div className="ocr-processing-indicator">
                  <div className="ocr-processing-spinner"></div>
                  <div style={{ flex: 1 }}>
                    <strong>OCR 분석 중...</strong>
                    <div
                      style={{
                        fontSize: "12px",
                        color: "#64748b",
                        marginTop: "4px",
                      }}
                    >
                      경과 시간: {Math.floor(ocrElapsedTime / 60)}분{" "}
                      {ocrElapsedTime % 60}초
                      <br />
                      예상 소요 시간: 약 2-3분
                    </div>
                  </div>
                </div>
              )}

              {ocrApplied && !ocrProcessing && (
                <p
                  className="ocr-summary-hint"
                  style={{ color: "#10b981", marginTop: "8px" }}
                >
                  ✓ OCR 결과가 자동으로 입력되었습니다. 필요시 수정해주세요.
                </p>
              )}
            </div>
          )}
        </>
      )}

      {/* 직접 입력 모드: 일반 입력 폼 표시 */}
      {!expense && !useReceipt && (
        <>
          <div className="form-group">
            <label className="form-label">지출 일자 *</label>
            <input
              className="form-input"
              type="date"
              value={formData.receiptDate}
              onChange={(e) =>
                setFormData({ ...formData, receiptDate: e.target.value })
              }
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">가맹점명 *</label>
            <input
              className="form-input"
              type="text"
              value={formData.merchant}
              onChange={(e) =>
                setFormData({ ...formData, merchant: e.target.value })
              }
              required
              placeholder="가맹점명을 입력하세요"
            />
          </div>

          <div className="form-group">
            <label className="form-label">금액 *</label>
            <input
              className="form-input"
              type="number"
              value={formData.amount}
              onChange={(e) =>
                setFormData({
                  ...formData,
                  amount: parseInt(e.target.value) || 0,
                })
              }
              required
              min="0"
              placeholder="금액을 입력하세요"
            />
          </div>

          <div className="form-group">
            <label className="form-label">카테고리 *</label>
            <select
              className="form-select"
              value={formData.category}
              onChange={(e) =>
                setFormData({ ...formData, category: e.target.value })
              }
              required
            >
              <option value="">선택하세요</option>
              <option value="식비">식비</option>
              <option value="교통비">교통비</option>
              <option value="비품">비품</option>
              <option value="기타">기타</option>
            </select>
          </div>

          <div className="form-group">
            <label className="form-label">상세내용</label>
            <textarea
              className="form-textarea"
              value={formData.description}
              onChange={(e) =>
                setFormData({ ...formData, description: e.target.value })
              }
              rows={3}
              placeholder="상세 내용을 입력하세요"
            />
          </div>
        </>
      )}

      {/* 수정 모드: 기존 데이터 표시 */}
      {expense && (
        <>
          <div className="form-group">
            <label className="form-label">지출 일자</label>
            <input
              className="form-input"
              type="date"
              value={formData.receiptDate}
              onChange={(e) =>
                setFormData({ ...formData, receiptDate: e.target.value })
              }
            />
          </div>

          <div className="form-group">
            <label className="form-label">가맹점명</label>
            <input
              className="form-input"
              type="text"
              value={formData.merchant}
              onChange={(e) =>
                setFormData({ ...formData, merchant: e.target.value })
              }
            />
          </div>

          <div className="form-group">
            <label className="form-label">금액</label>
            <input
              className="form-input"
              type="number"
              value={formData.amount}
              onChange={(e) =>
                setFormData({
                  ...formData,
                  amount: parseInt(e.target.value) || 0,
                })
              }
              min="0"
            />
          </div>

          <div className="form-group">
            <label className="form-label">카테고리</label>
            <select
              className="form-select"
              value={formData.category}
              onChange={(e) =>
                setFormData({ ...formData, category: e.target.value })
              }
            >
              <option value="">선택하세요</option>
              <option value="식비">식비</option>
              <option value="교통비">교통비</option>
              <option value="비품">비품</option>
              <option value="기타">기타</option>
            </select>
          </div>

          <div className="form-group">
            <label className="form-label">상세내용</label>
            <textarea
              className="form-textarea"
              value={formData.description}
              onChange={(e) =>
                setFormData({ ...formData, description: e.target.value })
              }
              rows={3}
            />
          </div>
        </>
      )}

      <div
        className="form-actions"
        style={{
          display: "flex",
          gap: "12px",
          justifyContent: "flex-end",
          marginTop: "24px",
        }}
      >
        <button
          className="btn btn-secondary"
          type="button"
          onClick={onCancel}
          disabled={uploadingReceipt || ocrProcessing}
        >
          취소
        </button>
        <button
          className="btn btn-primary"
          type="submit"
          disabled={uploadingReceipt || ocrProcessing}
        >
          {uploadingReceipt
            ? "업로드 중..."
            : ocrProcessing
            ? "OCR 처리 중..."
            : expense
            ? "수정"
            : "등록"}
        </button>
      </div>

      {/* OCR 결과 모달 */}
      <OcrResultModal
        isOpen={showOcrModal}
        onClose={handleOcrModalClose}
        extraction={ocrResult}
        onApply={handleApplyOcrResult}
        onCancel={handleOcrModalClose}
      />
    </form>
  );
};

export default ExpenseForm;
