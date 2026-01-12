import axios from "axios";
import {getCookie, setCookie, removeCookie} from "./cookieUtil";
import {getAccessToken} from "../util/authToken"; // ✅ 추가

// API 서버 주소
export const API_SERVER_HOST = "http://localhost:8080";

// JWT용 axios 인스턴스 생성
const jwtAxios = axios.create({
    baseURL: process.env.REACT_APP_API_BASE_URL || "http://localhost:8080/api",
    timeout: 10000,
    headers: {"Content-Type": "application/json"},
    withCredentials: false,
});

// 로그인/회원가입 같은 "토큰 없는 요청"인지 체크
const isAuthEndpoint = (url = "") => {
    return url.startsWith("/auth/");
};

// ✅ WS에서도 똑같이 쓰도록 export
export function getAuthTokenForRequest() {
    return getAccessToken();
}

/**
 * Refresh Token으로 새 Access Token 발급 요청
 */
const refreshJWT = async (accessToken, refreshToken) => {
    const header = {headers: {Authorization: `Bearer ${accessToken}`}};

    const res = await axios.get(
        `${API_SERVER_HOST}/api/auth/refresh?refreshToken=${refreshToken}`,
        header
    );

    console.log("토큰 갱신 완료:", res.data);
    return res.data;
};

/**
 * 요청 전 인터셉터
 * - 쿠키에서 토큰을 가져와 Authorization 헤더에 자동 첨부
 * - /auth/** 엔드포인트는 토큰을 붙이지 않음
 */
const beforeReq = (config) => {
    const url = config.url || "";
    const method = (config.method || "GET").toUpperCase();

    console.log("[REQ]", method, (config.baseURL || "") + url);

    // ✅ FormData를 사용하는 경우 Content-Type 헤더를 제거
    // axios가 자동으로 "multipart/form-data; boundary=..." 형식으로 설정함
    if (config.data instanceof FormData) {
        delete config.headers["Content-Type"];
        console.log("[REQ] FormData detected - Content-Type 헤더 제거");
    }

    // ✅ /auth/** 는 토큰 붙이지 않음 (로그인 시점에 token 없거나 꼬이는 거 방지)
    if (!isAuthEndpoint(url)) {
        const memberInfo = getCookie("member");

        // 로그인 정보가 없으면 에러
        if (!memberInfo) {
            console.log("로그인 정보 없음");
            return Promise.reject({
                response: {
                    data: {error: "REQUIRE_LOGIN"},
                },
            });
        }

        const token = memberInfo?.accessToken;

        console.log("[REQ token exists?]", !!token);
        if (token) {
            console.log("[REQ token head]", token.slice(0, 20) + "...");
            config.headers.Authorization = `Bearer ${token}`;
        }
    } else {
        console.log("[REQ auth endpoint] skip Authorization");
    }

    // ✅ 로그인 요청만 formLogin 호환으로 Content-Type 자동 변경
    if (url === "/auth/login" && config.data instanceof URLSearchParams) {
        config.headers["Content-Type"] = "application/x-www-form-urlencoded";
    }

    return config;
};

/**
 * 요청 실패 인터셉터
 */
const requestFail = (err) => {
    console.log("--- jwtAxios 요청 에러 ---", err);
    return Promise.reject(err);
};

/**
 * 응답 전 인터셉터
 * - Access Token 만료 시 자동으로 갱신 후 원래 요청 재시도
 */
const beforeRes = async (res) => {
    const data = res.data;

    // Access Token 만료 에러인 경우 (토큰 자동 갱신)
    // 서버가 "message": "ERROR_ACCESS_TOKEN" 형식으로 반환하므로 확인
    if (data && (data.error === "ERROR_ACCESS_TOKEN" || data.message === "ERROR_ACCESS_TOKEN")) {
        console.log("[JWT] Access Token 만료 - 갱신 시도");
        return await handleTokenRefresh(res.config);
    }

    return res;
};

