import React, { useState } from "react";
import "./LoginPage.css";
import useCustomLogin from "../../hooks/useCustomLogin";
import FaceLogin from "../../components/face/FaceLogin";

const LoginPage = () => {
  const [formData, setFormData] = useState({
    employeeNo: "",
    password: "",
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  // 👇 [추가] Face ID 화면을 보여줄지 말지 결정하는 스위치
  const [showFaceLogin, setShowFaceLogin] = useState(false);

  const { doLogin, moveToPath, doFaceLogin } = useCustomLogin();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (error) setError("");
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const data = await doLogin(formData);
      if (data.error) {
        setError(data.message || "로그인 실패");
      } else {
        moveToPath("/dashboard");
      }
    } catch (errorResponse) {
      setError("로그인 실패");
    } finally {
      setLoading(false);
    }
  };

  // Face ID 성공 시
  const handleFaceLoginSuccess = async (faceData) => {
    console.log("📸 Face ID 인식된 초기 데이터:", faceData);

    try {
      // 1. 로그인 처리 (쿠키 저장 + 리덕스 갱신)
      // (await를 썼으니 데이터가 다 저장될 때까지 기다립니다)
      const savedMember = await doFaceLogin(faceData);

      console.log("🎉 로그인 완료 데이터:", savedMember);

      if (savedMember) {
        alert(`${savedMember.name}님 환영합니다!`);

        // 🚨 [수정] moveToPath 대신 '강제 새로고침 이동' 사용
        // 이렇게 해야 대시보드 컴포넌트가 다시 로딩되면서
        // 쿠키에 있는 'ADMIN' 권한을 읽고 관리자 메뉴를 그려줍니다.
        window.location.href = "/dashboard";
      }
    } catch (err) {
      console.error("로그인 후처리 중 에러:", err);
    }
  };

  // 👇 [추가] Face ID 취소 시 (카메라 끄기)
  const handleFaceCancel = () => {
    setShowFaceLogin(false); // 스위치 끄기
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <h1 className="login-title">로그인</h1>

        {/* 기존 폼 유지 */}
        <form className="login-form" onSubmit={handleSubmit}>
          {/* ... (input들 기존과 동일) ... */}
          <div className="input-group">
            <label htmlFor="employeeNo">아이디</label>
            <input
              type="text"
              id="employeeNo"
              name="employeeNo"
              value={formData.employeeNo}
              onChange={handleChange}
              required
            />
          </div>
          <div className="input-group">
            <label htmlFor="password">비밀번호</label>
            <input
              type="password"
              id="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              required
            />
          </div>

          {error && (
            <div className="error-message" style={{ color: "red" }}>
              {error}
            </div>
          )}

          <button type="submit" className="signup-button" disabled={loading}>
            {loading ? "로그인 중..." : "로그인"}
          </button>
        </form>

        {/* 👇 [수정] Face ID 영역 */}
        <div
          style={{
            marginTop: "30px",
            borderTop: "1px solid #e0e0e0",
            paddingTop: "20px",
            textAlign: "center",
          }}
        >
          {/* 조건부 렌더링: showFaceLogin이 참일 때만 카메라를 켭니다 */}
          {showFaceLogin ? (
            <FaceLogin
              onLoginSuccess={handleFaceLoginSuccess}
              onCancel={handleFaceCancel}
            />
          ) : (
            // 평소에는 이 버튼만 보입니다.
            <div>
              <p
                style={{
                  fontSize: "13px",
                  color: "#888",
                  marginBottom: "15px",
                }}
              >
                생체 인증으로 빠르고 간편하게 로그인하세요.
              </p>
              <button
                onClick={() => setShowFaceLogin(true)} // 버튼 누르면 켜짐!
                style={{
                  padding: "10px 20px",
                  backgroundColor: "#4A90E2",
                  color: "white",
                  border: "none",
                  borderRadius: "5px",
                  cursor: "pointer",
                  fontWeight: "bold",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  margin: "0 auto",
                  gap: "8px",
                }}
              >
                <span>📷</span> Face ID로 로그인
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
