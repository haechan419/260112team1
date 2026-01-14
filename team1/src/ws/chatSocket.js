// src/ws/chatSocket.js
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

/**
 * ✅ 목표
 * - connect 1번만 (중복 호출 안전)
 * - 연결 전 subscribe 요청은 "요청/핸들러"만 등록 → onConnect에서 자동 구독
 * - 재연결 시 live subscription만 정리하고(handlers/요청 유지) → 자동 복구
 * - room 메시지 토픽:      /topic/room/{roomId}
 * - room read 토픽:        /topic/room/{roomId}/read
 * - 내 방 리스트 이벤트:     /user/queue/rooms
 * - ping(선택):            /user/queue/ping + /app/ping
 */

const WS_URL = "http://localhost:8080/ws-chat";

let client = null;
let connecting = false;
let connectPromise = null;

let pingSubscribed = false;

// ✅ STOMP subscription 객체들(실제 live sub만)
const roomSubs = new Map(); // roomId -> sub (/topic/room/{roomId})
const roomReadSubs = new Map(); // roomId -> sub (/topic/room/{roomId}/read)
let roomsSub = null;

// ✅ rooms 이벤트 핸들러/요청
let roomsHandlers = [];
let roomsSubscribeRequested = false;

// ✅ room 메시지 핸들러/요청
const roomHandlers = new Map(); // roomId(string) -> Set<fn>
const roomSubscribeRequested = new Set(); // roomId(string)

// ✅ room read 핸들러/요청
const roomReadHandlers = new Map(); // roomId(string) -> Set<fn>
const roomReadSubscribeRequested = new Set(); // roomId(string)

// ─────────────────────────────────────────────────────────────
// internal helpers
// ─────────────────────────────────────────────────────────────
function safeJson(body) {
    try {
        return JSON.parse(body);
    } catch {
        return body;
    }
}

function hasHandlers(map, key) {
    const set = map.get(key);
    return set && set.size > 0;
}

function subscribeIfNeeded({ key, destination, subsMap, handlersMap, label }) {
    if (!client?.connected) return;
    if (subsMap.has(key)) return;

    const handlers = handlersMap.get(key);
    if (!handlers || handlers.size === 0) return;

    console.log(`✅ subscribing ${destination} (${label}) ...`);

    const sub = client.subscribe(destination, (msg) => {
        const body = safeJson(msg.body);
        for (const fn of handlers) {
            try {
                fn?.(body);
            } catch (e) {
                console.error("handler error", e);
            }
        }
    });

    subsMap.set(key, sub);
}

function cleanupLiveSubsOnly() {
    // ✅ 실제 STOMP sub 객체만 정리 (요청/핸들러는 유지 → 재연결 자동복구)
    try {
        for (const sub of roomSubs.values()) sub?.unsubscribe?.();
    } catch {}
    roomSubs.clear();

    try {
        for (const sub of roomReadSubs.values()) sub?.unsubscribe?.();
    } catch {}
    roomReadSubs.clear();

    try {
        roomsSub?.unsubscribe?.();
    } catch {}
    roomsSub = null;

    // ping도 재연결 시 다시 붙이기
    pingSubscribed = false;
}

function resubscribeAll(onPing) {
    if (!client?.connected) return;

    // 1) rooms 구독
    if (roomsSubscribeRequested && !roomsSub) {
        console.log("✅ subscribing /user/queue/rooms ...");
        roomsSub = client.subscribe("/user/queue/rooms", (msg) => {
            const body = safeJson(msg.body);
            for (const h of roomsHandlers) {
                try {
                    h?.(body);
                } catch (e) {
                    console.error("rooms handler error", e);
                }
            }
        });
    }

    // 2) room 메시지 구독 (요청된 방들)
    for (const roomId of roomSubscribeRequested) {
        const key = String(roomId);
        if (!hasHandlers(roomHandlers, key)) continue;

        subscribeIfNeeded({
            key,
            destination: `/topic/room/${key}`,
            subsMap: roomSubs,
            handlersMap: roomHandlers,
            label: "room(deferred)",
        });
    }

    // 3) room read 구독
    for (const roomId of roomReadSubscribeRequested) {
        const key = String(roomId);
        if (!hasHandlers(roomReadHandlers, key)) continue;

        subscribeIfNeeded({
            key,
            destination: `/topic/room/${key}/read`,
            subsMap: roomReadSubs,
            handlersMap: roomReadHandlers,
            label: "roomRead(deferred)",
        });
    }

    // 4) ping(선택)
    if (!pingSubscribed) {
        pingSubscribed = true;
        client.subscribe("/user/queue/ping", (msg) => onPing?.(msg.body));
        try {
            client.publish({ destination: "/app/ping", body: "" });
        } catch {}
    }
}

