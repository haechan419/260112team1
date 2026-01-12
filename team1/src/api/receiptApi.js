import jwtAxios from "../util/jwtUtil";

// ✅ 수정: jwtAxios의 baseURL이 이미 /api이므로 상대 경로 사용
const prefix = "/receipt/receipts";

// 함수로 export
export const uploadReceipt = async (expenseId, file) => {
    // ✅ 추가: expenseId 유효성 검사
    if (!expenseId || expenseId === "undefined" || expenseId === "null") {
        throw new Error("expenseId가 필요합니다.");
    }

    // ✅ 추가: file 유효성 검사
    if (!file) {
        throw new Error("파일이 필요합니다.");
    }

    const formData = new FormData();
    formData.append("expenseId", expenseId.toString());
    formData.append("file", file);

    // ✅ 디버깅: FormData 내용 확인
    console.log("[receiptApi] FormData expenseId:", expenseId);
    console.log("[receiptApi] FormData file:", file?.name, file?.size);

    // ✅ 변경: meetingNoteApi.js와 완전히 동일한 방식
    // transformRequest 제거하고 headers에 명시적으로 설정
    // axios가 FormData를 감지하면 자동으로 boundary를 포함한 multipart/form-data를 설정함
    // ✅ 추가: OCR 처리는 시간이 오래 걸릴 수 있으므로 타임아웃을 5분(300초)으로 설정
    try {
        const res = await jwtAxios.post(`${prefix}/upload`, formData, {
            timeout: 300000, // 5분 (300초) - OCR 처리가 2-3분 걸릴 수 있음
            headers: {
                "Content-Type": "multipart/form-data",
            },
        });
        return res.data;
    } catch (error) {
        // ✅ 추가: 에러 상세 로그
        console.error("[receiptApi] 업로드 실패:", error);
        console.error("[receiptApi] 에러 응답:", error.response?.data);
        console.error("[receiptApi] 에러 상태:", error.response?.status);
        console.error("[receiptApi] 요청 URL:", error.config?.url);
        console.error("[receiptApi] 요청 헤더:", error.config?.headers);
        throw error;
    }
};

export const getReceipt = async (id) => {
    const res = await jwtAxios.get(`${prefix}/${id}`);
    return res.data;
};

export const getReceiptImage = async (id) => {
    const res = await jwtAxios.get(`${prefix}/${id}/image`, {
        responseType: "blob",
    });
    return res.data; // ✅ 수정: res.data 반환 (blob 데이터만 반환)
};

export const getExtraction = async (id) => {
    const res = await jwtAxios.get(`${prefix}/${id}/extraction`);
    return res.data;
};

export const deleteReceipt = async (id) => {
    const res = await jwtAxios.delete(`${prefix}/${id}`);
    return res.data;
};

// 기존 코드와의 호환성을 위한 객체 export (점진적 마이그레이션)
export const receiptApi = {
    uploadReceipt,
    getReceipt,
    getReceiptImage,
    getExtraction,
    deleteReceipt,
};
