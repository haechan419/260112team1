import { useEffect, useMemo, useRef, useState, useCallback } from "react";
import { chatApi } from "../../api/chatApi";
import RoomList from "./RoomList";
import MessageList from "./MessageList";
import MessageInput from "./MessageInput";
import "../../styles/chatPanel.css";
import {
    connectChatSocket,
    disconnectChatSocket,
    subscribeRoom,
    unsubscribeRoom,
    subscribeRooms,
    sendRoomMessage,
} from "../../ws/chatSocket";

export default function ChatPanel({ roomId }) {
    const prevRoomIdRef = useRef(null);
    const selectedRoomIdRef = useRef(null);

    const [otherLastReadMessageId, setOtherLastReadMessageId] = useState(null);

    const [rooms, setRooms] = useState([]);
    const [selectedRoomId, setSelectedRoomId] = useState(null);
    const [messages, setMessages] = useState([]);
    const [err, setErr] = useState("");

    // ✅ 중복 방지용
    const seenIdsRef = useRef(new Set());

    const selectedRoom = useMemo(() => {
        if (!selectedRoomId) return null;
        return rooms.find((r) => String(r.roomId ?? r.id) === String(selectedRoomId));
    }, [rooms, selectedRoomId]);

    const roomTitle = selectedRoom?.partnerName || "(알 수 없음)";

    const latestMessageId = useMemo(() => {
        if (!messages?.length) return null;
        return Math.max(...messages.map((m) => m.messageId ?? m.id));
    }, [messages]);

    const toMillis = (v) => {
        if (!v) return 0;
        if (typeof v === "number") return v;
        const t = Date.parse(v);
        return Number.isNaN(t) ? 0 : t;
    };

    const loadRooms = useCallback(async () => {
        try {
            const data = await chatApi.getRooms();
            const raw = Array.isArray(data) ? data : [];

            const sorted = [...raw].sort((a, b) => {
                const atA =
                    toMillis(a.lastCreatedAt) ||
                    toMillis(a.lastMessageCreatedAt) ||
                    toMillis(a.updatedAt);

                const atB =
                    toMillis(b.lastCreatedAt) ||
                    toMillis(b.lastMessageCreatedAt) ||
                    toMillis(b.updatedAt);

                return atB - atA;
            });

            setRooms(sorted);

            setSelectedRoomId((prev) => {
                if (roomId != null) return String(roomId);
                if (prev) return prev;
                const first = sorted.length ? (sorted[0].roomId ?? sorted[0].id) : null;
                return first != null ? String(first) : null;
            });
        } catch (e) {
            setErr(e?.response?.data?.message || e.message || "방 목록 로딩 실패");
        }
    }, [roomId]);

    const loadMessagesOnce = useCallback(async (rid) => {
        if (!rid) return;
        try {
            const data = await chatApi.getMessages(rid, { limit: 30 });
            const list = Array.isArray(data) ? data : [];

            // ✅ attachments 포함 그대로 유지 (서버가 내려주면 그대로 담김)
            setMessages(list);

            // ✅ seenIds 갱신
            const next = new Set();
            for (const m of list) next.add(String(m.messageId ?? m.id));
            seenIdsRef.current = next;
        } catch (e) {
            setErr(e?.response?.data?.message || e.message || "메시지 로딩 실패");
            setMessages([]);
            seenIdsRef.current = new Set();
        }
    }, []);

    const loadRoomMeta = useCallback(async (rid) => {
        if (!rid) return;
        try {
            const meta = await chatApi.getRoomMeta(rid);
            setOtherLastReadMessageId(meta?.otherLastReadMessageId ?? null);
        } catch {
            setOtherLastReadMessageId(null);
        }
    }, []);

    // ✅ 메시지 요약 텍스트 만들기 (첨부-only면 📎 파일)
    const summarizeIncoming = useCallback((incoming) => {
        const text = (incoming?.content ?? "").trim();
        if (text) return text;

        const hasAtt = Array.isArray(incoming?.attachments) && incoming.attachments.length > 0;
        if (hasAtt) {
            if (incoming.attachments.length === 1) return "📎 파일 1개";
            return `📎 파일 ${incoming.attachments.length}개`;
        }
        return "…";
    }, []);

    // ✅ rooms를 로컬에서 즉시 갱신 + 맨 위로 올림
    const bumpRoomByIncoming = useCallback(
        (incoming) => {
            const rid = String(incoming.roomId);
            const createdAt = incoming.createdAt ?? new Date().toISOString();
            const lastContent = summarizeIncoming(incoming);

            setRooms((prev) => {
                const next = prev.map((r) => {
                    const rId = String(r.roomId ?? r.id);
                    if (rId !== rid) return r;

                    return {
                        ...r,
                        lastContent,
                        lastCreatedAt: createdAt, // ✅ 정렬 키
                    };
                });

                next.sort((a, b) => {
                    const atA =
                        toMillis(a.lastCreatedAt) ||
                        toMillis(a.lastMessageCreatedAt) ||
                        toMillis(a.updatedAt);

                    const atB =
                        toMillis(b.lastCreatedAt) ||
                        toMillis(b.lastMessageCreatedAt) ||
                        toMillis(b.updatedAt);

                    return atB - atA;
                });

                return next;
            });
        },
        [summarizeIncoming]
    );

    // 1) 최초 rooms 로딩
    useEffect(() => {
        loadRooms();
    }, [loadRooms]);

    // 2) 부모 roomId 바뀌면 선택 반영
    useEffect(() => {
        if (roomId == null) return;
        setSelectedRoomId(String(roomId));
    }, [roomId]);

    // 3) WS 연결 + rooms 전역 이벤트 구독 (한 번만)
    useEffect(() => {
        const jwt = localStorage.getItem("jwt");
        if (!jwt) return;

        connectChatSocket(jwt);

        subscribeRooms((evt) => {
            console.log("📩 rooms evt", evt);
            if (evt?.type === "ROOMS_CHANGED") loadRooms();
        });

        return () => {
            disconnectChatSocket();
        };
    }, [loadRooms]);

    // 4) 방 선택 시: REST 1회 로딩 + WS room 구독
    useEffect(() => {
        if (!selectedRoomId) return;

        // ✅ 방 바뀌면 seen 초기화 (중복 방지 set)
        seenIdsRef.current = new Set();

        const prev = prevRoomIdRef.current;
        if (prev && String(prev) !== String(selectedRoomId)) {
            unsubscribeRoom(prev);
        }
        prevRoomIdRef.current = selectedRoomId;
        selectedRoomIdRef.current = selectedRoomId;

        loadMessagesOnce(selectedRoomId);
        loadRoomMeta(selectedRoomId);

        subscribeRoom(selectedRoomId, (incoming) => {
            // ✅ 서버가 type을 같이 보낼 수도 있음
            // MESSAGE 타입만 처리 (없으면 그냥 처리)
            if (incoming?.type && incoming.type !== "MESSAGE") return;

            const msgId = String(incoming.messageId ?? incoming.id);
            if (!msgId) return;

            if (seenIdsRef.current.has(msgId)) return;
            seenIdsRef.current.add(msgId);

            const msg = {
                messageId: incoming.messageId ?? incoming.id,
                roomId: incoming.roomId ?? selectedRoomIdRef.current,
                senderId: incoming.senderId,
                content: incoming.content ?? "",
                createdAt: incoming.createdAt,
                // ✅ 핵심: attachments 그대로 붙이기
                attachments: Array.isArray(incoming.attachments) ? incoming.attachments : [],
            };

            setMessages((prevMsgs) => [...prevMsgs, msg]);

            // ✅ 방 리스트 즉시 업데이트(첨부-only면 📎 파일)
            bumpRoomByIncoming(msg);
        });

        return () => {
            unsubscribeRoom(selectedRoomId);
        };
    }, [selectedRoomId, loadMessagesOnce, loadRoomMeta, bumpRoomByIncoming]);

    // 5) 읽음 처리
    useEffect(() => {
        if (!selectedRoomId || !latestMessageId) return;

        chatApi.updateRead(selectedRoomId, latestMessageId).catch(() => {});
        setRooms((prev) =>
            prev.map((r) => {
                const rid = String(r.roomId ?? r.id);
                return rid === String(selectedRoomId) ? { ...r, unreadCount: 0 } : r;
            })
        );
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [latestMessageId, selectedRoomId]);

    // 6) 전송: WS publish (텍스트만)
    const handleSend = useCallback(
        (text) => {
            if (!selectedRoomId) return;
            setErr("");

            const ok = sendRoomMessage(selectedRoomId, text);
            if (!ok) {
                setErr("소켓 연결이 끊겨서 전송 실패");
                return;
            }
        },
        [selectedRoomId]
    );

    return (
        <div className="chatPanelShell">
            <aside className="chatPanelLeft">
                <div className="chatPanelSearch">
                    <input placeholder="대화 검색 (MVP)" />
                </div>

                <RoomList
                    rooms={rooms}
                    selectedRoomId={selectedRoomId}
                    onSelect={setSelectedRoomId}
                    onDeleted={(deletedId) => {
                        setRooms((prev) =>
                            prev.filter((r) => String(r.roomId ?? r.id) !== String(deletedId))
                        );

                        if (String(selectedRoomId) === String(deletedId)) {
                            const remain = rooms.filter(
                                (r) => String(r.roomId ?? r.id) !== String(deletedId)
                            );
                            const next = remain.length ? (remain[0].roomId ?? remain[0].id) : null;
                            setSelectedRoomId(next != null ? String(next) : null);
                            setMessages([]);
                            seenIdsRef.current = new Set();
                        }
                    }}
                />
            </aside>

            <main className="chatPanelRight">
                <div className="chatPanelTop">
                    <div className="chatPanelRoomTitle">
                        {selectedRoomId ? roomTitle : "방을 선택하세요"}
                    </div>
                    <button className="miniBtn" onClick={loadRooms}>
                        ↻
                    </button>
                </div>

                {err && <div className="chatErr">{err}</div>}

                <MessageList messages={messages} otherLastReadMessageId={otherLastReadMessageId} />
                <MessageInput
                    disabled={!selectedRoomId}
                    roomId={selectedRoomId}
                    onSend={handleSend}
                />

            </main>
        </div>
    );
}
