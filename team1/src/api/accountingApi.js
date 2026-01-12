import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "../util/jwtUtil";

const prefix = `${API_SERVER_HOST}/api/admin/accounting`;

// 함수로 export
export const getDepartmentStatistics = async (params) => {
    const res = await jwtAxios.get(`${prefix}/statistics/department`, { params });
    return res.data;
};

export const getCategoryStatistics = async (params) => {
    const res = await jwtAxios.get(`${prefix}/statistics/category`, { params });
    return res.data;
};

export const getSummary = async () => {
    const res = await jwtAxios.get(`${prefix}/statistics/summary`);
    return res.data;
};

export const getOverBudgetList = async () => {
    const res = await jwtAxios.get(`${prefix}/statistics/over-budget`);
    return res.data;
};

export const getDepartments = async () => {
    const res = await jwtAxios.get(`${prefix}/departments`);
    return res.data;
};

// ✅ 최적화: 모든 통계 정보를 한번에 조회
export const getAllStatistics = async () => {
    const res = await jwtAxios.get(`${prefix}/statistics/all`);
    return res.data;
};

// 기존 코드와의 호환성을 위한 객체 export (점진적 마이그레이션)
export const accountingApi = {
    getDepartmentStatistics: (params) => {
        return jwtAxios.get(`${prefix}/statistics/department`, { params });
    },
    getCategoryStatistics: (params) => {
        return jwtAxios.get(`${prefix}/statistics/category`, { params });
    },
    getSummary: () => {
        return jwtAxios.get(`${prefix}/statistics/summary`);
    },
    getOverBudgetList: () => {
        return jwtAxios.get(`${prefix}/statistics/over-budget`);
    },
    getDepartments: () => {
        return jwtAxios.get(`${prefix}/departments`);
    },

};

// 한해찬 추가
export const getMonthlyExpenseTrend = async (params) => {
    const res = await jwtAxios.get(`${prefix}/statistics/monthly-trend`, { params });
    return res.data;
};
