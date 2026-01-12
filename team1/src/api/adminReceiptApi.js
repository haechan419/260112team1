import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "../util/jwtUtil";

const prefix = `/admin/receipts`;

/**
 * 영수증 목록 조회
 *
 * @param {Object} params - 조회 파라미터
 * @returns {Promise<Object>} 영수증 목록
 */
export const getReceipts = async (params) => {
    const res = await jwtAxios.get(`${prefix}/list`, { params });
    return res.data;
};

export const getReceipt = async (id) => {
    const res = await jwtAxios.get(`${prefix}/${id}`);
    return res.data;
};

export const getReceiptImage = async (id) => {
    const res = await jwtAxios.get(`${prefix}/${id}/image`, {
        responseType: "blob",
    });
    return res.data; // ✅ 수정: res 대신 res.data 반환 (blob 데이터만 반환)
};

export const verifyReceipt = async (id, data) => {
    // 영수증이 있는 경우: id 사용
    if (id) {
        const res = await jwtAxios.put(`${prefix}/${id}/verify`, data);
        return res.data;
    } else {
        // 영수증이 없는 경우: expenseId 사용
        const res = await jwtAxios.put(
            `${prefix}/expense/${data.expenseId}/verify`,
            data
        );
        return res.data;
    }
};

export const getReceiptExtraction = async (id) => {
    const res = await jwtAxios.get(`${prefix}/${id}/extraction`);
    return res.data;
};

// 기존 코드와의 호환성을 위한 객체 export (점진적 마이그레이션)
export const adminReceiptApi = {
    getReceipts: (params) => {
        return jwtAxios.get(`${prefix}/list`, { params });
    },
    getReceipt: (id) => {
        return jwtAxios.get(`${prefix}/${id}`);
    },
    getReceiptImage: (id) => {
        return jwtAxios
            .get(`${prefix}/${id}/image`, {
                responseType: "blob",
            })
            .then((res) => res.data); // ✅ 수정: res.data 반환
    },
    verifyReceipt: (id, data) => {
        if (id) {
            return jwtAxios.put(`${prefix}/${id}/verify`, data);
        } else {
            return jwtAxios.put(`${prefix}/expense/${data.expenseId}/verify`, data);
        }
    },
    getReceiptExtraction: (id) => {
        return jwtAxios.get(`${prefix}/${id}/extraction`);
    },
};
