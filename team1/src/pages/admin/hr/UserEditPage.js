import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  getUser,
  updateUser,
  unlockUser,
  resignUser,
} from "../../../api/adminUserApi";
import AppLayout from "../../../components/layout/AppLayout";
import ProfileImageUpload from "../../../components/admin/hr/ProfileImageUpload";
import "./UserEditPage.css";

const UserEditPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  // 폼 상태
  const [formData, setFormData] = useState({
    employeeNo: "",
    name: "",
    birthDate: "",
    phone: "",
    email: "",
    address: "",
    addressDetail: "",
    departmentName: "",
    position: "",
    role: "USER",
    newPassword: "",
  });

  // 상태값 (읽기 전용)
  const [isLocked, setIsLocked] = useState(false);
  const [isActive, setIsActive] = useState(true);
  const [thumbnailUrl, setThumbnailUrl] = useState(null);

  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  // 부서 목록
  const departments = [
    "개발1팀",
    "개발2팀",
    "인사팀",
    "재무팀",
    "영업팀",
    "마케팅팀",
    "기획팀",
    "디자인팀",
  ];

  // 직급 목록
  const positions = ["사원", "주임", "대리", "과장", "차장", "부장"];

  useEffect(() => {
    if (id) {
      loadUser();
    }
  }, [id]);

  const loadUser = async () => {
    setLoading(true);
    try {
      const response = await getUser(id);
      console.log("사원 정보 응답:", response);
      console.log("role 값:", response.role);

      // ✅ role이 없으면 기본값 "USER" 설정 (하지만 백엔드에서 반환해야 함)
      const userRole = response.role || "USER";
      console.log("설정할 role:", userRole);

      setFormData({
        employeeNo: response.employeeNo || "",
        name: response.name || "",
        birthDate: response.birthDate || "",
        phone: response.phone || "",
        email: response.email || "",
        address: response.address || "",
        addressDetail: response.addressDetail || "",
        departmentName: response.departmentName || "",
        position: response.positionName || "",
        role: userRole,
        newPassword: "",
      });
      setIsLocked(response.locked || false);
      setIsActive(response.active !== false);
      setThumbnailUrl(response.thumbnailUrl || null);
    } catch (error) {
      console.error("사원 정보 조회 실패:", error);
      alert("사원 정보를 불러올 수 없습니다.");
      navigate("/admin/hr/users");
    } finally {
      setLoading(false);
    }
  };

  // 입력값 변경
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  // 저장
  const handleSubmit = async () => {
    if (!formData.name.trim()) {
      alert("이름을 입력해주세요.");
      return;
    }

    setSaving(true);
    try {
      await updateUser(id, formData);
      alert("사원 정보가 수정되었습니다.");
      navigate(`/admin/hr/users/${id}`);
    } catch (error) {
      console.error("사원 수정 실패:", error);
      if (error.response?.data?.message) {
        alert(error.response.data.message);
      } else {
        alert("사원 수정에 실패했습니다.");
      }
    } finally {
      setSaving(false);
    }
  };

  // 잠금 해제
  const handleUnlock = async () => {
    if (!window.confirm("계정 잠금을 해제하시겠습니까?")) return;

    try {
      await unlockUser(id);
      alert("계정 잠금이 해제되었습니다.");
      setIsLocked(false);
    } catch (error) {
      console.error("잠금 해제 실패:", error);
      alert("잠금 해제에 실패했습니다.");
    }
  };

  // 퇴사 처리
  const handleResign = async () => {
    if (
      !window.confirm(
        "정말 퇴사 처리하시겠습니까?\n이 작업은 되돌릴 수 없습니다."
      )
    )
      return;

    try {
      await resignUser(id);
      alert("퇴사 처리되었습니다.");
      setIsActive(false);
      setIsLocked(true);
    } catch (error) {
      console.error("퇴사 처리 실패:", error);
      alert("퇴사 처리에 실패했습니다.");
    }
  };

  // 취소
  const handleCancel = () => {
    if (window.confirm("수정 중인 내용이 사라집니다. 취소하시겠습니까?")) {
      navigate(`/admin/hr/users/${id}`);
    }
  };

  // 이미지 변경 핸들러
  const handleImageChange = (result) => {
    if (result && result.thumbnailUrl) {
      setThumbnailUrl(result.thumbnailUrl);
    } else {
      setThumbnailUrl(null);
    }
  };

  if (loading) {
    return (
      <AppLayout>
        <div className="user-edit-page">
          <div className="loading">로딩 중...</div>
        </div>
      </AppLayout>
    );
  }

  return (
    <AppLayout>
      <div className="user-edit-page">
        {/* 상단 영역: 사진 + 직원 정보 */}
        <div className="top-section">
          {/* 왼쪽: 사진 업로드 영역 */}
          <div className="photo-section">
            <ProfileImageUpload 
              userId={parseInt(id)}
              thumbnailUrl={thumbnailUrl}
              onImageChange={handleImageChange}
            />
          </div>

          {/* 오른쪽: 직원 정보 */}
          <div className="info-section">
            <h3 className="section-title">직원 정보</h3>
            <div className="info-grid">
              <div className="info-row">
                <label>
                  이름 <span className="required">*</span>
                </label>
                <input
                  type="text"
                  name="name"
                  className="form-input"
                  value={formData.name}
                  onChange={handleChange}
                />
              </div>
              <div className="info-row">
                <label>생년월일</label>
                <input
                  type="date"
                  name="birthDate"
                  className="form-input"
                  value={formData.birthDate}
                  onChange={handleChange}
                />
              </div>
              <div className="info-row">
                <label>사번</label>
                <div className="info-value readonly">{formData.employeeNo}</div>
              </div>
              <div className="info-row">
                <label>새 비밀번호</label>
                <input
                  type="password"
                  name="newPassword"
                  className="form-input"
                  placeholder="변경 시에만 입력"
                  value={formData.newPassword}
                  onChange={handleChange}
                />
              </div>
              <div className="info-row">
                <label>연락처</label>
                <input
                  type="text"
                  name="phone"
                  className="form-input"
                  placeholder="010-0000-0000"
                  value={formData.phone}
                  onChange={handleChange}
                />
              </div>
              <div className="info-row">
                <label>이메일</label>
                <input
                  type="email"
                  name="email"
                  className="form-input"
                  value={formData.email}
                  onChange={handleChange}
                />
              </div>
              <div className="info-row">
                <label>주소</label>
                <input
                  type="text"
                  name="address"
                  className="form-input"
                  value={formData.address}
                  onChange={handleChange}
                />
              </div>
              <div className="info-row">
                <label>상세주소</label>
                <input
                  type="text"
                  name="addressDetail"
                  className="form-input"
                  value={formData.addressDetail}
                  onChange={handleChange}
                />
              </div>
              <div className="info-row checkbox-row">
                <label></label>
                <div className="checkbox-group">
                  <label className="checkbox-label">
                    <input type="checkbox" checked={isLocked} disabled />
                    <span>계정 잠금 여부</span>
                    {isLocked && (
                      <button
                        type="button"
                        className="btn btn-sm btn-warning"
                        onClick={handleUnlock}
                      >
                        잠금 해제
                      </button>
                    )}
                  </label>
                  <label className="checkbox-label">
                    <input type="checkbox" checked={!isActive} disabled />
                    <span>퇴사 처리 여부</span>
                    {isActive && (
                      <button
                        type="button"
                        className="btn btn-sm btn-danger"
                        onClick={handleResign}
                      >
                        퇴사 처리
                      </button>
                    )}
                  </label>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* 하단 영역: 회사 정보 */}
        <div className="bottom-section">
          <div className="company-section">
            <h3 className="section-title">회사 정보</h3>
            <div className="company-grid">
              <div className="company-row">
                <label>부서</label>
                <select
                  name="departmentName"
                  className="form-select"
                  value={formData.departmentName}
                  onChange={handleChange}
                >
                  <option value="">부서 선택</option>
                  {departments.map((dept) => (
                    <option key={dept} value={dept}>
                      {dept}
                    </option>
                  ))}
                </select>
              </div>
              <div className="company-row">
                <label>직원 직급</label>
                <select
                  name="position"
                  className="form-select"
                  value={formData.position}
                  onChange={handleChange}
                >
                  <option value="">직급 선택</option>
                  {positions.map((pos) => (
                    <option key={pos} value={pos}>
                      {pos}
                    </option>
                  ))}
                </select>
              </div>
              <div className="company-row">
                <label>권한</label>
                <select
                  name="role"
                  className="form-select"
                  value={formData.role}
                  onChange={handleChange}
                >
                  <option value="USER">일반 사용자</option>
                  <option value="ADMIN">관리자</option>
                </select>
              </div>
            </div>
          </div>

          {/* 버튼 영역 */}
          <div className="button-section">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={handleCancel}
            >
              취소
            </button>
            <button
              type="button"
              className="btn btn-primary"
              onClick={handleSubmit}
              disabled={saving}
            >
              {saving ? "저장 중..." : "정보 수정"}
            </button>
          </div>
        </div>
      </div>
    </AppLayout>
  );
};

export default UserEditPage;