/**
 * 응답 실패 인터셉터
 * - 401 에러 처리 (토큰 갱신 실패 시 로그아웃)
 */
const responseFail = async (err) => {
    const status = err.response?.status;
    const url = err.config?.url || "";
    const errorData = err.response?.data;

    console.log("[RES ERR]", status, url, errorData);

    // ✅ 로그인 요청이 401인 경우: 바로 튕기지 말고 화면에서 메시지 띄우게 둔다
    if (status === 401 && url.includes("/auth/login")) {
        return Promise.reject(err);
    }

    // ✅ 401 에러이고 ERROR_ACCESS_TOKEN 메시지인 경우 토큰 갱신 시도
    if (status === 401 && (errorData?.message === "ERROR_ACCESS_TOKEN" || errorData?.error === "ERROR_ACCESS_TOKEN")) {
        console.log("[JWT] 401 ERROR_ACCESS_TOKEN - 토큰 갱신 시도");
        return await handleTokenRefresh(err.config);
    }
    // 권한 없음 처리(403)
    if (status === 403) {
        console.warn("권한이 없습니다:", errorData);
    }
    // 토큰 갱신 실패 시 기존 화면에 유지
    return Promise.reject(err);
}

// 공통 토큰 갱신 처리 함수 (내부 사용)
async function handleTokenRefresh(originalConfig) {
    // ✅ 무한 루프 방지: 이미 토큰 갱신 시도 중인 요청인지 확인
    if (originalConfig._retry) {
        console.error("[JWT] 토큰 갱신 실패 - 무한 루프 방지");
        return Promise.reject({
            response: {
                status: 401,
                data: {error: "TOKEN_REFRESH_FAILED"},
            },
        });
    }
    originalConfig._retry = true;

    const memberCookieValue = getCookie("member");

    if (!memberCookieValue || !memberCookieValue.refreshToken) {
        console.log("[JWT] Refresh Token 없음 - 로그아웃 처리");
        // 에러 발생 시 삭제 한해찬!!!
        removeCookie("member"); // 필요 시 쿠키 삭제

        return Promise.reject({
            response: {
                data: {error: "REQUIRE_LOGIN"},
            },
        });
    }

    try {
        // Refresh Token으로 새 토큰 발급
        const result = await refreshJWT(
            memberCookieValue.accessToken,
            memberCookieValue.refreshToken
        );

        console.log("[JWT] 새 토큰 발급 완료");

        // 쿠키에 새 토큰 저장
        memberCookieValue.accessToken = result.accessToken;
        memberCookieValue.refreshToken = result.refreshToken;
        setCookie("member", memberCookieValue, 1);

        // ✅ 새 토큰으로 Authorization 헤더 설정
        originalConfig.headers.Authorization = `Bearer ${result.accessToken}`;
        
        // ✅ FormData인 경우 Content-Type 헤더 제거 (재요청 시에도 유지)
        if (originalConfig.data instanceof FormData) {
            delete originalConfig.headers["Content-Type"];
        }

        return await jwtAxios(originalConfig);
    } catch (error) {
        console.error("[JWT] 토큰 갱신 실패:", error);
        removeCookie("member"); // 토큰 갱신 실패 시 쿠키 삭제
        return Promise.reject(error);
    }
}

// // 401 에러 시 로그아웃 처리 (토큰 갱신이 실패한 경우)
// if (status === 401) {
//     console.log("[JWT] 401 에러 - 로그아웃 처리");
//     // 기존 코드: 쿠키 삭제 및 로그인 화면으로 이동
//     // removeCookie("member");
//     // window.location.href = "/";
//     // 401 에러 발생 시 기존 화면에 유지
// } else if (status === 403) {
//     console.warn("권한이 없습니다:", err.response?.data);
// }

// return Promise.reject(err);
// }
// ;

// 인터셉터 등록
jwtAxios.interceptors.request.use(beforeReq, requestFail);
jwtAxios.interceptors.response.use(beforeRes, responseFail);

export default jwtAxios;
