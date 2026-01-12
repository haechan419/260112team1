import { useEffect, useMemo, useRef, useState, useCallback } from "react";
import { chatApi } from "../../api/chatApi";
import "../../styles/newChatModal.css";

export default function NewChatModal({ open, onClose, onCreated }) {
    const [q, setQ] = useState("");
    const [results, setResults] = useState([]);
    const [picked, setPicked] = useState(() => new Set());
    const [loading, setLoading] = useState(false);
    const [err, setErr] = useState("");

    // ✅ debounce / stale response 방지용
    const reqSeqRef = useRef(0);

    // ✅ pickedList를 최신 상태로 들고있게 (handleStart에서 stale 방지)
    const pickedList = useMemo(() => Array.from(picked), [picked]);

    // ✅ 서버 응답이 userId든 id든 다 커버
    const getUserId = useCallback((u) => u?.userId ?? u?.id ?? null, []);

    // ✅ 모달 열릴 때 초기화
    useEffect(() => {
        if (!open) return;
        setQ("");
        setResults([]);
        setPicked(new Set());
        setErr("");
        setLoading(false);
        reqSeqRef.current += 1; // 기존 검색 응답 무효화
    }, [open]);

    // ✅ ESC 닫기 / Ctrl+Enter 시작
    useEffect(() => {
        if (!open) return;

        const onKeyDown = (e) => {
            if (e.key === "Escape") onClose?.();
            if ((e.ctrlKey || e.metaKey) && e.key === "Enter") {
                if (pickedList.length > 0 && !loading) handleStart();
            }
        };

        window.addEventListener("keydown", onKeyDown);
        return () => window.removeEventListener("keydown", onKeyDown);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [open, onClose, pickedList.length, loading]);

    // ✅ 유저 선택 토글
    const togglePick = useCallback((userId) => {
        if (userId == null) return;
        setPicked((prev) => {
            const next = new Set(prev);
            if (next.has(userId)) next.delete(userId);
            else next.add(userId);
            return next;
        });
    }, []);

    // ✅ 검색 (debounce + 최신 응답만 반영)
    useEffect(() => {
        if (!open) return;

        const keyword = q.trim();
        if (!keyword) {
            setResults([]);
            setErr("");
            return;
        }

        const mySeq = ++reqSeqRef.current;

        const t = setTimeout(async () => {
            try {
                setErr("");
                const data = await chatApi.searchUsers(keyword, 20);

                // ✅ 모달 닫혔거나, 더 최신 요청이 있으면 무시
                if (!open || reqSeqRef.current !== mySeq) return;

                const arr = Array.isArray(data) ? data : [];

                // ✅ id(userId/id) 없는 항목은 제거 + 중복 id 제거(안전)
                const seen = new Set();
                const normalized = [];
                for (const u of arr) {
                    const uid = getUserId(u);
                    if (uid == null) continue;
                    if (seen.has(uid)) continue;
                    seen.add(uid);
                    normalized.push(u);
                }

                console.log("[SEARCH] keyword=", keyword, "count=", normalized.length, "sample=", normalized[0]);

                setResults(normalized);
            } catch (e) {
                if (!open || reqSeqRef.current !== mySeq) return;
                console.error("[SEARCH] error", e?.response?.status, e?.response?.data, e);
                setErr(e?.response?.data?.message || e.message || "검색 실패");
                setResults([]);
            }
        }, 250);

        return () => clearTimeout(t);
    }, [q, open, getUserId]);

    // ✅ DM/그룹 생성 + 부모에 roomId 넘기기
    const handleStart = useCallback(async () => {
        // 🔥 디버그: 상태 확인
        console.log("[START] open=", open, "loading=", loading);
        console.log("[START] picked(raw) =", Array.from(picked));
        console.log("[START] pickedList(memo) =", pickedList);

        // ✅ 최신 picked 보장 + 값 정제(undefined/null 제거)
        // ⚠️ 지금은 원인확정이 목적이라 Number 강제변환을 잠깐 빼둠 (UUID/문자열이면 NaN으로 비게 됨)
        const list = Array.from(picked).filter((v) => v !== undefined && v !== null);

        console.log("[START] list(filtered) =", list, "types=", list.map((v) => typeof v));

        if (list.length === 0 || loading) {
            setErr("선택된 유저가 없거나 로딩 중입니다. (picked/list 확인 필요)");
            return;
        }

        setLoading(true);
        setErr("");

        try {
            console.log("[START] calling API... mode=", list.length === 1 ? "DM" : "GROUP");

            let res;
            if (list.length === 1) {
                res = await chatApi.createDm(list[0]);
            } else {
                res = await chatApi.createGroup(list);
            }

            console.log("[START] API res =", res);

            // ✅ 서버 응답 형태 다양성 대응
            const roomId = res?.roomId ?? res?.id ?? res?.data?.roomId ?? res?.data?.id;

            console.log("[START] parsed roomId =", roomId);

            if (!roomId) throw new Error("roomId missing (응답 키 확인 필요)");

            onCreated?.(roomId); // ✅ 부모가 setActiveRoomId(roomId) 해야 열림
            onClose?.(); // 모달 닫기
        } catch (e) {
            // 🔥 여기서 status / response 바디를 반드시 본다
            console.error("[START] API error", e?.response?.status, e?.response?.data, e);

            const status = e?.response?.status;
            const msg =
                e?.response?.data?.message ||
                (typeof e?.response?.data === "string" ? e.response.data : null) ||
                e?.message ||
                "채팅 시작 실패";

            // 상태코드까지 같이 보여주면 네가 바로 판단 가능
            setErr(status ? `(${status}) ${msg}` : msg);
        } finally {
            setLoading(false);
        }
    }, [open, picked, pickedList, loading, onCreated, onClose]);

    if (!open) return null;

    const isPicked = (userId) => picked.has(userId);

    return (
        <div className="ncmOverlay" onMouseDown={onClose}>
            <div className="ncmModal" onMouseDown={(e) => e.stopPropagation()}>
                <div className="ncmHeader">
                    <div className="ncmTitle">새 채팅 시작</div>
                    <button className="ncmClose" onClick={onClose} aria-label="close" type="button">
                        ✕
                    </button>
                </div>

                <div className="ncmBody">
                    <input
                        className="ncmSearch"
                        placeholder="이름/사번 검색"
                        value={q}
                        onChange={(e) => setQ(e.target.value)}
                        autoFocus
                    />

                    {err && <div className="ncmErr">{err}</div>}

                    <div className="ncmList">
                        {results.map((u) => {
                            const uid = getUserId(u);
                            if (uid == null) return null;

                            return (
                                <button
                                    key={uid} // ✅ unique key
                                    className={`ncmItem ${isPicked(uid) ? "picked" : ""}`}
                                    onClick={() => togglePick(uid)}
                                    type="button"
                                >
                                    <div className="ncmName">{u.name}</div>
                                    <div className="ncmMeta">
                                        <span>#{uid}</span>
                                        {u.employeeNo ? <span> · {u.employeeNo}</span> : null}
                                        {u.departmentName ? <span> · {u.departmentName}</span> : null}
                                    </div>
                                </button>
                            );
                        })}

                        {q.trim() && results.length === 0 && !err && (
                            <div className="ncmEmpty">검색 결과가 없습니다.</div>
                        )}
                    </div>
                </div>

                <div className="ncmFooter">
                    <div className="ncmPicked">선택: {pickedList.length}명</div>
                    <button
                        className="ncmStart"
                        onClick={handleStart}
                        disabled={loading || pickedList.length === 0}
                        type="button"
                        title="Ctrl+Enter"
                    >
                        {loading ? "생성 중..." : pickedList.length === 1 ? "DM 시작" : "그룹 시작"}
                    </button>
                </div>
            </div>
        </div>
    );
}
