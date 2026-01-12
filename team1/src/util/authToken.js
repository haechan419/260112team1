// src/util/authToken.js
import { getCookie } from "./cookieUtil";

/**
 * getCookie("member")가
 * - 객체일 수도 있고
 * - JSON 문자열일 수도 있고
 * - URL-encoded 된 JSON 문자열일 수도 있고
 * - JSON 문자열이 한 번 더 stringify 된 형태일 수도 있음
 *
 * => 어떤 형태든 accessToken만 안정적으로 꺼내준다.
 */
export function getAccessToken() {
    const v = getCookie("member");
    if (!v) return null;

    // 1) 이미 객체면 바로
    if (typeof v === "object") return v.accessToken ?? null;

    // 2) 문자열이면 파싱 시도 (decode -> JSON.parse -> (문자열이면) JSON.parse)
    if (typeof v === "string") {
        const tryParse = (s) => {
            try { return JSON.parse(s); } catch { return null; }
        };

        // raw
        let parsed = tryParse(v);

        // decodeURIComponent 시도
        if (!parsed) parsed = tryParse(decodeURIComponent(v));

        // 2중 stringify 케이스: 1차 parse 결과가 string이면 2차 parse
        if (typeof parsed === "string") parsed = tryParse(parsed);

        if (parsed && typeof parsed === "object") return parsed.accessToken ?? null;
    }

    return null;
}
