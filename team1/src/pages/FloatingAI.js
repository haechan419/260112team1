import { useEffect, useRef, useState } from "react";
import "../styles/floatingai.css";
import { useFloatingAI } from "../context/FloatingAIContext";

import { getAuthTokenForRequest } from "../util/jwtUtil"; // ✅ jwtUtil 버전
import { downloadChatAttachment } from "../api/chatApi"; // ✅ jwtAxios 기반 다운로드 유틸(토큰 인자 X)

// ========================================
// 공통: JWT 포함 POST helper
// ========================================
async function postJson(url, body) {
    const token = getAuthTokenForRequest();

    const res = await fetch(url, {
        method: "POST",
        headers: {
            "Content-Type": "application/json; charset=utf-8",
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify(body),
    });

    if (!res.ok) {
        const text = await res.text().catch(() => "");
        throw new Error(`AI API failed: ${res.status} ${text}`);
    }
    return await res.json();
}

// ========================================
// Spring Boot AI (JWT 필요)
// ========================================
const SPRING_AI_BASE = "http://localhost:8080/api/ai";

async function aiGenerate(prompt) {
    const data = await postJson(`${SPRING_AI_BASE}/generate`, { prompt });
    if (data && data.ok === false) throw new Error(data.message || "AI API ok=false");
    return data;
}

async function aiFindContext(roomId, query) {
    return postJson(`${SPRING_AI_BASE}/find-context`, { roomId, query });
}

async function aiFindChatFilesGlobal(query) {
    return postJson(`${SPRING_AI_BASE}/find-chat-files-global`, { query });
}

// ========================================
// Python FastAPI (JWT 없으면 그대로, 필요하면 나중에 추가)
// ========================================
async function attendanceAiRequest(prompt) {
    const res = await fetch("http://localhost:8000/api/ai/attendance", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ prompt }),
    });

    if (!res.ok) {
        const text = await res.text();
        throw new Error(`출결 AI API failed: ${res.status} ${text}`);
    }
    return await res.json();
}

async function performanceAiRequest(prompt) {
    const res = await fetch("http://localhost:8000/api/ai/performance", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ prompt }),
    });

    if (!res.ok) {
        const text = await res.text();
        throw new Error(`실적 AI API failed: ${res.status} ${text}`);
    }
    return await res.json();
}

// ========================================
// 키워드 분기
// ========================================
function isAttendanceQuery(prompt) {
    const keywords = ["출결", "출근", "지각", "결근", "휴가", "근태", "출석", "attendance"];
    const lower = prompt.toLowerCase();
    return keywords.some((k) => lower.includes(k));
}

function isPerformanceQuery(prompt) {
    const keywords = [
        "실적",
        "매출",
        "비교",
        "그래프",
        "차트",
        "성과",
        "목표달성",
        "계약",
        "달성률",
        "순위",
        "1위",
        "최고",
        "부서",
        "팀",
        "작년",
        "전년",
        "성장",
        "추이",
        "분석",
    ];
    const lower = prompt.toLowerCase();
    return keywords.some((k) => lower.includes(k));
}

// ========================================
// (채팅) 결과 포맷팅 - 컨텍스트
// ========================================
function formatContextResult(data) {
    const summary = (data?.summary ?? "").toString().trim();
    const msgs = Array.isArray(data?.messages) ? data.messages : [];

    const lines = [];
    lines.push(`📌 요약\n${summary || "(요약 없음)"}`);

    if (msgs.length) {
        lines.push("");
        lines.push(`🧾 근거 메시지 (${Math.min(5, msgs.length)}개)`);
        msgs.slice(0, 5).forEach((m) => {
            const roomId = m.roomId != null ? `room:${m.roomId}` : "room:?";
            const when = m.createdAt ? String(m.createdAt) : "";
            const content = (m.content ?? "").toString();
            lines.push(`- [${roomId}] ${when}  ${content}`);
        });
    } else {
        lines.push("");
        lines.push("🧾 근거 메시지: 없음");
    }

    return { text: lines.join("\n"), messages: msgs };
}

