import React, { useState } from "react";
import { receiptApi } from "../../api/receiptApi";
import "./ReceiptUpload.css";

const ReceiptUpload = ({ expenseId, onUploadSuccess, onUploadError }) => {
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [preview, setPreview] = useState(null);

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      const selectedFile = e.target.files[0];
      setFile(selectedFile);

      // 미리보기 생성
      const reader = new FileReader();
      reader.onloadend = () => {
        setPreview(reader.result);
      };
      reader.readAsDataURL(selectedFile);
    }
  };

  const handleUpload = async () => {
    if (!file) {
      onUploadError("파일을 선택해주세요.");
      return;
    }

    setUploading(true);
    try {
      await receiptApi.uploadReceipt(expenseId, file);
      onUploadSuccess();
      setFile(null);
      setPreview(null);
      // 파일 input 초기화
      const fileInput = document.querySelector('input[type="file"]');
      if (fileInput) {
        fileInput.value = "";
      }
    } catch (error) {
      onUploadError(error instanceof Error ? error.message : "업로드에 실패했습니다.");
    } finally {
      setUploading(false);
    }
  };

  const handleRemove = () => {
    setFile(null);
    setPreview(null);
    const fileInput = document.querySelector('input[type="file"]');
    if (fileInput) {
      fileInput.value = "";
    }
  };

  return (
    <div className="receipt-upload">
      <div className="upload-section">
        <label className="file-input-label">
          <input
            type="file"
            accept="image/*"
            onChange={handleFileChange}
            disabled={uploading}
            className="file-input"
          />
          <span className="file-input-button">
            {uploading ? "업로드 중..." : "파일 선택"}
          </span>
        </label>
        {file && (
          <div className="file-info">
            <span className="file-name">{file.name}</span>
            <span className="file-size">
              ({(file.size / 1024).toFixed(2)} KB)
            </span>
            <button className="btn-remove" onClick={handleRemove} disabled={uploading}>
              ×
            </button>
          </div>
        )}
      </div>

      {preview && (
        <div className="preview-section">
          <div className="preview-label">미리보기</div>
          <div className="preview-image-container">
            <img src={preview} alt="영수증 미리보기" className="preview-image" />
          </div>
        </div>
      )}

      {file && (
        <div className="upload-actions">
          <button
            className="btn btn-primary"
            onClick={handleUpload}
            disabled={uploading}
          >
            {uploading ? "업로드 중..." : "업로드"}
          </button>
          <button
            className="btn btn-secondary"
            onClick={handleRemove}
            disabled={uploading}
          >
            취소
          </button>
        </div>
      )}
    </div>
  );
};

export default ReceiptUpload;
