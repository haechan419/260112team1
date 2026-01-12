export const API_SERVER_HOST = "http://localhost:8080";

const prefix = `${API_SERVER_HOST}/api/notice`;

// 목록 조회
export const getNoticeList = async (pageParam)