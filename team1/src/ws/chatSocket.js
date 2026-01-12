// src/ws/chatSocket.js
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

let client = null;
let connecting = false;
let pingSubscribed = false;

// 실제 STOMP subscription 객체들
const roomSubs = new Map(); // roomId -> sub
let roomsSub = null;

// ✅ rooms(내 채팅방 리스트 이벤트) 구독 대기열
let roomsHandlers = [];
let roomsSubscribeRequested = false;

// ✅ room(특정 방 메시지) 구독 대기열
const roomHandlers = new Map();           // roomId(string) -> Set<fn>
const roomSubscribeRequested = new Set(); // roomId(string) set

export function connectChatSocket(jwt, onPing) {
    console.log("🔥 connectChatSocket CALLED", {
        hasJwt: Boolean(jwt),
        jwtPrefix: jwt?.slice?.(0, 20),
        connected: client?.connected,
        connecting,
    });

    if (!jwt) {
        console.warn("⛔ STOMP connect skipped: jwt is null");
        return null;
    }

    if (client?.connected) return client;
    if (connecting) return client;

    connecting = true;

    client = new Client({
        webSocketFactory: () => new SockJS("http://localhost:8080/ws-chat"),
        connectHeaders: { Authorization: `Bearer ${jwt}` },
        reconnectDelay: 3000,
        debug: (msg) => console.log("[STOMP]", msg),

        onConnect: () => {
            connecting = false;
            console.log("✅ STOMP connected");

            // 1) rooms 구독 (내 방 리스트 이벤트)
            if (roomsSubscribeRequested && !roomsSub) {
                console.log("✅ subscribing /user/queue/rooms ...");
                roomsSub = client.subscribe("/user/queue/rooms", (msg) => {
                    let body = msg.body;
                    try { body = JSON.parse(msg.body); } catch {}
                    for (const h of roomsHandlers) h?.(body);
                });
            }

            // 2) room 구독 (연결 전에 요청된 것들 붙이기)
            for (const roomId of roomSubscribeRequested) {
                const key = String(roomId);
                if (roomSubs.has(key)) continue;

                const handlers = roomHandlers.get(key);
                if (!handlers || handlers.size === 0) continue;

                console.log(`✅ subscribing /topic/room/${key} (deferred) ...`);
                const sub = client.subscribe(`/topic/room/${key}`, (msg) => {
                    let body = msg.body;
                    try { body = JSON.parse(msg.body); } catch {}
                    for (const fn of handlers) fn?.(body);
                });

                roomSubs.set(key, sub);
            }

            // 3) ping (선택)
            if (!pingSubscribed) {
                pingSubscribed = true;
                client.subscribe("/user/queue/ping", (msg) => onPing?.(msg.body));
                try { client.publish({ destination: "/app/ping", body: "" }); } catch {}
            }
        },

        onWebSocketError: (evt) => {
            console.error("🧨 WebSocket error", evt);
        },

        onWebSocketClose: (evt) => {
            connecting = false;
            console.log("🔌 WebSocket closed", evt?.code, evt?.reason);

            // 실제 sub 객체 정리 (요청/핸들러는 유지해야 재연결 시 자동복구됨)
            try { for (const sub of roomSubs.values()) sub?.unsubscribe?.(); } catch {}
            roomSubs.clear();

            try { roomsSub?.unsubscribe?.(); } catch {}
            roomsSub = null;

            // 끊기면 ping도 다시 붙일 수 있게
            pingSubscribed = false;
        },

        onStompError: (frame) => {
            console.error("❌ STOMP error", frame.headers["message"], frame.body);
        },
    });

    client.activate();
    return client;
}

export function disconnectChatSocket() {
    try { for (const sub of roomSubs.values()) sub?.unsubscribe?.(); } catch {}
    roomSubs.clear();

    try { roomsSub?.unsubscribe?.(); } catch {}
    roomsSub = null;

    roomsSubscribeRequested = false;
    roomsHandlers = [];

    roomSubscribeRequested.clear();
    roomHandlers.clear();

    pingSubscribed = false;
    connecting = false;

    if (client) {
        try { client.deactivate(); } catch {}
        client = null;
    }
}

// rooms(내 방 리스트 이벤트) 구독
export function subscribeRooms(onEvent) {
    if (typeof onEvent === "function") roomsHandlers.push(onEvent);
    roomsSubscribeRequested = true;

    if (client?.connected && !roomsSub) {
        console.log("✅ subscribing /user/queue/rooms (immediate) ...");
        roomsSub = client.subscribe("/user/queue/rooms", (msg) => {
            let body = msg.body;
            try { body = JSON.parse(msg.body); } catch {}
            for (const h of roomsHandlers) h?.(body);
        });
    }

    return roomsSub;
}

export function unsubscribeRooms() {
    try { roomsSub?.unsubscribe?.(); } catch {}
    roomsSub = null;
    roomsSubscribeRequested = false;
    roomsHandlers = [];
}

// room(특정 채팅방 메시지) 구독
export function subscribeRoom(roomId, onMsg) {
    const key = String(roomId);

    // handler 등록(연결 전에도 등록 가능)
    if (typeof onMsg === "function") {
        let set = roomHandlers.get(key);
        if (!set) {
            set = new Set();
            roomHandlers.set(key, set);
        }
        set.add(onMsg);
    }

    // "이 방 구독 원함" 표시
    roomSubscribeRequested.add(key);

    // 연결 전이면 큐잉
    if (!client?.connected) {
        console.warn("⛔ subscribeRoom queued: not connected yet");
        return null;
    }

    // 이미 구독 중이면 반환
    if (roomSubs.has(key)) return roomSubs.get(key);

    // 즉시 구독
    const handlers = roomHandlers.get(key);
    const sub = client.subscribe(`/topic/room/${key}`, (msg) => {
        let body = msg.body;
        try { body = JSON.parse(msg.body); } catch {}
        if (handlers) for (const fn of handlers) fn?.(body);
    });

    roomSubs.set(key, sub);
    return sub;
}

/**
 * unsubscribeRoom(roomId)
 * - roomId만: 완전 해제(요청/핸들러/sub 제거)
 * unsubscribeRoom(roomId, onMsg)
 * - onMsg까지: 특정 핸들러만 제거(남으면 유지)
 */
export function unsubscribeRoom(roomId, onMsg) {
    const key = String(roomId);

    if (typeof onMsg === "function") {
        const set = roomHandlers.get(key);
        if (set) {
            set.delete(onMsg);
            if (set.size === 0) {
                roomHandlers.delete(key);
                roomSubscribeRequested.delete(key);

                const sub = roomSubs.get(key);
                if (sub) {
                    try { sub.unsubscribe(); } catch {}
                    roomSubs.delete(key);
                }
            }
        }
        return;
    }

    roomHandlers.delete(key);
    roomSubscribeRequested.delete(key);

    const sub = roomSubs.get(key);
    if (!sub) return;
    try { sub.unsubscribe(); } catch {}
    roomSubs.delete(key);
}

export function sendRoomMessage(roomId, content) {
    if (!client?.connected) {
        console.warn("⛔ sendRoomMessage skipped: not connected");
        return false;
    }

    const trimmed = (content ?? "").trim();
    if (!trimmed) return false;

    client.publish({
        destination: "/app/chat/send",
        body: JSON.stringify({ roomId: Number(roomId), content: trimmed }),
    });

    return true;
}
