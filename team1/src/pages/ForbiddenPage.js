import React from "react";
import { useNavigate } from "react-router-dom";

const ForbiddenPage = () => {
  const navigate = useNavigate();

  return (
    <div
      style={{
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        height: "100vh",
        backgroundColor: "#f8f9fa",
      }}
    >
      <div
        style={{
          textAlign: "center",
          padding: "60px",
          backgroundColor: "white",
          borderRadius: "16px",
          boxShadow: "0 4px 20px rgba(0,0,0,0.1)",
        }}
      >
        <div style={{ fontSize: "80px", marginBottom: "20px" }}>🚫</div>
        <h1
          style={{ fontSize: "48px", color: "#e74c3c", marginBottom: "10px" }}
        >
          403
        </h1>
        <h2 style={{ fontSize: "24px", color: "#333", marginBottom: "20px" }}>
          접근 권한이 없습니다
        </h2>
        <p style={{ color: "#666", marginBottom: "30px" }}>
          이 페이지는 관리자만 접근할 수 있습니다.
        </p>
        <button
          onClick={() => navigate("/dashboard")}
          style={{
            padding: "12px 32px",
            fontSize: "16px",
            backgroundColor: "#3498db",
            color: "white",
            border: "none",
            borderRadius: "8px",
            cursor: "pointer",
          }}
        >
          홈으로 돌아가기
        </button>
      </div>
    </div>
  );
};

export default ForbiddenPage;
