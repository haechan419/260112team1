import "../../styles/kakaoChat.css";
import { chatApi } from "../../api/chatApi";

export default function RoomList({ rooms, selectedRoomId, onSelect, onDeleted }) {
    if (!rooms || rooms.length === 0) {
        return <div className="kcEmpty">참여 중인 채팅방이 없습니다.</div>;
    }

    const handleDelete = async (rid) => {
        const ok = window.confirm("이 채팅방을 목록에서 삭제(나가기) 할까요?");
        if (!ok) return;

        try {
            await chatApi.deleteRoom(rid);
            // ✅ 부모(ChatPanel)에서 rooms 갱신/선택 변경/WS 구독 정리까지 처리
            onDeleted?.(String(rid));
        } catch (err) {
            alert(err?.response?.data?.message || err.message || "삭제 실패");
        }
    };

    const handleSelect = (rid) => {
        onSelect?.(String(rid));
    };

    return (
        <div className="kcRoomList">
            {rooms.map((r) => {
                const rid = String(r.roomId ?? r.id);
                const active = rid === String(selectedRoomId);
                const unread = Number(r.unreadCount ?? 0);

                const title = (r.partnerName || r.title || "").trim() || "(알 수 없음)";
                const preview = (r.lastContent || "…").trim();

                return (
                    // ✅ 바깥은 div (button 중첩 방지)
                    <div
                        key={rid}
                        className={`kcRoomItem ${active ? "active" : ""}`}
                        role="button"
                        tabIndex={0}
                        onClick={(e) => {
                            e.preventDefault();
                            e.stopPropagation();
                            handleSelect(rid);
                        }}
                        onKeyDown={(e) => {
                            // ✅ Enter/Space 둘 다 클릭처럼
                            if (e.key === "Enter" || e.key === " ") {
                                e.preventDefault();
                                e.stopPropagation();
                                handleSelect(rid);
                            }
                        }}
                    >
                        <div className="kcRoomAvatar">{title?.[0] || "D"}</div>

                        <div className="kcRoomMid">
                            <div className="kcRoomName">{title}</div>
                            <div className="kcRoomPreview">{preview}</div>
                        </div>

                        <div className="kcRoomRight">
                            {unread > 0 && <div className="kcBadge">{unread}</div>}

                            {/* ✅ 삭제 버튼만 button 유지 */}
                            <button
                                type="button"
                                className="kcRoomDeleteBtn"
                                onClick={(e) => {
                                    e.preventDefault();
                                    e.stopPropagation();
                                    handleDelete(rid);
                                }}
                                aria-label="Delete room"
                                title="삭제/나가기"
                            >
                                ✕
                            </button>
                        </div>
                    </div>
                );
            })}
        </div>
    );
}
