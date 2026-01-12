import axios from "axios";
import jwtAxios, { API_SERVER_HOST as HOST } from "../util/jwtUtil";

// 백엔드 주소
export const API_SERVER_HOST = HOST;
const prefix = `${API_SERVER_HOST}/api/products`;

// 1. 등록 (POST) 관리자 권한 필요 (jwtAxios)
export const postAdd = async (productObj) => {
    const header = { headers: { "Content-Type": "multipart/form-data" } };
    // 일반 axios -> jwtAxios로 변경
    const res = await jwtAxios.post(`${prefix}/`, productObj, header);
    return res.data;
};

// 2. 목록 조회 (GET)
export const getList = async (pageParam) => {
    const { page, size, category } = pageParam;

    const params = { page: page, size: size };

    if (category && category !== "All") {
        params.category = category;
    }

    const res = await axios.get(`${prefix}/list`, { params: params });
    return res.data;
};

// 3. 상세 조회 (GET)
export const getOne = async (pno) => {
    const res = await axios.get(`${prefix}/${pno}`);
    return res.data;
};

// 4. 수정 (PUT) 관리자 권한 필요 (jwtAxios)
export const putOne = async (pno, productObj) => {
    const header = { headers: { "Content-Type": "multipart/form-data" } };
    const res = await jwtAxios.put(`${prefix}/${pno}`, productObj, header);
    return res.data;
};

// 5. 삭제 (DELETE) 관리자 권한 필요 (jwtAxios)
export const deleteOne = async (pno) => {
    const res = await jwtAxios.delete(`${prefix}/${pno}`);
    return res.data;
};

// 6. 순서 변경 (PUT) 관리자 권한 필요 (jwtAxios)
export const putOrder = async (pnoList) => {
    // 1. pnoList가 진짜 배열인지 확인 (안전장치)
    if (!Array.isArray(pnoList)) {
        console.error("putOrder 오류: 배열이 아닙니다.", pnoList);
        throw new Error("Invalid Data");
    }

    console.log("📤 순서 변경 요청 보냄:", pnoList); // [35, 36, 12, ...] 형태여야 함

    // 2. PUT 요청 보내기
    // 백엔드 컨트롤러 주소가 "/api/products/order" 라고 가정합니다.
    const res = await jwtAxios.put(`${prefix}/order`, pnoList);

    return res.data;
};