// ─────────────────────────────────────────────────────────────
// connect / ensureConnected / disconnect
// ─────────────────────────────────────────────────────────────
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

    // 이미 연결되어 있으면 그대로
    if (client?.connected) return client;

    // 이미 연결 시도중이면 기존 client 반환
    if (connecting) return client;

    connecting = true;

    client = new Client({
        webSocketFactory: () =>
            new SockJS(WS_URL, null, {
                transports: ["websocket"],          // ✅ 강제
                withCredentials: false,
                xhrFields: { withCredentials: false },
            }),


        connectHeaders: { Authorization: `Bearer ${jwt}` },
        reconnectDelay: 3000,
        withCredentials: false,
        debug: (msg) => console.log("[STOMP]", msg),

        onConnect: () => {
            connecting = false;
            console.log("✅ STOMP connected");

            // 연결되면 큐/요청/핸들러 기준으로 자동 복구
            resubscribeAll(onPing);
        },

        onWebSocketError: (evt) => {
            console.error("🧨 WebSocket error", evt);
        },

        onWebSocketClose: (evt) => {
            connecting = false;
            console.warn("🔌 WebSocket closed", evt?.code, evt?.reason);

            // ✅ live sub만 정리하고, handler/요청은 유지 → 자동복구
            cleanupLiveSubsOnly();
        },

        onStompError: (frame) => {
            console.error("❌ STOMP error", frame.headers?.["message"], frame.body);
        },
    });

    client.activate();
    return client;
}

/**
 * ✅ subscribe 전에 이걸 쓰면 "not connected yet" 경고 없이 안전해짐
 * - connectChatSocket을 1번만 실행하도록 Promise로 묶음
 */
export async function ensureConnected(jwt, onPing) {
    if (!jwt) throw new Error("JWT is required for STOMP connect");

    if (client?.connected) return client;

    // 이미 connectPromise가 있으면 그걸 기다림
    if (connectPromise) return connectPromise;

    connectChatSocket(jwt, onPing);

    connectPromise = new Promise((resolve, reject) => {
        const start = Date.now();
        const timeoutMs = 8000;

        const timer = setInterval(() => {
            if (client?.connected) {
                clearInterval(timer);
                connectPromise = null;
                return resolve(client);
            }
            if (Date.now() - start > timeoutMs) {
                clearInterval(timer);
                connectPromise = null;
                return reject(new Error("STOMP connect timeout"));
            }
        }, 50);
    });

    return connectPromise;
}

export function disconnectChatSocket() {
    // ✅ 완전 초기화(핸들러/요청까지 다 지움)
    cleanupLiveSubsOnly();

    roomsSubscribeRequested = false;
    roomsHandlers = [];

    roomSubscribeRequested.clear();
    roomHandlers.clear();

    roomReadSubscribeRequested.clear();
    roomReadHandlers.clear();

    pingSubscribed = false;
    connecting = false;
    connectPromise = null;

    if (client) {
        try {
            client.deactivate();
        } catch {}
        client = null;
    }
}

