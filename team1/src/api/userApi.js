import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "../util/jwtUtil";

const prefix = `${API_SERVER_HOST}/api/users`;

// 함수로 export
export const getCurrentUser = async () => {
  const res = await jwtAxios.get(`${prefix}/me`);
  return res.data;
};

// 기존 코드와의 호환성을 위한 객체 export (점진적 마이그레이션)
export const userApi = {
  getCurrentUser: () => {
    return jwtAxios.get(`${prefix}/me`);
  },
};
