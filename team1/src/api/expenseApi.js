import jwtAxios from "../util/jwtUtil";

/**
 * 지출 내역 API 클라이언트
 *
 * @namespace expenseApi
 */
export const expenseApi = {
    getExpenses: (params) => {
        return jwtAxios.get("/receipt/expenses/list", { params });
    },

    getExpense: (id) => {
        return jwtAxios.get(`/receipt/expenses/${id}`);
    },

    createExpense: (data) => {
        return jwtAxios.post("/receipt/expenses/", data);
    },

    updateExpense: (id, data) => {
        return jwtAxios.put(`/receipt/expenses/${id}`, data);
    },

    deleteExpense: (id) => {
        return jwtAxios.delete(`/receipt/expenses/${id}`);
    },

    submitExpense: (id, data) => {
        return jwtAxios.post(`/receipt/expenses/${id}/submit`, data);
    },
};

