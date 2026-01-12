import jwtAxios from "../util/jwtUtil";

// 회의록 업로드
export const uploadMeetingNote = async (file) => {
    const formData = new FormData();
    formData.append("file", file);

    const response = await jwtAxios.post("/meeting-notes/upload", formData, {
        headers: {
            "Content-Type": "multipart/form-data",
        },
    });
    return response.data;
};

// 회의록 조회
export const getMeetingNote = async (id) => {
    const response = await jwtAxios.get(`/meeting-notes/${id}`);
    return response.data;
};

// 회의록 목록 조회
export const getMeetingNoteList = async () => {
    const response = await jwtAxios.get("/meeting-notes/list");
    return response.data;
};

// 미분석 회의록 목록 조회
export const getUnanalyzedMeetingNoteList = async () => {
    const response = await jwtAxios.get("/meeting-notes/unanalyzed");
    return response.data;
};

// 회의록 파일 다운로드
export const downloadMeetingNoteFile = async (id) => {
    const response = await jwtAxios.get(`/meeting-notes/${id}/file`, {
        responseType: "blob",
    });
    return response.data;
};

// 회의록 분석하여 Todo 생성
export const analyzeMeetingNote = async (id) => {
    const response = await jwtAxios.post(`/meeting-notes/${id}/analyze`, null, {
        timeout: 120000, // AI 분석은 시간이 오래 걸릴 수 있으므로 120초로 설정
    });
    return response.data;
};

// 회의록 삭제
export const deleteMeetingNote = async (id) => {
    const response = await jwtAxios.delete(`/meeting-notes/${id}`);
    return response.data;
};

