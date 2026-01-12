import React, { useState, useRef } from "react";
import { useDispatch } from "react-redux";
import { uploadMeetingNote, analyzeMeetingNote } from "../../api/meetingNoteApi";
import { fetchTodos } from "../../slices/todoSlice";
import "./MeetingNoteUpload.css";

export default function MeetingNoteUpload() {
    const dispatch = useDispatch();
    const [file, setFile] = useState(null);
    const [uploading, setUploading] = useState(false);
    const [analyzing, setAnalyzing] = useState(false);
    const [uploadedNoteId, setUploadedNoteId] = useState(null);
    const [message, setMessage] = useState("");
    const fileInputRef = useRef(null);

    // 파일 선택 핸들러
    const handleFileSelect = (e) => {
        const selectedFile = e.target.files[0];
        if (selectedFile) {
            // 파일 타입 검증 (TXT만 허용)
            const allowedTypes = ["text/plain"];
            const allowedExtensions = [".txt"];
            const fileName = selectedFile.name.toLowerCase();
            const isValidType = allowedTypes.includes(selectedFile.type) ||
                allowedExtensions.some(ext => fileName.endsWith(ext));

            if (!isValidType) {
                setMessage("지원하지 않는 파일 형식입니다. (TXT 파일만 가능)");
                return;
            }
            setFile(selectedFile);
            setMessage("");
            setUploadedNoteId(null);
        }
    };

    // 드래그 앤 드롭 핸들러
    const handleDragOver = (e) => {
        e.preventDefault();
        e.stopPropagation();
    };

    const handleDragLeave = (e) => {
        e.preventDefault();
        e.stopPropagation();
    };

    const handleDrop = (e) => {
        e.preventDefault();
        e.stopPropagation();

        const droppedFile = e.dataTransfer.files[0];
        if (droppedFile) {
            // 파일 타입 검증 (TXT만 허용)
            const allowedTypes = ["text/plain"];
            const allowedExtensions = [".txt"];
            const fileName = droppedFile.name.toLowerCase();
            const isValidType = allowedTypes.includes(droppedFile.type) ||
                allowedExtensions.some(ext => fileName.endsWith(ext));

            if (!isValidType) {
                setMessage("지원하지 않는 파일 형식입니다. (TXT 파일만 가능)");
                return;
            }
            setFile(droppedFile);
            setMessage("");
            setUploadedNoteId(null);
        }
    };

    // 파일 업로드 핸들러
    const handleUpload = async () => {
        if (!file) {
            setMessage("파일을 선택해주세요.");
            return;
        }

        setUploading(true);
        setMessage("");

        try {
            const response = await uploadMeetingNote(file);
            setUploadedNoteId(response.result);
            setMessage("업로드가 완료되었습니다!");
            setFile(null);
            if (fileInputRef.current) {
                fileInputRef.current.value = "";
            }
        } catch (error) {
            setMessage(
                error.response?.data?.message || "업로드에 실패했습니다. 다시 시도해주세요."
            );
        } finally {
            setUploading(false);
        }
    };

    // 분석 및 Todo 생성 핸들러
    const handleAnalyze = async () => {
        if (!uploadedNoteId) {
            setMessage("먼저 회의록을 업로드해주세요.");
            return;
        }

        setAnalyzing(true);
        setMessage("");

        try {
            const response = await analyzeMeetingNote(uploadedNoteId);
            setMessage(
                `분석이 완료되었습니다! ${response.todoCount}개의 Todo가 생성되었습니다.`
            );
            // Todo 목록 새로고침
            dispatch(fetchTodos());
            setUploadedNoteId(null);
        } catch (error) {
            setMessage(
                error.response?.data?.message || "분석에 실패했습니다. 다시 시도해주세요."
            );
        } finally {
            setAnalyzing(false);
        }
    };

    // 파일 제거 핸들러
    const handleRemoveFile = () => {
        setFile(null);
        setMessage("");
        setUploadedNoteId(null);
        if (fileInputRef.current) {
            fileInputRef.current.value = "";
        }
    };

    return (
        <div className="meeting-note-upload-container">
            <h3>회의록 업로드</h3>

            <div
                className="upload-area"
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
            >
                {file ? (
                    <div className="file-selected">
                        <div className="file-info">
                            <span className="file-icon">📄</span>
                            <span className="file-name">{file.name}</span>
                            <span className="file-size">
                                ({(file.size / 1024).toFixed(2)} KB)
                            </span>
                        </div>
                        <button className="btn-remove" onClick={handleRemoveFile}>
                            <span>×</span>
                            <span>제거</span>
                        </button>
                    </div>
                ) : (
                    <div className="upload-placeholder">
                        <div className="upload-icon">📎</div>
                        <p>파일을 드래그하거나 클릭하여 선택하세요</p>
                        <p className="upload-hint">TXT 파일만 지원됩니다</p>
                    </div>
                )}
            </div>

            <input
                ref={fileInputRef}
                type="file"
                accept=".txt"
                onChange={handleFileSelect}
                style={{ display: "none" }}
            />

            <div className="upload-actions">
                <button
                    className="btn-select"
                    onClick={() => fileInputRef.current?.click()}
                    disabled={uploading || analyzing}
                >
                    파일 선택
                </button>
                <button
                    className="btn-upload"
                    onClick={handleUpload}
                    disabled={!file || uploading || analyzing}
                >
                    {uploading ? "업로드 중..." : "업로드"}
                </button>
                <button
                    className="btn-analyze"
                    onClick={handleAnalyze}
                    disabled={!uploadedNoteId || analyzing}
                >
                    {analyzing ? "분석 중..." : "분석 및 Todo 생성"}
                </button>
            </div>

            {message && (
                <div
                    className={`message ${message.includes("실패") || message.includes("지원하지")
                        ? "error"
                        : "success"
                        }`}
                >
                    {message}
                </div>
            )}
        </div>
    );
}

