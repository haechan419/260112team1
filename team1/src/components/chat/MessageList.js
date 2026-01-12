import { useEffect, useMemo, useRef } from "react";
import { getCookie } from "../../util/cookieUtil";
import { decodeJwtPayload } from "../../util/jwtDecode";
import "../../styles/kakaoChat.css";
import { downloadChatAttachment } from "../../api/chatApi"; // ✅ 1순위: API 레이어

function toKoreanDate(d) {
    const y = d.getFullYear();
    const m = d.getMonth() + 1;
    const day = d.getDate();
    const week = ["일", "월", "화", "수", "목", "금", "토"][d.getDay()];
    return `${y}년 ${m}월 ${day}일 (${week})`;
}

function dateKey(iso) {
    const d = new Date(iso);
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, "0");
    const day = String(d.getDate()).padStart(2, "0");
    return `${y}-${m}-${day}`;
}

function formatTime(iso) {
    const d = new Date(iso);
    const h = d.getHours();
    const m = String(d.getMinutes()).padStart(2, "0");
    const ampm = h < 12 ? "오전" : "오후";
    const hh = h % 12 === 0 ? 12 : h % 12;
    return `${ampm} ${hh}:${m}`;
}

// ✅ 파일명만 깔끔하게
function shortName(name = "") {
    if (!name) return "file";
    if (name.length <= 28) return name;
    const dot = name.lastIndexOf(".");
    if (dot > 0 && dot < name.length - 1) {
        const ext = name.slice(dot);
        return name.slice(0, 22) + "…" + ext;
    }
    return name.slice(0, 26) + "…";
}

function formatBytes(bytes) {
    const n = Number(bytes);
    if (!Number.isFinite(n) || n <= 0) return "";
    const units = ["B", "KB", "MB", "GB"];
    let v = n;
    let i = 0;
    while (v >= 1024 && i < units.length - 1) {
        v /= 1024;
        i++;
    }
    const fixed = i === 0 ? String(Math.round(v)) : v.toFixed(1);
    return `${fixed} ${units[i]}`;
}

function isImageMime(mime = "") {
    return mime.startsWith("image/");
}

export default function MessageList({ messages, otherLastReadMessageId }) {
    const bottomRef = useRef(null);

    const token = useMemo(() => {
        const member = getCookie("member");
        return member?.accessToken ?? null;
    }, []);

    const meId = useMemo(() => {
        const member = getCookie("member");
        const token = member?.accessToken;
        const payload = token ? decodeJwtPayload(token) : null;
        return payload?.id ?? null;
    }, []);

    // 1) createdAt 기준 오래된 → 최신 정렬
    const sorted = useMemo(() => {
        const arr = Array.isArray(messages) ? [...messages] : [];
        arr.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
        return arr;
    }, [messages]);

    // 2) 렌더용 “날짜칩 + 메시지” 합성
    const rows = useMemo(() => {
        const out = [];
        let prevKey = null;

        for (const m of sorted) {
            const key = dateKey(m.createdAt);
            if (key !== prevKey) {
                out.push({ type: "date", key, label: toKoreanDate(new Date(m.createdAt)) });
                prevKey = key;
            }
            out.push({ type: "msg", msg: m });
        }
        return out;
    }, [sorted]);

    useEffect(() => {
        bottomRef.current?.scrollIntoView({ behavior: "smooth" });
    }, [rows.length]);

    if (!sorted || sorted.length === 0) {
        return <div className="kcEmpty">메시지가 없습니다. 첫 메시지를 보내보세요.</div>;
    }

    return (
        <div className="kcMsgArea">
            {rows.map((r) => {
                if (r.type === "date") {
                    return (
                        <div key={`date-${r.key}`} className="kcDateChip">
                            {r.label}
                        </div>
                    );
                }

                const m = r.msg;
                const id = m.messageId ?? m.id;
                const mine = meId != null && Number(m.senderId) === Number(meId);

                const attachments = Array.isArray(m.attachments) ? m.attachments : [];
                const hasAtt = attachments.length > 0;
                const text = (m.content ?? "").trim();
                const hasText = text.length > 0;

                const isReadByOther =
                    mine &&
                    otherLastReadMessageId != null &&
                    Number(otherLastReadMessageId) >= Number(id);

                return (
                    <div key={`msg-${id}`} className={`kcRow ${mine ? "me" : "other"}`}>
                        {!mine && <div className="kcAvatar">{String(m.senderId).slice(-2)}</div>}

                        <div className="kcBubbleWrap">
                            {/* ✅ 1) 텍스트 버블: content 있을 때만 표시 */}
                            {hasText && <div className={`kcBubble ${mine ? "me" : "other"}`}>{text}</div>}

                            {/* ✅ 2) 첨부파일 카드들 */}
                            {hasAtt && (
                                <div className={`kcAttList ${mine ? "me" : "other"}`}>
                                    {attachments.map((a) => {
                                        const attId = a.attachmentId ?? a.id;
                                        const name = a.originalName ?? "file";
                                        const mime = a.mimeType ?? "";
                                        const size = a.size ?? a.fileSize ?? null;

                                        const img = isImageMime(mime);

                                        return (
                                            // ✅ a태그(새탭/href) 제거: Authorization 없는 요청이 나가서 SPA index.html/401 HTML 등으로 꼬일 수 있음
                                            <div
                                                key={`att-${id}-${attId}`}
                                                className={`kcAttCard ${mine ? "me" : "other"}`}
                                                title={name}
                                                role="group"
                                            >
                                                {img ? (
                                                    // ✅ 보호 리소스면 img src로 미리보기는 안됨(Authorization 못 붙임) → 일단 아이콘 처리
                                                    <div className="kcAttIcon">🖼️</div>
                                                ) : (
                                                    <div className="kcAttIcon">📎</div>
                                                )}

                                                <div className="kcAttMeta">
                                                    <div className="kcAttName">{shortName(name)}</div>
                                                    <div className="kcAttSub">
                                                        {mime ? mime : "file"}
                                                        {size ? ` · ${formatBytes(size)}` : ""}
                                                    </div>
                                                </div>

                                                <button
                                                    type="button"
                                                    className="kcAttAction"
                                                    onClick={async (e) => {
                                                        e.preventDefault();
                                                        e.stopPropagation();
                                                        try {
                                                            await downloadChatAttachment(attId, name, token);
                                                        } catch (err) {
                                                            console.error(err);
                                                            alert("다운로드 실패");
                                                        }
                                                    }}
                                                >
                                                    다운로드
                                                </button>
                                            </div>
                                        );
                                    })}
                                </div>
                            )}

                            {/* ✅ 3) 시간/읽음 */}
                            <div className={`kcMeta ${mine ? "me" : "other"}`}>
                                {mine && (
                                    <span className={`kcRead ${isReadByOther ? "read" : "unread"}`}>
                    {isReadByOther ? "읽음" : "1"}
                  </span>
                                )}
                                <span className="kcTime">{formatTime(m.createdAt)}</span>
                            </div>
                        </div>
                    </div>
                );
            })}

            <div ref={bottomRef} />
        </div>
    );
}
