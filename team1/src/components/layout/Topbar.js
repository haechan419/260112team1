import React, { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import useCustomLogin from "../../hooks/useCustomLogin";
import { useFloatingAI } from "../../context/FloatingAIContext";
import "../../styles/layout.css";
import NotificationBell from "../common/NotificationBell";
import ChatDrawer from "../chat/ChatDrawer";
import { chatApi } from "../../api/chatApi";
import FloatingAI from "../../pages/FloatingAI"; //


export default function Topbar({ onMenuClick }) {
    const navigate = useNavigate();
    const { loginState, doLogout } = useCustomLogin();
    const { setOpen: openAI } = useFloatingAI();

    const [chatOpen, setChatOpen] = useState(false);
    const [activeRoomId, setActiveRoomId] = useState(null);

    const [rooms, setRooms] = useState([]);
    const [roomsOpen, setRoomsOpen] = useState(false);

    // ✅ rooms=0일 때 NewChatModal 자동 오픈
    const [autoOpenNewChat, setAutoOpenNewChat] = useState(false);

    const handleLogout = () => {
        alert("로그아웃 성공.");
        doLogout();
        navigate("/");
    };

    const buildRoomTitle = useCallback((r) => {
        const partner = (r?.partnerName ?? "").toString().trim();
        if (partner && partner.toLowerCase() !== "null") return partner;

        const t = (r?.title ?? r?.name ?? "").toString().trim();
        if (t && t.toLowerCase() !== "null") return t;

        const rid = r?.roomId ?? r?.id;
        return `Room ${rid ?? "?"}`;
    }, []);

    const loadRooms = useCallback(async () => {
        try {
            const data = await chatApi.getRooms();
            const list = Array.isArray(data) ? data : [];
            setRooms(list);
            return list;
        } catch (e) {
            console.error("❌ rooms fetch failed", e);
            setRooms([]);
            return [];
        }
    }, []);

    useEffect(() => {
        if (!loginState?.employeeNo) return;
        loadRooms();
    }, [loginState?.employeeNo, loadRooms]);

    const openRoom = (roomId) => {
        setActiveRoomId(String(roomId));
        setChatOpen(true);
        setRoomsOpen(false);
        setAutoOpenNewChat(false);
    };

    return (
        <>
            <header className="topbar">
                <div className="topbar-left">
                    {/* 햄버거 메뉴 버튼 (모바일) */}
                    <button
                        className="hamburger-btn"
                        onClick={onMenuClick}
                        aria-label="Toggle menu"
                        title="메뉴"
                        type="button"
                    >
                        <span></span>
                        <span></span>
                        <span></span>
                    </button>
                    <button
                        className="ai-topbar-btn"
                        onClick={() => openAI(true)}
                        aria-label="Open AI assistant"
                        title="AI Assistant"
                        type="button"
                    >
                        AI
                    </button>
                </div>


                <div className="topbar-right">
          <div className="user-profile">
            <div className="avatar-circle">
              {loginState?.thumbnailUrl || loginState?.profileImageUrl ? (
                <img
                  src={`http://localhost:8080${
                    loginState.thumbnailUrl || loginState.profileImageUrl
                  }`}
                  alt="프로필 이미지"
                  style={{
                    width: "100%",
                    height: "100%",
                    objectFit: "cover",
                  }}
                />
                            ) : (
                                <span style={{ fontSize: "18px" }}>👤</span>
                            )}

                        </div>
                        <div className="user-info">
                            <div className="user-name">{loginState.name || "사용자"}님</div>
                            <div className="user-dept">{loginState.departmentName || "부서없음"}</div>
                        </div>
                    </div>

                    <button className="logout-btn" onClick={handleLogout}>
                        로그아웃
                    </button>

                    <div style={{ marginLeft: "10px", display: "flex", alignItems: "center" }}>
                        <NotificationBell />
                    </div>

                    {/* 💬 버튼 */}
                    <div style={{ position: "relative" }}>
                        <button
                            className="topIconBtn"
                            onClick={async () => {
                                // ✅ 채팅창 열려있으면 팝오버는 안 띄우고 닫기만
                                if (chatOpen) {
                                    setRoomsOpen(false);
                                    return;
                                }

                                const list = await loadRooms();

                                // ✅ rooms가 0이면: 팝오버 대신 "바로 채팅창 + NewChatModal"
                                if (list.length === 0) {
                                    setRoomsOpen(false);
                                    setChatOpen(true);
                                    setActiveRoomId(null);
                                    setAutoOpenNewChat(true);
                                    return;
                                }

                                // rooms가 있으면: 팝오버 토글
                                setAutoOpenNewChat(false);
                                setRoomsOpen((v) => !v);
                            }}
                            aria-label="Open chat"
                            title="Chat"
                            type="button"
                        >
                            💬
                        </button>

                        {roomsOpen && (
                            <div className="chatRoomsPopover">
                                {rooms.length === 0 ? (
                                    <div className="chatRoomsEmpty">채팅방 없음</div>
                                ) : (
                                    rooms.map((r) => {
                                        const rid = r.roomId ?? r.id;
                                        const label = buildRoomTitle(r);

                                        return (
                                            <button
                                                key={rid}
                                                className="chatRoomItem"
                                                onClick={() => openRoom(rid)}
                                                type="button"
                                                title={label}
                                            >
                                                {label}
                                            </button>
                                        );
                                    })
                                )}
                            </div>
                        )}
                    </div>
                </div>
            </header>
            
             {/*한해찬*/}
            <FloatingAI
                roomId={activeRoomId}
                onOpenRoom={(rid) => {
                    setActiveRoomId(String(rid));
                    setChatOpen(true);
                    setRoomsOpen(false);
                    setAutoOpenNewChat(false);
                }}
            />



            <ChatDrawer
                open={chatOpen}
                onClose={() => {
                    setChatOpen(false);
                    setAutoOpenNewChat(false);
                }}
                roomId={activeRoomId}
                autoOpenNewChat={autoOpenNewChat}
                onChangeRoom={(rid) => {
                    console.log("[TOPBAR] onChangeRoom =", rid);
                    setActiveRoomId(String(rid));
                    setChatOpen(true);
                    setRoomsOpen(false);
                    setAutoOpenNewChat(false);
                    loadRooms();
                }}
                onRoomsChanged={() => loadRooms()}
            />
        </>
    );
}
