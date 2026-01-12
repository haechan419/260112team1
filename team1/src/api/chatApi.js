// src/api/chatApi.js
import jwtAxios from "../util/jwtUtil";

// (선택) 절대 URL이 필요한 경우만 사용
const API_BASE = process.env.REACT_APP_API_BASE_URL || "http://localhost:8080";

/**
 * 첨부파일 다운로드 URL (백엔드가 /api/files/chat/{id}/download 라는 전제)
 * - inline=true면 브라우저에서 바로 열기용(가능한 경우)
 */
export function chatAttachmentDownloadUrl(attachmentId, inline = false) {
    const q = inline ? "?inline=true" : "";
    return `${API_BASE}/api/files/chat/${attachmentId}/download${q}`;
}

/**
 * ✅ JWT 포함 다운로드 (jwtAxios로 바이너리 받기)
 * - axios는 blob/arraybuffer 설정이 핵심
 * - content-type이 json/html이면 에러로 간주 (SPA fallback / 에러 응답 방지)
 */
export async function downloadChatAttachment(attachmentId, filename) {
    const url = `/files/chat/${attachmentId}/download`; // jwtAxios baseURL이 /api 라면 OK
    // 만약 jwtAxios baseURL이 이미 "/api" 포함이면 위처럼 쓰고,
    // baseURL이 "http://localhost:8080"만이면 "/api/files/..."로 바꿔야 함.

    const res = await jwtAxios.get(url, {
        responseType: "arraybuffer", // ✅ 가장 안전
        headers: { Accept: "*/*" },
    });

    // axios는 headers가 객체
    const ct = String(res.headers?.["content-type"] || "").toLowerCase();

    // ✅ json/html이면 파일이 아니라 에러 응답일 가능성이 큼
    if (ct.includes("application/json") || ct.includes("text/html")) {
        // arraybuffer -> text 변환 시도
        let text = "";
        try {
            text = new TextDecoder("utf-8").decode(res.data).slice(0, 200);
        } catch {}
        throw new Error(`Download failed: CT=${ct} BODY=${text}`);
    }

    const blob = new Blob([res.data], { type: ct || "application/octet-stream" });
    const objectUrl = window.URL.createObjectURL(blob);

    const a = document.createElement("a");
    a.href = objectUrl;
    a.download = filename || "file";
    document.body.appendChild(a);
    a.click();
    a.remove();

    window.URL.revokeObjectURL(objectUrl);
}

export const chatApi = {
    // --- user search / create rooms ---
    searchUsers: (q, limit = 20) =>
        jwtAxios.get("/chat/users/search", { params: { q, limit } }).then((r) => r.data),

    createDm: (targetUserId) =>
        jwtAxios.post("/chat/rooms/dm", { targetUserId }).then((r) => r.data),

    createGroup: (memberUserIds) =>
        jwtAxios.post("/chat/rooms/group", { memberUserIds }).then((r) => r.data),

    invite: (roomId, userIds) =>
        jwtAxios.post(`/chat/rooms/${roomId}/invite`, { userIds }).then((r) => r.data),

    // --- rooms / messages ---
    getRooms: () => jwtAxios.get("/chat/rooms").then((r) => r.data),

    getMessages: (roomId, { cursor, limit } = {}) => {
        const params = {};
        if (cursor) params.cursor = cursor;
        if (limit) params.limit = limit;
        return jwtAxios.get(`/chat/rooms/${roomId}/messages`, { params }).then((r) => r.data);
    },

    sendMessage: (roomId, content) =>
        jwtAxios.post(`/chat/rooms/${roomId}/messages`, { content }).then((r) => r.data),

    updateRead: (roomId, lastReadMessageId = null) =>
        jwtAxios.post(`/chat/rooms/${roomId}/read`, { lastReadMessageId }).then((r) => r.data),

    getRoomMeta: (roomId) =>
        jwtAxios.get(`/chat/rooms/${roomId}/meta`).then((r) => r.data),

    deleteRoom: (roomId) =>
        jwtAxios.delete(`/chat/rooms/${roomId}`).then((r) => r.data),

    uploadAttachments: async (roomId, content, files) => {
        const form = new FormData();
        if (content != null) form.append("content", content); // "" 가능
        if (files && files.length) for (const f of files) form.append("files", f);

        const res = await jwtAxios.post(`/chat/rooms/${roomId}/attachments`, form, {
            headers: { "Content-Type": "multipart/form-data" },
        });
        return res.data; // { ok, messageId, attachments }
    },

    // --- AI file search (global) ---
    aiFindChatFilesGlobal: (query) => {
        if (!query || !query.trim()) throw new Error("query is required");
        return jwtAxios
            .post("/ai/find-chat-files-global", { query })
            .then((r) => r.data); // { summary: string, files: AiChatFileItem[] }
    },
};
