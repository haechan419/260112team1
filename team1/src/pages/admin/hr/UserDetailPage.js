import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { getUser } from "../../../api/adminUserApi";
import AppLayout from "../../../components/layout/AppLayout";
import ProfileImageUpload from "../../../components/admin/hr/ProfileImageUpload";
import "./UserDetailPage.css";

const UserDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (id) {
      loadUser();
    }
  }, [id]);

  const loadUser = async () => {
    setLoading(true);
    try {
      const response = await getUser(id);
      setUser(response);
    } catch (error) {
      console.error("사원 상세 조회 실패:", error);
      alert("사원 정보를 불러올 수 없습니다.");
      navigate("/admin/hr/users");
    } finally {
      setLoading(false);
    }
  };

  // 수정 페이지로 이동
  const handleEdit = () => {
    navigate(`/admin/hr/users/${id}/edit`);
  };

  // 목록으로 돌아가기
  const handleBack = () => {
    navigate("/admin/hr");
  };

  // 날짜 포맷
  const formatDate = (dateString) => {
    if (!dateString) return "-";
    return dateString.split("T")[0];
  };

  if (loading) {
    return (
      <AppLayout>
        <div className="user-detail-page">
          <div className="loading">로딩 중...</div>
        </div>
      </AppLayout>
    );
  }

  if (!user) {
    return (
      <AppLayout>
        <div className="user-detail-page">
          <div className="empty-state">사원 정보를 찾을 수 없습니다.</div>
        </div>
      </AppLayout>
    );
  }

  return (
    <AppLayout>
      <div className="user-detail-page">
        {/* 상단 영역: 사진 + 직원 정보 */}
        <div className="top-section">
          {/* 왼쪽: 사진 업로드 영역 */}
          <div className="photo-section">
            <ProfileImageUpload 
              userId={parseInt(id)}
              thumbnailUrl={user.thumbnailUrl}
              readOnly={true}
            />
          </div>

          {/* 오른쪽: 직원 정보 */}
          <div className="info-section">
            <h3 className="section-title">직원 정보</h3>
            <div className="info-grid">
              <div className="info-row">
                <label>이름</label>
                <div className="info-value">{user.name}</div>
              </div>
              <div className="info-row">
                <label>생년월일</label>
                <div className="info-value">{user.birthDate || "-"}</div>
              </div>
              <div className="info-row">
                <label>사번</label>
                <div className="info-value">{user.employeeNo}</div>
              </div>
              <div className="info-row">
                <label>연락처</label>
                <div className="info-value">{user.phone || "-"}</div>
              </div>
              <div className="info-row">
                <label>이메일</label>
                <div className="info-value">{user.email || "-"}</div>
              </div>
              <div className="info-row">
                <label>주소</label>
                <div className="info-value">{user.address || "-"}</div>
              </div>
              <div className="info-row">
                <label>상세주소</label>
                <div className="info-value">{user.addressDetail || "-"}</div>
              </div>
              <div className="info-row checkbox-row">
                <label></label>
                <div className="checkbox-group">
                  <label className="checkbox-label">
                    <input
                      type="checkbox"
                      checked={user.locked || false}
                      disabled
                    />
                    <span>계정 잠금 여부</span>
                  </label>
                  <label className="checkbox-label">
                    <input type="checkbox" checked={!user.active} disabled />
                    <span>퇴사 처리 여부</span>
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
                <div className="info-value">{user.departmentName || "-"}</div>
              </div>
              <div className="company-row">
                <label>직원 직급</label>
                <div className="info-value">{user.positionName || "-"}</div>
              </div>
              <div className="company-row">
                <label>입사 일</label>
                <div className="info-value">
                  {formatDate(user.createdUserAt)}
                </div>
              </div>
            </div>
          </div>

          {/* 버튼 영역 */}
          <div className="button-section">
            <button className="btn btn-secondary" onClick={handleBack}>
              ← 목록으로
            </button>
            <button className="btn btn-primary" onClick={handleEdit}>
              정보 수정
            </button>
          </div>
        </div>
      </div>
    </AppLayout>
  );
};

export default UserDetailPage;
