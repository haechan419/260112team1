import axios from "axios";

// API 서버 주소
export const API_SERVER_HOST = "http://localhost:8080";

// axios 인스턴스 생성
const apiClient = axios.create({
    baseURL: API_SERVER_HOST,
    withCredentials: false,
    headers: {
        // ⭐ Spring Security formLogin은 form-urlencoded 형식을 기대함!
        "Content-Type": "application/x-www-form-urlencoded",
    },
});

/**
 * 로그인 API
 * @param {string} employeeNo - 사번 (로그인 ID)
 * @param {string} password - 비밀번호
 * @returns {Promise} axios response
 */
export const login = async (employeeNo, password) => {
    // ⭐ URLSearchParams로 form 데이터 생성
    const params = new URLSearchParams();
    params.append("employeeNo", employeeNo);
    params.append("password", password);

    const response = await apiClient.post("/api/auth/login", params);
    return response;
};

/**
 * 로그아웃 API
 */
export const logoutApi = async () => {
    const response = await apiClient.post("/api/auth/logout");
    return response;
};

export default {
    login,
    logoutApi,
};
