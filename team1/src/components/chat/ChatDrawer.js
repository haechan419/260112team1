import { useEffect, useState, useCallback } from "react";
import ChatPanel from "./ChatPanel";
import NewChatModal from "./NewChatModal";
import "../../styles/chatDrawer.css";

import { getAuthTokenForRequest } from "../../util/jwtUtil"; // ✅ 추가
import { connectChatSocket, subscribeRooms } from "../../ws/chatSocket";

export default function ChatDrawer({
                                       open, onClose, roomId, onChangeRoom, autoOpenNewChat, onRoomsChanged,
                                   }) {
    const [newChatOpen, setNewChatOpen] = useState(false);

    useEffect(() => {
        if (!open) return;

        const jwt = getAuthTokenForRequest();
        console.log("🧷 ChatDrawer open -> connect socket. jwt?", Boolean(jwt));

        connectChatSocket(jwt, (ping) => console.log("🏓 ping", ping));

        subscribeRooms((evt) => {
            console.log("📩 rooms event", evt);
            // onRoomsChanged?.(); // 필요하면 켜
        });
    }, [open, onRoomsChanged]);

    // (나머지 기존 코드 그대로)
    useEffect(() => {
        if (!open) return;
        if (autoOpenNewChat) setNewChatOpen(true);
    }, [open, autoOpenNewChat]);

    useEffect(() => {
        if (!open) return;
        const onKeyDown = (e) => {
            if (e.key === "Escape") {
                if (newChatOpen) setNewChatOpen(false);
                else onClose?.();
            }
        };
        window.addEventListener("keydown", onKeyDown);
        return () => window.removeEventListener("keydown", onKeyDown);
    }, [open, onClose, newChatOpen]);

    const handleCreated = useCallback(
        (createdRoomId) => {
            console.log("[DRAWER] onCreated roomId=", createdRoomId);
            onChangeRoom?.(createdRoomId);
            setNewChatOpen(false);
            onRoomsChanged?.();
        },
        [onChangeRoom, onRoomsChanged]
    );

    if (!open) return null;

    return (
        <div className="chatOverlay" onMouseDown={onClose}>
            <div className="chatDrawer" onMouseDown={(e) => e.stopPropagation()}>
                <div className="chatDrawerHeader">
                    <div className="chatDrawerTitle">Chat</div>
                    <div className="chatDrawerActions">
                        <button className="chatNewBtn" onClick={() => setNewChatOpen(true)} title="새 채팅" type="button">＋</button>
                        <button className="chatCloseBtn" onClick={onClose} aria-label="Close chat" type="button">✕</button>
                    </div>
                </div>

                <div className="chatDrawerBody">
                    {roomId ? <ChatPanel key={roomId} roomId={roomId} /> : <div className="chatEmpty">대화를 선택하거나 새 채팅을 시작하세요</div>}
                </div>

                <NewChatModal open={newChatOpen} onClose={() => setNewChatOpen(false)} onCreated={handleCreated} />
            </div>
        </div>
    );
}
