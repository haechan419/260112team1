// ChatPanel.jsx
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
    unsubscribeRooms,
    sendRoomMessage,
} from "../../ws/chatSocket";

export default function ChatPanel({ roomId, scrollToMessageId }) {
    const prevRoomIdRef = useRef(null);
    const selectedRoomIdRef = useRef(null);

    const [otherLastReadMessageId, setOtherLastReadMessageId] = useState(null);

    const [rooms, setRooms] = useState([]);
    const [selectedRoomId, setSelectedRoomId] = useState(null);
    const [messages, setMessages] = useState([]);
    const [err, setErr] = useState("");

    // ✅ 중복 방지(메시지ID)
    const seenIdsRef = useRef(new Set());

    // ✅ 스크롤 요청 저장
    const scrollReqRef = useRef(null); // { roomId: "18", messageId: "1234" }

    // ===== helpers =====
    const toMillis = useCallback((v) => {
        if (!v) return 0;
        if (typeof v === "number") return v;
        const t = Date.parse(v);
        return Number.isNaN(t) ? 0 : t;
    }, []);

    const normalizeMessages = useCallback((list) => {
        const arr = Array.isArray(list) ? list : [];
        const mapped = arr.map((m) => ({ ...m, messageId: m.messageId ?? m.id }));
        // ✅ 최신이 위 (내림차순)
        mapped.sort((a, b) => (b.messageId ?? 0) - (a.messageId ?? 0));
        return mapped;
    }, []);

    const hasMessageId = useCallback((list, targetId) => {
        const t = String(targetId);
        return (list || []).some((m) => String(m.messageId ?? m.id) === t);
    }, []);

    const scrollToDomMessage = useCallback((targetId) => {
        const id = String(targetId);
        requestAnimationFrame(() => {
            const el = document.getElementById(`msg-${id}`);
            if (!el) return;
            el.scrollIntoView({ behavior: "smooth", block: "center" });
            el.classList.add("chat-msg-highlight");
            setTimeout(() => el.classList.remove("chat-msg-highlight"), 1800);
        });
    }, []);

    // ===== loaders =====
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
                // ✅ 부모가 roomId를 줬으면 그걸 최우선
                if (roomId != null) return String(roomId);
                // ✅ 기존 선택 유지
                if (prev) return prev;
                // ✅ 없으면 첫 방 선택
                const first = sorted.length ? (sorted[0].roomId ?? sorted[0].id) : null;
                return first != null ? String(first) : null;
            });
        } catch (e) {
            setErr(e?.response?.data?.message || e.message || "방 목록 로딩 실패");
            setRooms([]);
        }
    }, [roomId, toMillis]);

    const loadMessagesOnce = useCallback(
        async (rid, opts = {}) => {
            if (!rid) return [];
            try {
                const data = await chatApi.getMessages(rid, { limit: 30, ...opts });
                const list = normalizeMessages(data);

                setMessages(list);

                // ✅ seenIds 갱신
                const next = new Set();
                for (const m of list) next.add(String(m.messageId ?? m.id));
                seenIdsRef.current = next;

                return list;
            } catch (e) {
                setErr(e?.response?.data?.message || e.message || "메시지 로딩 실패");
                setMessages([]);
                seenIdsRef.current = new Set();
                return [];
            }
        },
        [normalizeMessages]
    );

    const loadRoomMeta = useCallback(async (rid) => {
        if (!rid) return;
        try {
            const meta = await chatApi.getRoomMeta(rid);
            setOtherLastReadMessageId(meta?.otherLastReadMessageId ?? null);
        } catch {
            setOtherLastReadMessageId(null);
        }
    }, []);

    // ✅ rooms 업데이트(좌측 리스트)용: 들어온 메시지로 room 올리기
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
                        lastCreatedAt: createdAt,
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
        [summarizeIncoming, toMillis]
    );

    // ✅ 타깃 메시지 찾을 때까지 older fetch 반복 후 스크롤
    const ensureMessageLoadedAndScroll = useCallback(
        async (rid, targetMessageId) => {
            if (!rid || !targetMessageId) return;

            const targetId = String(targetMessageId);

            // 최신 상태 기준으로 확인하려고, 현재 messages를 먼저 복사
            let current = (Array.isArray(messages) ? messages : []).slice();

            if (hasMessageId(current, targetId)) {
                scrollToDomMessage(targetId);
                return;
            }

            let tries = 0;
            const MAX_TRIES = 8;
            const PAGE_SIZE = 50;

            while (tries < MAX_TRIES) {
                tries += 1;

                const oldest = current.length
                    ? String(current[current.length - 1].messageId ?? current[current.length - 1].id)
                    : null;

                const older = await chatApi.getMessages(rid, {
                    limit: PAGE_SIZE,
                    ...(oldest ? { beforeMessageId: oldest } : {}),
                });

                const olderList = normalizeMessages(older);
                if (!olderList.length) break;

                const mergedMap = new Map();
                for (const m of [...current, ...olderList]) {
                    mergedMap.set(String(m.messageId ?? m.id), { ...m, messageId: m.messageId ?? m.id });
                }

                current = Array.from(mergedMap.values()).sort((a, b) => (b.messageId ?? 0) - (a.messageId ?? 0));
                setMessages(current);

                // seenIds 갱신
                const nextSeen = new Set();
                for (const m of current) nextSeen.add(String(m.messageId ?? m.id));
                seenIdsRef.current = nextSeen;

                if (hasMessageId(current, targetId)) {
                    requestAnimationFrame(() => scrollToDomMessage(targetId));
                    return;
                }

                const newOldest = current.length
                    ? String(current[current.length - 1].messageId ?? current[current.length - 1].id)
                    : null;

                if (newOldest === oldest) break;
            }

            setErr((prev) => prev || "해당 메시지를 찾지 못했습니다. (더 오래된 메시지일 수 있음)");
        },
        [messages, hasMessageId, normalizeMessages, scrollToDomMessage]
    );

    // ===== derived =====
    const selectedRoom = useMemo(() => {
        if (!selectedRoomId) return null;
        return rooms.find((r) => String(r.roomId ?? r.id) === String(selectedRoomId));
    }, [rooms, selectedRoomId]);

    const roomTitle = selectedRoom?.partnerName || "(알 수 없음)";

    const latestMessageId = useMemo(() => {
        if (!messages?.length) return null;
        return Math.max(...messages.map((m) => m.messageId ?? m.id));
    }, [messages]);

    // ===== effects =====

    // 1) 최초 rooms 로딩 + 부모 roomId 바뀌면 선택 반영
    useEffect(() => {
        loadRooms();
    }, [loadRooms]);

    useEffect(() => {
        if (roomId == null) return;
        setSelectedRoomId(String(roomId));
    }, [roomId]);

    // 2) 스크롤 타깃이 들어오면 요청 저장
    useEffect(() => {
        if (scrollToMessageId == null) return;
        const rid = roomId != null ? String(roomId) : selectedRoomIdRef.current;
        if (!rid) return;
        scrollReqRef.current = { roomId: String(rid), messageId: String(scrollToMessageId) };
    }, [scrollToMessageId, roomId]);

    // ✅ 3) WS 연결 + rooms 이벤트 구독은 “딱 1번만”
    const loadRoomsRef = useRef(loadRooms);
    useEffect(() => {
        loadRoomsRef.current = loadRooms;
    }, [loadRooms]);

    useEffect(() => {
        const jwt = localStorage.getItem("jwt");
        if (!jwt) return;

        connectChatSocket(jwt);

        const handler = (evt) => {
            // 서버에서 ROOM_CHANGED 같은 이벤트 보내면 여기서 즉시 rooms 갱신
            if (evt?.type === "ROOMS_CHANGED" || evt?.type === "ROOM_CHANGED") {
                loadRoomsRef.current?.();
            }
        };

        subscribeRooms(handler);

        return () => {
            // ✅ 여기서만 끊음(언마운트 시)
            unsubscribeRooms?.();
            disconnectChatSocket();
        };
    }, []);

    // ✅ 4) 방 구독/해제: selectedRoomId 변경 때만
    const onIncomingMessage = useCallback(
        (incoming) => {
            if (!incoming) return;
            if (incoming?.type && incoming.type !== "MESSAGE") return;

            const currentRoom = selectedRoomIdRef.current;
            const incomingRoomId = String(incoming.roomId ?? "");
            if (!currentRoom) return;

            // ✅ 다른 방 메시지는 “리스트만 갱신”하고 메시지창은 건드리지 않음
            if (incomingRoomId && incomingRoomId !== String(currentRoom)) {
                bumpRoomByIncoming(incoming);
                return;
            }

            const msgId = String(incoming.messageId ?? incoming.id);
            if (!msgId) return;
            if (seenIdsRef.current.has(msgId)) return;

            seenIdsRef.current.add(msgId);

            const msg = {
                messageId: incoming.messageId ?? incoming.id,
                roomId: incoming.roomId ?? currentRoom,
                senderId: incoming.senderId,
                content: incoming.content ?? "",
                createdAt: incoming.createdAt,
                attachments: Array.isArray(incoming.attachments) ? incoming.attachments : [],
            };

            setMessages((prevMsgs) => normalizeMessages([...prevMsgs, msg]));
            bumpRoomByIncoming(msg);

            // ✅ “특정 메시지로 스크롤” 요청이 남아있으면 시도
            const req = scrollReqRef.current;
            if (req && String(req.roomId) === String(currentRoom)) {
                requestAnimationFrame(() => {
                    scrollToDomMessage(req.messageId);
                    scrollReqRef.current = null;
                });
            }
        },
        [bumpRoomByIncoming, normalizeMessages, scrollToDomMessage]
    );

    useEffect(() => {
        if (!selectedRoomId) return;

        // ✅ ref 갱신
        selectedRoomIdRef.current = selectedRoomId;

        // ✅ 방 바뀌면 seen 초기화
        seenIdsRef.current = new Set();

        // ✅ 이전 방 구독 해제
        const prev = prevRoomIdRef.current;
        if (prev && String(prev) !== String(selectedRoomId)) {
            unsubscribeRoom(prev, onIncomingMessage);
        }
        prevRoomIdRef.current = selectedRoomId;

        // ✅ REST 1회 로딩 + meta 로딩
        (async () => {
            await loadMessagesOnce(selectedRoomId);
            await loadRoomMeta(selectedRoomId);

            // ✅ 로딩 직후 스크롤 요청 처리
            const req = scrollReqRef.current;
            if (req && String(req.roomId) === String(selectedRoomId)) {
                await ensureMessageLoadedAndScroll(selectedRoomId, req.messageId);
                scrollReqRef.current = null;
            }
        })();

        // ✅ WS 구독
        subscribeRoom(selectedRoomId, onIncomingMessage);

        return () => {
            unsubscribeRoom(selectedRoomId, onIncomingMessage);
        };
    }, [selectedRoomId, onIncomingMessage, loadMessagesOnce, loadRoomMeta, ensureMessageLoadedAndScroll]);

    // 5) 읽음 처리(REST)
    useEffect(() => {
        if (!selectedRoomId || !latestMessageId) return;

        chatApi.updateRead(selectedRoomId, latestMessageId).catch(() => {});
        setRooms((prev) =>
            prev.map((r) => {
                const rid = String(r.roomId ?? r.id);
                return rid === String(selectedRoomId) ? { ...r, unreadCount: 0 } : r;
            })
        );
    }, [latestMessageId, selectedRoomId]);

    // 6) 전송: WS publish + (보험) REST 재로딩
    const handleSend = useCallback(
        async (text) => {
            if (!selectedRoomId) return;
            setErr("");

            const ok = sendRoomMessage(selectedRoomId, text);
            if (!ok) {
                setErr("소켓 연결이 끊겨서 전송 실패");
                return;
            }

            // ✅ 보험: 서버가 sender에게 echo 안 해도 바로 보이게
            // 너무 빠르면 서버 저장 전에 당길 수 있으니 살짝 딜레이
            setTimeout(() => {
                loadMessagesOnce(selectedRoomId);
                loadRooms();
            }, 120);
        },
        [selectedRoomId, loadMessagesOnce, loadRooms]
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
                        setRooms((prev) => prev.filter((r) => String(r.roomId ?? r.id) !== String(deletedId)));

                        if (String(selectedRoomId) === String(deletedId)) {
                            const remain = rooms.filter((r) => String(r.roomId ?? r.id) !== String(deletedId));
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
                    <div className="chatPanelRoomTitle">{selectedRoomId ? roomTitle : "방을 선택하세요"}</div>
                    <button className="miniBtn" onClick={loadRooms}>
                        ↻
                    </button>
                </div>

                {err && <div className="chatErr">{err}</div>}

                <div className="kcChatCol">
                    <MessageList messages={messages} otherLastReadMessageId={otherLastReadMessageId} />

                    <MessageInput disabled={!selectedRoomId} roomId={selectedRoomId} onSend={handleSend} />
                </div>
            </main>
        </div>
    );
}
