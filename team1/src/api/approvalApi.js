import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "../util/jwtUtil";

const prefix = `${API_SERVER_HOST}/api/approval-requests`;

/**
 * 승인 요청 목록 조회
 *
 * @param {Object} params - 조회 파라미터 (page, size, requestType, status, startDate, endDate)
 * @returns {Promise<Object>} 승인 요청 목록
 */
export const getApprovalRequests = async (params) => {
    const res = await jwtAxios.get(`${prefix}/list`, { params });
    return res.data;
};

/**
 * 지출 결재 목록 조회 (관리자 전용)
 *
 * @param {Object} params - 조회 파라미터 (page, size, status, startDate, endDate)
 * @returns {Promise<Object>} 지출 결재 목록
 */
export const getExpenseApprovals = async (params) => {
    const res = await jwtAxios.get(`${prefix}/types/expense`, { params });
    return res.data;
};

/**
 * 상품 결재 목록 조회
 *
 * @param {Object} params - 조회 파라미터
 * @returns {Promise<Object>} 상품 결재 목록
 */
export const getProductApprovals = async (params) => {
    const res = await jwtAxios.get(`${prefix}/types/product`, { params });
    return res.data;
};

/**
 * 승인 요청 상세 조회
 *
 * @param {number} id - 승인 요청 ID
 * @returns {Promise<Object>} 승인 요청 상세 정보
 */
export const getApprovalRequest = async (id) => {
    const res = await jwtAxios.get(`${prefix}/${id}`);
    return res.data;
};

/**
 * 승인 처리 이력 조회
 *
 * @param {number} id - 승인 요청 ID
 * @returns {Promise<Array>} 처리 이력 목록
 */
export const getApprovalLogs = async (id) => {
    const res = await jwtAxios.get(`${prefix}/${id}/logs`);
    return res.data;
};

/**
 * 승인 요청 처리 (관리자 전용)
 *
 * @param {number} id - 승인 요청 ID
 * @param {Object} data - 처리 정보 (action, message)
 * @returns {Promise<Object>} 처리 결과
 */
export const actionApproval = async (id, data) => {
    const res = await jwtAxios.put(`${prefix}/${id}/action`, data);
    return res.data;
};

/**
 * 승인 요청 API 클라이언트 (호환성 유지)
 *
 * @namespace approvalApi
 */
export const approvalApi = {
    getApprovalRequests: (params) => {
        return jwtAxios.get(`${prefix}/list`, { params });
    },
    getExpenseApprovals: (params) => {
        return jwtAxios.get(`${prefix}/types/expense`, { params });
    },
    getProductApprovals: (params) => {
        return jwtAxios.get(`${prefix}/types/product`, { params });
    },
    getApprovalRequest: (id) => {
        return jwtAxios.get(`${prefix}/${id}`);
    },
    getApprovalLogs: (id) => {
        return jwtAxios.get(`${prefix}/${id}/logs`);
    },
    actionApproval: (id, data) => {
        return jwtAxios.put(`${prefix}/${id}/action`, data);
    },
};

