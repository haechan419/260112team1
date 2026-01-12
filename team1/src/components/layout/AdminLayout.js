import React from "react";
import { Navigate, Outlet } from "react-router-dom";
import { useSelector } from "react-redux";

/**
 * 관리자 전용 레이아웃
 * - /admin/* 하위 모든 라우트에 자동 적용
 */
const AdminLayout = () => {
    const loginState = useSelector((state) => state.loginSlice);

    // 로그인 안 됨
    if (!loginState || !loginState.employeeNo) {
        return <Navigate to="/" replace />;
    }

    // ADMIN 권한 체크
    const isAdmin = loginState?.roleNames?.includes("ADMIN") || false;
    if (!isAdmin) {
        return <Navigate to="/forbidden" replace />;
    }

    // ✅ 권한 있으면 자식 라우트 렌더링
    return <Outlet />;
};

export default AdminLayout;
