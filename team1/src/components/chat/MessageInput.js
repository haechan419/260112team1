import { useRef, useState } from "react";
import "../../styles/kakaoChat.css";
import { chatApi } from "../../api/chatApi";

export default function MessageInput({ disabled, roomId, onSend }) {
    const [text, setText] = useState("");
    const [files, setFiles] = useState([]); // File[]
    const [uploading, setUploading] = useState(false);
    const fileRef = useRef(null);

    const pickFiles = () => {
        if (disabled || uploading) return;
        fileRef.current?.click();
    };

    const onPick = (e) => {
        const picked = Array.from(e.target.files || []);
        if (picked.length === 0) return;

        // ✅ 같은 파일 다시 선택 가능하게 input value 초기화
        e.target.value = "";

        // ✅ 누적 선택 (원하면 여기서 덮어쓰기(setFiles(picked))로 바꿔도 됨)
        setFiles((prev) => {
            const next = [...prev, ...picked];
            // 너무 많이 붙지 않게 (옵션)
            return next.slice(0, 10);
        });
    };

    const removeFile = (idx) => {
        setFiles((prev) => prev.filter((_, i) => i !== idx));
    };

    const sendTextOnly = async () => {
        const v = text.trim();
        if (!v || disabled || uploading) return;
        await onSend(v);
        setText("");
    };

    const sendWithFiles = async () => {
        if (disabled || uploading) return;
        if (!roomId) return;

        // 파일만 보내도 되게 content는 "" 허용
        const content = (text ?? "").trim(); // "" 가능
        if (!files.length && !content) return;

        setUploading(true);
        try {
            // ✅ 1) 파일 업로드 + message 생성
            const res = await chatApi.uploadAttachments(roomId, content, files);

            // res: { ok:true, messageId, attachments:[{attachmentId, originalName, mimeType, size, url}] }
            // ✅ 2) 입력 상태 초기화
            setText("");
            setFiles([]);

            // ✅ 3) (선택) 서버 WS 브로드캐스트가 아직 확실치 않으면
            // 여기서 onSend를 "즉시 append용"으로 확장할 수도 있음.
            // 지금은 너희가 WS 브로드캐스트 하기로 했으니 아무것도 안 해도 됨.
            // 만약 “업로드 메시지가 바로 화면에 안 뜬다”면 말해. 그때 fallback 넣어줄게.

        } catch (e) {
            console.error(e);
            alert(e?.response?.data?.message || e.message || "파일 전송 실패");
        } finally {
            setUploading(false);
        }
    };

    // ✅ 엔터는 텍스트 전송만, 첨부가 있으면 Enter 대신 버튼으로 보내게 (실수 방지)
    const onKeyDown = (e) => {
        if (e.key !== "Enter") return;
        if (files.length > 0) return;
        sendTextOnly();
    };

    const canSendText = !disabled && !uploading && text.trim().length > 0 && files.length === 0;
    const canSendFiles = !disabled && !uploading && (files.length > 0 || text.trim().length > 0);

    return (
        <div className="kcInputBar">
            {/* 숨김 파일 인풋 */}
            <input
                ref={fileRef}
                type="file"
                multiple
                style={{ display: "none" }}
                onChange={onPick}
            />

            {/* 📎 버튼 */}
            <button
                type="button"
                className="kcAttachBtn"
                disabled={disabled || uploading}
                onClick={pickFiles}
                title="파일 첨부"
            >
                📎
            </button>

            <div className="kcInputStack">
                {/* 첨부 미리보기(파일명 칩) */}
                {files.length > 0 && (
                    <div className="kcFileChips">
                        {files.map((f, idx) => (
                            <div key={`${f.name}-${idx}`} className="kcFileChip">
                                <span className="kcFileName">{f.name}</span>
                                <button
                                    type="button"
                                    className="kcFileRemove"
                                    onClick={() => removeFile(idx)}
                                    disabled={uploading}
                                    title="삭제"
                                >
                                    ✕
                                </button>
                            </div>
                        ))}
                    </div>
                )}

                <input
                    className="kcInput"
                    value={text}
                    disabled={disabled || uploading}
                    placeholder={
                        disabled
                            ? "방을 선택하세요"
                            : uploading
                                ? "업로드 중..."
                                : files.length
                                    ? "파일 설명(선택)"
                                    : "메시지를 입력하세요"
                    }
                    onChange={(e) => setText(e.target.value)}
                    onKeyDown={onKeyDown}
                />
            </div>

            {/* 전송 버튼: 파일 있으면 업로드 전송, 없으면 텍스트 전송 */}
            <button
                className="kcSend"
                disabled={files.length ? !canSendFiles : !canSendText}
                onClick={files.length ? sendWithFiles : sendTextOnly}
            >
                {uploading ? "..." : "전송"}
            </button>
        </div>
    );
}
