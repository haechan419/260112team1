import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { createUser, uploadProfileImage } from "../../../api/adminUserApi";
import AppLayout from "../../../components/layout/AppLayout";
import ProfileImageUpload from "../../../components/admin/hr/ProfileImageUpload";
import "./UserCreatePage.css";

const UserCreatePage = () => {
  const navigate = useNavigate();

  // 폼 상태
  const [formData, setFormData] = useState({
    employeeNo: "",
    password: "",
    name: "",
    birthDate: "",
    phone: "",
    email: "",
    address: "",
    addressDetail: "",
    departmentName: "",
    positionName: "",
    role: "USER",
  });

  const [loading, setLoading] = useState(false);
  const [profileImageFile, setProfileImageFile] = useState(null);

  // 부서 목록 (고정값 또는 API로 가져올 수 있음)
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

  // 입력값 변경
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  // 등록
  const handleSubmit = async () => {
    // 필수값 검증
    if (!formData.employeeNo.trim()) {
      alert("사번을 입력해주세요.");
      return;
    }
    if (!formData.password.trim()) {
      alert("비밀번호를 입력해주세요.");
      return;
    }
    if (!formData.name.trim()) {
      alert("이름을 입력해주세요.");
      return;
    }

    setLoading(true);
    try {
      // 1. 사원 등록
      const result = await createUser(formData);
      
      // 2. 프로필 이미지가 있으면 업로드
      if (profileImageFile) {
        try {
          await uploadProfileImage(result.id, profileImageFile);
        } catch (imgError) {
          console.error("프로필 이미지 업로드 실패:", imgError);
          // 이미지 업로드 실패해도 사원 등록은 성공
        }
      }
      
      alert("사원이 등록되었습니다.");
      navigate(`/admin/hr/users/${result.id}`);
    } catch (error) {
      console.error("사원 등록 실패:", error);
      if (error.response?.data?.message) {
        alert(error.response.data.message);
      } else {
        alert("사원 등록에 실패했습니다.");
      }
    } finally {
      setLoading(false);
    }
  };

  // 이미지 변경 핸들러
  const handleImageChange = (file) => {
    setProfileImageFile(file);
  };

  // 취소
  const handleCancel = () => {
    if (window.confirm("작성 중인 내용이 사라집니다. 취소하시겠습니까?")) {
      navigate("/admin/hr");
    }
  };

  return (
    <AppLayout>
      <div className="user-create-page">
        {/* 상단 영역: 사진 + 직원 정보 */}
        <div className="top-section">
          {/* 왼쪽: 사진 업로드 영역 */}
          <div className="photo-section">
            <ProfileImageUpload 
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
                  placeholder="이름 입력"
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
                <label>
                  사번 <span className="required">*</span>
                </label>
                <input
                  type="text"
                  name="employeeNo"
                  className="form-input"
                  placeholder="사번 입력"
                  value={formData.employeeNo}
                  onChange={handleChange}
                />
              </div>
              <div className="info-row">
                <label>
                  비밀번호 <span className="required">*</span>
                </label>
                <input
                  type="password"
                  name="password"
                  className="form-input"
                  placeholder="초기 비밀번호 입력"
                  value={formData.password}
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
                  placeholder="example@company.com"
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
                  placeholder="주소 입력"
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
                  placeholder="상세주소 입력"
                  value={formData.addressDetail}
                  onChange={handleChange}
                />
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
                  name="positionName"
                  className="form-select"
                  value={formData.positionName}
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
              disabled={loading}
            >
              {loading ? "등록 중..." : "사원 등록"}
            </button>
          </div>
        </div>
      </div>
    </AppLayout>
  );
};

export default UserCreatePage;
