import jwtAxios from "../util/jwtUtil";


export const chatApi = {
    // --- user search / create rooms ---
    searchUsers: (q, limit = 20) =>
        jwtAxios.get("/chat/users/search", { params: { q, limit } }).then(r => r.data),

    createDm: (targetUserId) =>
        jwtAxios.post("/chat/rooms/dm", { targetUserId }).then(r => r.data),

    createGroup: (memberUserIds) =>
        jwtAxios.post("/chat/rooms/group", { memberUserIds }).then(r => r.data),

    invite: (roomId, userIds) =>
        jwtAxios.post(`/chat/rooms/${roomId}/invite`, { userIds }).then(r => r.data),

    // --- rooms / messages ---
    getRooms: async () => {
        const res = await jwtAxios.get("/chat/rooms");
        return res.data;
    },

    getMessages: async (roomId, { cursor, limit } = {}) => {
        const params = {};
        if (cursor) params.cursor = cursor;
        if (limit) params.limit = limit;
        const res = await jwtAxios.get(`/chat/rooms/${roomId}/messages`, { params });
        return res.data;
    },

    sendMessage: async (roomId, content) => {
        const res = await jwtAxios.post(`/chat/rooms/${roomId}/messages`, { content });
        return res.data;
    },

    updateRead: async (roomId, lastReadMessageId = null) => {
        const res = await jwtAxios.post(`/chat/rooms/${roomId}/read`, { lastReadMessageId });
        return res.data;
    },

    getRoomMeta: async (roomId) => {
        const res = await jwtAxios.get(`/chat/rooms/${roomId}/meta`);
        return res.data;
    },

    deleteRoom: async (roomId) => {
        const res = await jwtAxios.delete(`/chat/rooms/${roomId}`);
        return res.data;
    },

    uploadAttachments: async (roomId, content, files) => {
        const form = new FormData();
        if (content != null) form.append("content", content); // "" 가능

        if (files && files.length) {
            for (const f of files) form.append("files", f);
        }

        const res = await jwtAxios.post(
            `/chat/rooms/${roomId}/attachments`,
            form,
            {
                headers: { "Content-Type": "multipart/form-data" },
            }
        );
        return res.data; // { ok, messageId, attachments }
    },
};
