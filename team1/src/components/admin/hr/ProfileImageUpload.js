import React, { useRef, useState } from "react";
import { uploadProfileImage, deleteProfileImage } from "../../../api/adminUserApi";
import "./ProfileImageUpload.css";

// API 서버 URL
const API_SERVER_HOST = "http://localhost:8080";

/**
 * 프로필 이미지 업로드 컴포넌트
 * 
 * @param {Object} props
 * @param {number} props.userId - 사용자 ID (수정 시 필요)
 * @param {string} props.thumbnailUrl - 현재 썸네일 URL
 * @param {function} props.onImageChange - 이미지 변경 시 콜백
 * @param {boolean} props.readOnly - 읽기 전용 모드
 */
const ProfileImageUpload = ({ 
  userId, 
  thumbnailUrl, 
  onImageChange,
  readOnly = false 
}) => {
  const fileInputRef = useRef(null);
  const [previewUrl, setPreviewUrl] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState(null);

  // 허용된 파일 타입
  const ALLOWED_TYPES = ["image/jpeg", "image/jpg", "image/png"];
  const MAX_SIZE = 5 * 1024 * 1024; // 5MB

  // 현재 표시할 이미지 URL
  const displayUrl = previewUrl || (thumbnailUrl ? `${API_SERVER_HOST}${thumbnailUrl}` : null);

  // 파일 선택 버튼 클릭
  const handleButtonClick = () => {
    if (readOnly) return;
    fileInputRef.current?.click();
  };

  // 파일 선택 처리
  const handleFileChange = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setError(null);

    // 파일 타입 검증
    if (!ALLOWED_TYPES.includes(file.type)) {
      setError("jpg, jpeg, png 파일만 업로드할 수 있습니다.");
      return;
    }

    // 파일 크기 검증
    if (file.size > MAX_SIZE) {
      setError("파일 크기는 5MB를 초과할 수 없습니다.");
      return;
    }

    // 미리보기 생성
    const reader = new FileReader();
    reader.onload = (e) => {
      setPreviewUrl(e.target.result);
    };
    reader.readAsDataURL(file);

    // userId가 있으면 바로 업로드 (수정 모드)
    if (userId) {
      await uploadImage(file);
    } else {
      // userId가 없으면 (등록 모드) 부모에게 파일 전달
      onImageChange?.(file, previewUrl);
    }
  };

  // 이미지 업로드
  const uploadImage = async (file) => {
    setUploading(true);
    try {
      const result = await uploadProfileImage(userId, file);
      setPreviewUrl(null); // 업로드 성공 시 미리보기 제거
      onImageChange?.(result);
      alert("프로필 이미지가 업로드되었습니다.");
    } catch (err) {
      console.error("이미지 업로드 실패:", err);
      setError(err.response?.data?.message || "이미지 업로드에 실패했습니다.");
      setPreviewUrl(null);
    } finally {
      setUploading(false);
    }
  };

  // 이미지 삭제
  const handleDelete = async () => {
    if (!userId || !thumbnailUrl) return;
    
    if (!window.confirm("프로필 이미지를 삭제하시겠습니까?")) return;

    try {
      await deleteProfileImage(userId);
      setPreviewUrl(null);
      onImageChange?.(null);
      alert("프로필 이미지가 삭제되었습니다.");
    } catch (err) {
      console.error("이미지 삭제 실패:", err);
      setError("이미지 삭제에 실패했습니다.");
    }
  };

  return (
    <div className="profile-image-upload">
      <h3 className="section-title">사진업로드(증명사진)</h3>
      
      <div 
        className={`photo-container ${!readOnly ? 'clickable' : ''}`}
        onClick={handleButtonClick}
      >
        {displayUrl ? (
          <img 
            src={displayUrl} 
            alt="프로필 사진" 
            className="profile-image"
          />
        ) : (
          <div className="photo-placeholder">
            <span className="placeholder-icon">👤</span>
            <span className="placeholder-text">사진 없음</span>
          </div>
        )}
        
        {uploading && (
          <div className="upload-overlay">
            <span>업로드 중...</span>
          </div>
        )}
      </div>

      {error && <div className="error-message">{error}</div>}

      {!readOnly && (
        <div className="button-group">
          <button 
            type="button" 
            className="btn btn-outline"
            onClick={handleButtonClick}
            disabled={uploading}
          >
            {uploading ? "업로드 중..." : "파일 업로드"}
          </button>
          
          {displayUrl && userId && (
            <button 
              type="button" 
              className="btn btn-outline btn-danger"
              onClick={handleDelete}
              disabled={uploading}
            >
              삭제
            </button>
          )}
        </div>
      )}

      <input
        ref={fileInputRef}
        type="file"
        accept=".jpg,.jpeg,.png"
        onChange={handleFileChange}
        style={{ display: "none" }}
      />

      <p className="upload-hint">
        * jpg, jpeg, png 파일만 가능 (최대 5MB)
      </p>
    </div>
  );
};

export default ProfileImageUpload;
