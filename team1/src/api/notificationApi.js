import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "../util/jwtUtil";
import { getRequestList } from "./requestApi";

const expensePrefix = `${API_SERVER_HOST}/api/receipt/expenses`;

// 지출결의(영수증)
export const getMyExpenseNotifications = async () => {
    try {
        const res = await jwtAxios.get(
            `${expensePrefix}/list?t=${new Date().getTime()}`
        );
        const data = res.data;

        if (data && Array.isArray(data.dtoList)) return data.dtoList;
        if (data && Array.isArray(data.content)) return data.content;
        if (Array.isArray(data)) return data;
        return [];
    } catch (err) {
        return [];
    }
};

// 2. 비품구매(주문)
export const getMyOrderNotifications = async () => {
    try {
        const res = await jwtAxios.get(
            `${API_SERVER_HOST}/api/requests/my?t=${new Date().getTime()}`
        );

        const data = res.data;

        if (Array.isArray(data)) return data;
        if (data && Array.isArray(data.dtoList)) return data.dtoList;

        return [];
    } catch (err) {
        console.error("비품 알림 조회 실패:", err);
        return [];
    }
};
