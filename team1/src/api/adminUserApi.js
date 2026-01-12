import jwtAxios from "../util/jwtUtil";


// 사원 목록 조회
export const getUsers = async (params) => {
    const res = await jwtAxios.get("/admin/users", { params });
    return res.data;
};

// 사원 상세 조회
export const getUser = async (id) => {
    const res = await jwtAxios.get(`/admin/users/${id}`);
    return res.data;
};

// 사원 등록
export const createUser = async (data) => {
    const res = await jwtAxios.post("/admin/users", data);
    return res.data;
};

// 사원 수정
export const updateUser = async (id, data) => {
    const res = await jwtAxios.put(`/admin/users/${id}`, data); // ✅ PUT으로 수정
    return res.data;
};

// 퇴사 처리
export const resignUser = async (id) => {
    const res = await jwtAxios.put(`/admin/users/${id}/resign`);
    return res.data;
};

// 계정 잠금 해제
export const unlockUser = async (id) => {
    const res = await jwtAxios.put(`/admin/users/${id}/unlock`);
    return res.data;
};

// ═══════════════════════════════════════════════════════════════
// 프로필 이미지 관련 API
// ═══════════════════════════════════════════════════════════════

// 프로필 이미지 업로드
export const uploadProfileImage = async (userId, file) => {
    const formData = new FormData();
    formData.append("file", file);

  // ✅ FormData를 사용할 때는 Content-Type 헤더를 명시하지 않아야 합니다.
  // axios가 자동으로 "multipart/form-data; boundary=..." 형식으로 설정합니다.
  // 수동으로 설정하면 boundary 파라미터가 없어서 서버가 파싱하지 못하고,
  // Authorization 헤더도 덮어씌워질 수 있습니다.
  const res = await jwtAxios.post(`/admin/users/${userId}/profile-image`, formData);
  return res.data;
};

// 프로필 이미지 조회
export const getProfileImage = async (userId) => {
    const res = await jwtAxios.get(`/admin/users/${userId}/profile-image`);
    return res.data;
};

// 프로필 이미지 삭제
export const deleteProfileImage = async (userId) => {
    const res = await jwtAxios.delete(`/admin/users/${userId}/profile-image`);
    return res.data;
};

export const adminUserApi = {
    getUsers,
    getUser,
    createUser,
    updateUser,
    resignUser,
    unlockUser,
    uploadProfileImage,
    getProfileImage,
    deleteProfileImage,
};

export default adminUserApi;
