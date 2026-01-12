import axios from "axios";

export const API_SERVER_HOST = "http://localhost:8080";

// 1. 백엔드 주소와 일치
const prefix = `${API_SERVER_HOST}/api/requests`;

// 1. 등록 (결재 상신)
export const postRequest = async (requestData) => {
    const res = await axios.post(`${prefix}/`, requestData);
    return res.data;
};

// 2. 목록 조회 (내 결재함)
export const getRequestList = async () => {
    const res = await axios.get(`${prefix}/list`);
    return res.data;
};

// 3. 상태 변경 (관리자 승인/반려용)
// URL 파라미터가 아니라 Body(JSON)로 보내도록 변경
export const putRequestStatus = async (rno, status, reason = "") => {
    const data = {
        status: status, // "APPROVED" or "REJECTED"
        rejectReason: reason, // 반려 사유 (없으면 빈 문자열)
    };

    // 백엔드: @PutMapping("/{rno}/status")
    const res = await axios.put(`${prefix}/${rno}/status`, data);
    return res.data;
};