// ========================================
// (채팅) 결과 포맷팅 - 파일
// ========================================
function formatFilesResult(data) {
    const summary = (data?.summary ?? "").toString().trim();
    const files = Array.isArray(data?.files) ? data.files : [];

    const lines = [];
    lines.push(`📎 파일 찾기 결과`);
    lines.push(`📌 요약\n${summary || "(요약 없음)"}`);

    if (files.length) {
        lines.push("");
        lines.push(`🗂️ 파일 (${Math.min(5, files.length)}개)`);

        files.slice(0, 5).forEach((f) => {
            const id = f.attachmentId ?? "?";
            const room = f.roomId != null ? `room:${f.roomId}` : "room:?";
            const when = f.createdAt ? String(f.createdAt) : "";
            const name = (f.originalName ?? "").toString();
            const snip = (f.messageSnippet ?? "").toString();

            lines.push(`- [${id}] [${room}] ${when}  ${name}`);
            if (snip) lines.push(`    ↳ ${snip}`);
        });
    } else {
        lines.push("");
        lines.push("🗂️ 파일: 없음");
    }

    return { text: lines.join("\n"), files };
}

// ========================================
// 메인 컴포넌트
// ========================================
export default function FloatingAI({ roomId, onOpenRoom }) {
    const { open, setOpen } = useFloatingAI();

    const emitOpenRoom = (payload) => {
        if (typeof onOpenRoom === "function") return onOpenRoom(payload);
        window.dispatchEvent(new CustomEvent("ai-open-room", { detail: payload }));
    };

    const [prompt, setPrompt] = useState("");
    const [loading, setLoading] = useState(false);
    const [err, setErr] = useState("");
    const textareaRef = useRef(null);

    // ✅ 채팅 컨텍스트/파일찾기 결과용
    const [resultText, setResultText] = useState("");
    const [resultMessages, setResultMessages] = useState([]);
    const [resultFiles, setResultFiles] = useState([]);

    // ✅ 기존 Python 응답(실적/출결) + 이미지 모달
    const [imageModal, setImageModal] = useState(false);
    const [response, setResponse] = useState({
        message: "",
        summary: "",
        hasFile: false,
        downloadUrl: "",
        fileName: "",
        chartImage: "",
    });

    // ====== UX: 열릴 때 포커스 ======
    useEffect(() => {
        if (open) {
            const t = setTimeout(() => textareaRef.current?.focus(), 50);
            return () => clearTimeout(t);
        }
    }, [open]);

    // ====== UX: ESC ======
    useEffect(() => {
        const onKeyDown = (e) => {
            if (e.key === "Escape") {
                if (imageModal) setImageModal(false);
                else setOpen(false);
            }
        };
        window.addEventListener("keydown", onKeyDown);
        return () => window.removeEventListener("keydown", onKeyDown);
    }, [imageModal, setOpen]);

    // ====== Enter 전송 ======
    const handleKeyDown = (e) => {
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            onRun();
        }
    };

    // ====== Reset ======
    const resetAll = () => {
        setPrompt("");
        setErr("");

        setResultText("");
        setResultMessages([]);
        setResultFiles([]);

        setResponse({
            message: "",
            summary: "",
            hasFile: false,
            downloadUrl: "",
            fileName: "",
            chartImage: "",
        });
    };

    // ========================================
    // ✅ AI 실행(통합)
    // 우선순위:
    // 1) 실적 키워드 → Python
    // 2) 출결 키워드 → Python
    // 3) roomId 있으면 → Spring find-context
    // 4) roomId 없으면 → Spring global file search
    // 5) (컨텍스트 메시지 0개면) generate fallback
    // ========================================
    const onRun = async () => {
        const p = prompt.trim();
        if (!p) return;

        setErr("");
        setLoading(true);

        // 실행 전 결과 초기화
        setResultText("");
        setResultMessages([]);
        setResultFiles([]);
        setResponse({
            message: "",
            summary: "",
            hasFile: false,
            downloadUrl: "",
            fileName: "",
            chartImage: "",
        });

        try {
            // 1) Python: 실적
            if (isPerformanceQuery(p)) {
                console.log("[AI] 실적 관련 → Python");
                const data = await performanceAiRequest(p);
                if (!data.ok) throw new Error(data.message || "처리 실패");

                setResponse({
                    message: data.message || "",
                    summary: data.summary || "",
                    hasFile: false,
                    downloadUrl: "",
                    fileName: "",
                    chartImage: data.chartImage || "",
                });
                return;
            }

            // 2) Python: 출결
            if (isAttendanceQuery(p)) {
                console.log("[AI] 출결 관련 → Python");
                const data = await attendanceAiRequest(p);
                if (!data.ok) throw new Error(data.message || "처리 실패");

                setResponse({
                    message: data.message || "",
                    summary: data.summary || "",
                    hasFile: data.hasFile || false,
                    downloadUrl: data.downloadUrl || "",
                    fileName: data.fileName || "",
                    chartImage: "",
                });
                return;
            }

            // 3) Spring: room 컨텍스트
            if (roomId) {
                const ctx = await aiFindContext(Number(roomId), p);
                const formatted = formatContextResult(ctx);

                setResultText(formatted.text);
                setResultMessages(formatted.messages);
                setResultFiles([]);

                // 컨텍스트 메시지 없으면 generate fallback
                const msgs = formatted.messages || [];
                if (msgs.length === 0) {
                    const finalPrompt = `한국어로만 답변해줘.\n\n${p}`;
                    const out = await aiGenerate(finalPrompt);

                    const text =
                        typeof out === "string"
                            ? out
                            : out?.result ?? out?.message ?? JSON.stringify(out);

                    setResultText((prev) => `${prev}\n\n🤖 (채팅에서 못 찾아서 일반 답변)\n${text}`);
                }
                return;
            }

            // 4) Spring: global 파일찾기
            const filesData = await aiFindChatFilesGlobal(p);
            const formattedFiles = formatFilesResult(filesData);

            setResultText(formattedFiles.text);
            setResultMessages([]);
            setResultFiles(formattedFiles.files || []);
        } catch (e) {
            setErr(e?.message || String(e));
        } finally {
            setLoading(false);
        }
    };

    // ====== Python 출결 파일 다운로드 (기존 유지) ======
    const handlePythonDownload = async () => {
        if (!response.downloadUrl) return;

        try {
            const downloadUrl = `http://localhost:8000${response.downloadUrl}`;
            const res = await fetch(downloadUrl);
            if (!res.ok) throw new Error("다운로드 실패");

            const blob = await res.blob();
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement("a");
            link.href = url;
            link.download = response.fileName || "출결데이터.xlsx";
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);
        } catch (e) {
            setErr("파일 다운로드 실패: " + (e?.message || String(e)));
        }
    };

    return (
        <>
            {/* ✅ 항상 떠있는 AI 버튼(FAB) */}
            <button
                className="ai-fab"
                type="button"
                aria-label="Open AI assistant"
                title="AI"
                onClick={() => setOpen(true)}
            >
                AI
            </button>

            {/* ✅ open일 때만 패널/오버레이 렌더 */}
            {open && (
                <div className="ai-overlay" onMouseDown={() => setOpen(false)}>
                    <div
                        className="ai-panel"
                        onMouseDown={(e) => e.stopPropagation()}
                        role="dialog"
                        aria-modal="true"
                    >
                        <div className="ai-panel__header">
                            <div className="ai-panel__title">
                                🤖 AI Assistant {roomId ? `(room ${roomId})` : "(global + python)"}
                            </div>
                            <button className="ai-x" onClick={() => setOpen(false)} type="button">
                                ✕
                            </button>
                        </div>

                        <div className="ai-panel__body">
              <textarea
                  ref={textareaRef}
                  className="ai-input"
                  rows={3}
                  value={prompt}
                  onChange={(e) => setPrompt(e.target.value)}
                  onKeyDown={handleKeyDown}
                  placeholder={
                      roomId
                          ? "예) 일정 마감일 변경 얘기했었나?"
                          : "예) 승인금액 리포트 엑셀 찾아줘 / 개발1팀 개발2팀 실적 비교해줘"
                  }
              />

                            <div className="ai-actions">
                                <button
                                    className="ai-btn"
                                    onClick={onRun}
                                    disabled={loading || !prompt.trim()}
                                    type="button"
                                >
                                    {loading ? "처리 중..." : "보내기"}
                                </button>

                                <button
                                    className="ai-btn ai-btn--ghost"
                                    onClick={resetAll}
                                    disabled={loading}
                                    type="button"
                                >
                                    초기화
                                </button>
                            </div>

                            {err && <div className="ai-error">❌ {err}</div>}

                            <div className="ai-result">
                                <div className="ai-result__label">💬 Result</div>
                                <div className="ai-result__box">
                                    {/* ===========================
                      1) Python 응답 UI
                     =========================== */}
                                    {response.message && (
                                        <>
                                            <p style={{ marginBottom: "10px", fontWeight: "500" }}>
                                                {response.message}
                                            </p>

                                            {response.summary && (
                                                <pre
                                                    style={{
                                                        background: "#f5f5f5",
                                                        padding: "12px",
                                                        borderRadius: "8px",
                                                        fontSize: "13px",
                                                        whiteSpace: "pre-wrap",
                                                        marginBottom: "12px",
                                                        lineHeight: "1.5",
                                                    }}
                                                >
                          {response.summary}
                        </pre>
                                            )}

                                            {response.hasFile && (
                                                <button
                                                    onClick={handlePythonDownload}
                                                    type="button"
                                                    style={{
                                                        display: "flex",
                                                        alignItems: "center",
                                                        gap: "8px",
                                                        padding: "12px 20px",
                                                        background:
                                                            "linear-gradient(135deg, #22c55e 0%, #16a34a 100%)",
                                                        color: "white",
                                                        border: "none",
                                                        borderRadius: "10px",
                                                        cursor: "pointer",
                                                        fontSize: "14px",
                                                        fontWeight: "600",
                                                        boxShadow: "0 2px 8px rgba(34, 197, 94, 0.3)",
                                                    }}
                                                >
                                                    엑셀 다운로드
                                                    <span style={{ fontSize: "12px", opacity: 0.9 }}>
                            ({response.fileName})
                          </span>
                                                </button>
                                            )}

                                            {response.chartImage && (
                                                <div style={{ marginTop: "16px" }}>
                                                    <img
                                                        src={`data:image/png;base64,${response.chartImage}`}
                                                        alt="부서 실적 비교 그래프"
                                                        onClick={() => setImageModal(true)}
                                                        style={{
                                                            width: "100%",
                                                            maxWidth: "700px",
                                                            borderRadius: "12px",
                                                            boxShadow: "0 4px 12px rgba(0, 0, 0, 0.15)",
                                                            cursor: "pointer",
                                                        }}
                                                    />
                                                    <p
                                                        style={{
                                                            fontSize: "12px",
                                                            color: "#888",
                                                            marginTop: "6px",
                                                            textAlign: "center",
                                                        }}
                                                    >
                                                        🔍 클릭하면 크게 볼 수 있습니다
                                                    </p>
                                                </div>
                                            )}
                                        </>
                                    )}

                                    {/* ===========================
                      2) Spring 채팅 컨텍스트: room 이동 버튼
                     =========================== */}
                                    {!response.message &&
                                        Array.isArray(resultMessages) &&
                                        resultMessages.length > 0 && (
                                            <div style={{ marginBottom: 10 }}>
                                                {resultMessages.slice(0, 5).map((m) => {
                                                    const rid = m.roomId;
                                                    const mid = m.messageId ?? m.id;
                                                    return (
                                                        <button
                                                            key={mid ?? `${rid}-${Math.random()}`}
                                                            type="button"
                                                            className="ai-btn ai-btn--ghost"
                                                            style={{ marginRight: 6, marginBottom: 6 }}
                                                            onClick={() => {
                                                                if (!rid) return;
                                                                emitOpenRoom({
                                                                    roomId: String(rid),
                                                                    messageId: mid != null ? String(mid) : null,
                                                                });
                                                            }}
                                                            title={`room ${rid}로 이동`}
                                                        >
                                                            room {rid}
                                                        </button>
                                                    );
                                                })}
                                            </div>
                                        )}

                                    {/* ===========================
                      3) Spring 전역 파일찾기: 파일 리스트 + 다운로드
                     =========================== */}
                                    {!response.message &&
                                        Array.isArray(resultFiles) &&
                                        resultFiles.length > 0 && (
                                            <div className="ai-result__files" style={{ marginBottom: 10 }}>
                                                {resultFiles.slice(0, 5).map((f) => (
                                                    <div key={f.attachmentId} className="ai-file-row">
                                                        <button
                                                            type="button"
                                                            className="ai-btn ai-btn--ghost ai-room-btn"
                                                            onClick={() => {
                                                                emitOpenRoom({
                                                                    roomId: String(f.roomId),
                                                                    messageId:
                                                                        f.messageId != null ? String(f.messageId) : null,
                                                                });
                                                            }}
                                                            title={`room ${f.roomId}로 이동`}
                                                        >
                                                            room {f.roomId ?? "?"}
                                                        </button>

                                                        <button
                                                            type="button"
                                                            className="ai-btn ai-btn--ghost ai-file-name"
                                                            onClick={() => {
                                                                const rid = f.roomId;
                                                                if (!rid) return;
                                                                emitOpenRoom({ roomId: String(rid) });
                                                            }}
                                                            title="해당 방으로 이동"
                                                        >
                                                            📎 {f.originalName || `file ${f.attachmentId}`}
                                                        </button>

                                                        <button
                                                            type="button"
                                                            className="ai-btn ai-btn--ghost ai-dl-btn"
                                                            onClick={async () => {
                                                                try {
                                                                    await downloadChatAttachment(
                                                                        f.attachmentId,
                                                                        f.originalName
                                                                    );
                                                                } catch (e) {
                                                                    setErr(e?.message || String(e));
                                                                }
                                                            }}
                                                            title="다운로드"
                                                        >
                                                            다운로드
                                                        </button>
                                                    </div>
                                                ))}
                                            </div>
                                        )}

                                    {/* ===========================
                      4) Spring 텍스트 결과 출력
                     =========================== */}
                                    {!response.message && (
                                        <pre style={{ margin: 0, whiteSpace: "pre-wrap" }}>
                      {resultText || "결과가 여기에 표시됩니다."}
                    </pre>
                                    )}
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* 이미지 확대 모달 (기존 유지) */}
            {imageModal && response.chartImage && (
                <div
                    style={{
                        position: "fixed",
                        top: 0,
                        left: 0,
                        right: 0,
                        bottom: 0,
                        background: "rgba(0, 0, 0, 0.9)",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        zIndex: 10000,
                        cursor: "pointer",
                        animation: "fadeIn 0.2s ease-out",
                    }}
                    onClick={() => setImageModal(false)}
                >
                    <div
                        style={{
                            position: "relative",
                            maxWidth: "95vw",
                            maxHeight: "95vh",
                            animation: "scaleIn 0.2s ease-out",
                        }}
                        onClick={(e) => e.stopPropagation()}
                    >
                        <img
                            src={`data:image/png;base64,${response.chartImage}`}
                            alt="부서 실적 비교 그래프 (확대)"
                            style={{
                                maxWidth: "95vw",
                                maxHeight: "85vh",
                                borderRadius: "16px",
                                boxShadow: "0 12px 48px rgba(0, 0, 0, 0.5)",
                            }}
                        />
                        <button
                            onClick={() => setImageModal(false)}
                            type="button"
                            style={{
                                position: "absolute",
                                top: "-50px",
                                right: "0",
                                background: "rgba(255, 255, 255, 0.1)",
                                border: "none",
                                color: "white",
                                fontSize: "28px",
                                cursor: "pointer",
                                width: "44px",
                                height: "44px",
                                borderRadius: "50%",
                                display: "flex",
                                alignItems: "center",
                                justifyContent: "center",
                                transition: "background 0.2s",
                            }}
                        >
                            ✕
                        </button>
                        <p
                            style={{
                                textAlign: "center",
                                color: "rgba(255, 255, 255, 0.6)",
                                marginTop: "16px",
                                fontSize: "14px",
                            }}
                        >
                            ESC 또는 바깥 영역을 클릭하면 닫힙니다
                        </p>
                    </div>
                </div>
            )}

            <style>{`
        @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
        @keyframes scaleIn { from { transform: scale(0.9); opacity: 0; } to { transform: scale(1); opacity: 1; } }
      `}</style>
        </>
    );
}
