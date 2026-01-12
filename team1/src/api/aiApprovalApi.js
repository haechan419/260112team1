/**
 * AI 승인/반려 추천 API
 * 
 * Python AI 서버와 통신하여 지출 내역의 승인/반려 추천을 받아옵니다.
 */

const AI_SERVER_HOST = "http://localhost:8000";

/**
 * AI 승인/반려 추천 요청
 * 
 * @param {Object} expenseData - 지출 내역 정보
 * @param {Object} expenseData.receiptDate - 지출 일자 (YYYY-MM-DD)
 * @param {Object} expenseData.merchant - 가맹점명
 * @param {Object} expenseData.amount - 금액 (숫자)
 * @param {Object} expenseData.category - 카테고리 (식비, 교통비, 비품, 기타)
 * @param {Object} expenseData.description - 상세내용
 * @param {Object|null} receiptExtraction - 영수증 OCR 추출 결과 (선택)
 * @returns {Promise<Object>} AI 추천 결과
 */
export const getApprovalRecommendation = async (expenseData, receiptExtraction = null) => {
  try {
    const response = await fetch(`${AI_SERVER_HOST}/api/ai/receipt/recommend-approval`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        expense_data: expenseData,
        receipt_extraction: receiptExtraction
      })
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`HTTP error! status: ${response.status}, message: ${errorText}`);
    }

    const data = await response.json();
    return data;
  } catch (error) {
    console.error('[AI Approval API] 호출 실패:', error);
    throw error;
  }
};