// ─────────────────────────────────────────────────────────────
// rooms(내 방 리스트 이벤트) 구독
// ─────────────────────────────────────────────────────────────
export function subscribeRooms(onEvent) {
    if (typeof onEvent === "function") roomsHandlers.push(onEvent);
    roomsSubscribeRequested = true;

    // 연결돼 있으면 즉시 구독
    if (client?.connected && !roomsSub) {
        console.log("✅ subscribing /user/queue/rooms (immediate) ...");
        roomsSub = client.subscribe("/user/queue/rooms", (msg) => {
            const body = safeJson(msg.body);
            for (const h of roomsHandlers) {
                try {
                    h?.(body);
                } catch (e) {
                    console.error("rooms handler error", e);
                }
            }
        });
    }

    return roomsSub;
}

export function unsubscribeRooms() {
    try {
        roomsSub?.unsubscribe?.();
    } catch {}
    roomsSub = null;

    roomsSubscribeRequested = false;
    roomsHandlers = [];
}

// ─────────────────────────────────────────────────────────────
// room 메시지(/topic/room/{roomId}) 구독
// ─────────────────────────────────────────────────────────────
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

    // 연결 전이면 큐잉 (onConnect에서 자동 subscribe)
    if (!client?.connected) {
        console.warn("⛔ subscribeRoom queued: not connected yet");
        return null;
    }

    // 이미 구독 중이면 반환
    if (roomSubs.has(key)) return roomSubs.get(key);

    // 즉시 구독
    subscribeIfNeeded({
        key,
        destination: `/topic/room/${key}`,
        subsMap: roomSubs,
        handlersMap: roomHandlers,
        label: "room(immediate)",
    });

    return roomSubs.get(key) ?? null;
}

export function unsubscribeRoom(roomId, onMsg) {
    const key = String(roomId);

    // 특정 핸들러만 제거
    if (typeof onMsg === "function") {
        const set = roomHandlers.get(key);
        if (set) {
            set.delete(onMsg);
            if (set.size === 0) {
                roomHandlers.delete(key);
                roomSubscribeRequested.delete(key);

                const sub = roomSubs.get(key);
                if (sub) {
                    try {
                        sub.unsubscribe();
                    } catch {}
                    roomSubs.delete(key);
                }
            }
        }
        return;
    }

    // 핸들러 전체 제거
    roomHandlers.delete(key);
    roomSubscribeRequested.delete(key);

    const sub = roomSubs.get(key);
    if (sub) {
        try {
            sub.unsubscribe();
        } catch {}
        roomSubs.delete(key);
    }
}

// ─────────────────────────────────────────────────────────────
// room read(/topic/room/{roomId}/read) 구독
// ─────────────────────────────────────────────────────────────
export function subscribeRoomRead(roomId, onRead) {
    const key = String(roomId);

    if (typeof onRead === "function") {
        let set = roomReadHandlers.get(key);
        if (!set) {
            set = new Set();
            roomReadHandlers.set(key, set);
        }
        set.add(onRead);
    }

    roomReadSubscribeRequested.add(key);

    if (!client?.connected) {
        console.warn("⛔ subscribeRoomRead queued: not connected yet");
        return null;
    }

    if (roomReadSubs.has(key)) return roomReadSubs.get(key);

    subscribeIfNeeded({
        key,
        destination: `/topic/room/${key}/read`,
        subsMap: roomReadSubs,
        handlersMap: roomReadHandlers,
        label: "roomRead(immediate)",
    });

    return roomReadSubs.get(key) ?? null;
}

export function unsubscribeRoomRead(roomId, onRead) {
    const key = String(roomId);

    if (typeof onRead === "function") {
        const set = roomReadHandlers.get(key);
        if (set) {
            set.delete(onRead);
            if (set.size === 0) {
                roomReadHandlers.delete(key);
                roomReadSubscribeRequested.delete(key);

                const sub = roomReadSubs.get(key);
                if (sub) {
                    try {
                        sub.unsubscribe();
                    } catch {}
                    roomReadSubs.delete(key);
                }
            }
        }
        return;
    }

    roomReadHandlers.delete(key);
    roomReadSubscribeRequested.delete(key);

    const sub = roomReadSubs.get(key);
    if (sub) {
        try {
            sub.unsubscribe();
        } catch {}
        roomReadSubs.delete(key);
    }
}

// ─────────────────────────────────────────────────────────────
// send
// ─────────────────────────────────────────────────────────────
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
