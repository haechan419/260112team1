import "../../styles/layout.css";
import React, {useCallback, useEffect, useState} from "react";
import {useNavigate, useLocation} from "react-router-dom";
import {useSelector} from "react-redux";

// 화살표 아이콘
const ChevronIcon = ({isOpen}) => (
    <svg
        viewBox="0 0 24 24"
        width="16"
        height="16"
        stroke="currentColor"
        strokeWidth="2"
        fill="none"
        strokeLinecap="round"
        strokeLinejoin="round"
        style={{
            transform: isOpen ? "rotate(180deg)" : "rotate(0deg)",
            transition: "transform 0.3s ease",
            color: "#888",
        }}
    >
        <polyline points="6 9 12 15 18 9"></polyline>
    </svg>
);

const subItemStyle = {
    paddingLeft: "45px",
    fontSize: "0.95em",
    cursor: "pointer",
    paddingTop: "10px",
    paddingBottom: "10px",
    color: "#555",
    display: "flex",
    alignItems: "center",
};

export default function Sidebar() {
    const navigate = useNavigate();
    const location = useLocation();

    // // [핵심] 개발용 권한 토글 (기본값: ADMIN)
    const [userRole, setUserRole] = useState("ADMIN");

    // const toggleRole = () => {
    //     setUserRole((prev) => (prev === "ADMIN" ? "USER" : "ADMIN"));
    // };

    // ✅ Redux에서 로그인 정보 가져오기
    const loginState = useSelector((state) => state.loginSlice);

    // ✅ ADMIN 권한 체크
    const isAdmin = loginState?.roleNames?.includes("ADMIN") || false;

    // ✨ URL을 보고 열어야 할 메뉴 판단
    const checkOpenStatus = useCallback((path) => {
        return {
            purchase: path.startsWith("/shop") || path.startsWith("/history"),
            // 👇 [변경] 승인 관리 페이지도 재고 그룹에 포함
            inventory:
                path.startsWith("/admin/shop") ||
                path.startsWith("/admin/product-approval"),
        };
    }, []);

    const [openMenu, setOpenMenu] = useState(() =>
        checkOpenStatus(location.pathname)
    );

    useEffect(() => {
        const nextStatus = checkOpenStatus(location.pathname);
        setOpenMenu((prev) => {
            if (
                prev.purchase === nextStatus.purchase &&
                prev.inventory === nextStatus.inventory
            ) {
                return prev;
            }
            return nextStatus;
        });
    }, [location.pathname, checkOpenStatus]);

    const toggleRole = () =>
        setUserRole((prev) => (prev === "ADMIN" ? "USER" : "ADMIN"));

    const toggleMenu = (key) => {
        setOpenMenu((prev) => ({
            purchase: key === "purchase" ? !prev.purchase : false,
            inventory: key === "inventory" ? !prev.inventory : false,
        }));
    };

    const handleStandaloneClick = (path) => {
        setOpenMenu({purchase: false, inventory: false});
        navigate(path);
    };

    const getNavItemClass = (path) => {
        return `nav-item ${location.pathname === path ? "active" : ""}`;
    };

    return (
        <aside className="sidebar">
            <div
                className="sidebar-logo"
                onClick={() => navigate("/dashboard")}
                style={{ cursor: "pointer" }} // 마우스 오버 시 손가락 모양 표시
            >
                SmartSpend
            </div>

            {/*/!* [개발 편의용] 모드 전환 버튼 *!/*/}
            {/*<div style={{padding: "10px 20px"}}>*/}
            {/*    <button*/}
            {/*        onClick={toggleRole}*/}
            {/*        style={{*/}
            {/*            width: "100%",*/}
            {/*            padding: "8px",*/}
            {/*            background: userRole === "ADMIN" ? "#e74c3c" : "#2ecc71",*/}
            {/*            border: "none",*/}
            {/*            borderRadius: "6px",*/}
            {/*            color: "white",*/}
            {/*            fontWeight: "bold",*/}
            {/*            cursor: "pointer",*/}
            {/*            fontSize: "12px",*/}
            {/*        }}*/}
            {/*    >*/}
            {/*        현재: {userRole === "ADMIN" ? "관리자 모드" : "사원 모드"} 🔄*/}
            {/*    </button>*/}
            {/*</div>*/}

            <nav className="sidebar-nav">
                {/* 1. [공통] 메인 메뉴 */}
                <div
                    className={`nav-item ${location.pathname === "/dashboard" ? "active" : ""}`}
                    onClick={() => navigate("/dashboard")}
                >
                    <span style={{marginRight: "10px"}}>🏠</span> Home
                </div>

                {/*<div*/}
                {/*    className={`nav-item ${*/}
                {/*        location.pathname === "/approval" ? "active" : ""*/}
                {/*    }`}*/}
                {/*    onClick={() => navigate("/approval")}*/}
                {/*>*/}
                {/*    <span style={{marginRight: "10px"}}>✅</span> 전자결재*/}
                {/*</div>*/}

                {/* [그룹 1] 비품 구매 */}
                <div className="nav-group">
                    <div
                        className="nav-item group-header"
                        onClick={() => toggleMenu("purchase")}
                        style={{justifyContent: "space-between", cursor: "pointer"}}
                    >
                        <div style={{display: "flex", alignItems: "center"}}>
                            <span style={{marginRight: "10px"}}>🛒</span> 비품 구매
                        </div>
                        <ChevronIcon isOpen={openMenu.purchase}/>
                    </div>

                    <div
                        className="submenu-container"
                        style={{
                            maxHeight: openMenu.purchase ? "200px" : "0",
                            opacity: openMenu.purchase ? 1 : 0,
                            overflow: "hidden",
                            transition: "all 0.4s ease-in-out",
                            backgroundColor: "rgba(0,0,0,0.03)",
                        }}
                    >
                        <div
                            className={getNavItemClass("/shop")}
                            onClick={() => navigate("/shop")}
                            style={{
                                ...subItemStyle,
                                // 현재 경로가 /shop이면 흰색(#ffffff), 아니면 기존 색상(#555)
                                color: location.pathname === "/shop" ? "#ffffff" : "#555"
                            }}
                        >
                            <span style={{fontSize: "12px", marginRight: "8px"}}>●</span>{" "}
                            비품 쇼핑하기
                        </div>
                        <div
                            className={getNavItemClass("/history")}
                            onClick={() => navigate("/history")}
                            style={{
                                ...subItemStyle,
                                // 현재 경로가 /shop이면 흰색(#ffffff), 아니면 기존 색상(#555)
                                color: location.pathname === "/history" ? "#ffffff" : "#555"
                            }}
                        >
                            <span style={{fontSize: "12px", marginRight: "8px"}}>●</span>{" "}
                            구매 신청 내역
                        </div>
                    </div>
                </div>

                <div
                    className={`nav-item ${
                        location.pathname === "/tasks" ? "active" : ""
                    }`}
                    onClick={() => navigate("/tasks")}
                >
                    <span style={{marginRight: "10px"}}>📁</span> 업무보드
                </div>

                <div
                    className={`nav-item ${
                        location.pathname === "/mypage" ? "active" : ""
                    }`}
                    onClick={() => navigate("/mypage")}
                >
                    <span style={{marginRight: "10px"}}>👤</span> 마이페이지
                </div>

                <div
                    className={`nav-item ${
                        location.pathname === "/expenses" ? "active" : ""
                    }`}
                    onClick={() => navigate("/expenses")}
                >
                    <span style={{marginRight: "10px"}}>💰</span> 내 지출 내역
                </div>

                {/*/!* 2. [관리자 전용] 섹션 *!/*/}
                {/*{userRole === "ADMIN" && (*/}
                {/* 2. [관리자 전용] 섹션 */}
                {isAdmin && (
                    <>
                        <div className="nav-label">Admin Settings</div>

                        <div
                            className={`nav-item ${
                                location.pathname === "/admin/hr" ? "active" : ""
                            }`}
                            onClick={() => navigate("/admin/hr")}
                        >
                            <span style={{ marginRight: "10px" }}>👥</span> 사원관리
                        </div>
                        <div
                            className={`nav-item ${
                                location.pathname === "/admin/attendance" ? "active" : ""
                            }`}
                            onClick={() => navigate("/admin/attendance")}
                        >
                            <span style={{ marginRight: "10px" }}>📅</span> 출결관리
                        </div>

                        <div
                            className={`nav-item ${
                                location.pathname === "/admin/approval" ? "active" : ""
                            }`}
                            onClick={() => navigate("/admin/approval")}
                        >
                            <span style={{marginRight: "10px"}}>📑</span> 결재관리
                        </div>

                        {/* 👇 [그룹 2] 비품 재고 */}
                        <div className="nav-group">
                            <div
                                className="nav-item group-header"
                                onClick={() => toggleMenu("inventory")}
                                style={{ justifyContent: "space-between", cursor: "pointer" }}
                            >
                                <div style={{ display: "flex", alignItems: "center" }}>
                                    <span style={{ marginRight: "10px" }}>📦</span> 비품 재고/관리
                                </div>
                                <ChevronIcon isOpen={openMenu.inventory} />
                            </div>

                            <div
                                className="submenu-container"
                                style={{
                                    maxHeight: openMenu.inventory ? "200px" : "0",
                                    opacity: openMenu.inventory ? 1 : 0,
                                    overflow: "hidden",
                                    transition: "all 0.4s ease-in-out",
                                    backgroundColor: "rgba(0,0,0,0.03)",
                                }}
                            >
                                <div
                                    className={getNavItemClass("/admin/shop")}
                                    onClick={() => navigate("/admin/shop")}
                                    style={{
                                        ...subItemStyle,
                                        // 현재 경로가 /shop이면 흰색(#ffffff), 아니면 기존 색상(#555)
                                        color: location.pathname === "/admin/shop" ? "#ffffff" : "#555"
                                    }}
                                >
                  <span style={{ fontSize: "12px", marginRight: "8px" }}>
                    ●
                  </span>{" "}
                                    상품 등록/관리
                                </div>
                                {/* 👇 [추가됨] 상품 승인 관리 메뉴 */}
                                <div
                                    className={getNavItemClass("/admin/product-approval")}
                                    onClick={() => navigate("/admin/product-approval")}
                                    style={{
                                        ...subItemStyle,
                                        // 현재 경로가 /shop이면 흰색(#ffffff), 아니면 기존 색상(#555)
                                        color: location.pathname === "/admin/product-approval" ? "#ffffff" : "#555"
                                    }}
                                >
                  <span style={{ fontSize: "12px", marginRight: "8px" }}>
                    ●
                  </span>{" "}
                                    상품 승인 관리
                                </div>
                            </div>
                        </div>

                        <div
                            className={`nav-item ${
                                location.pathname === "/admin/accounting" ? "active" : ""
                            }`}
                            onClick={() => navigate("/admin/accounting")}
                        >
                            <span style={{marginRight: "10px"}}>📊</span> 회계통계
                        </div>
                    </>
                )}
            </nav>

            {/*/!* 하단 고정 *!/*/}
            {/*<div style={{marginTop: "auto", paddingBottom: "20px"}}>*/}
            {/*    <div className="nav-item">*/}
            {/*        <span style={{marginRight: "10px"}}>⚙️</span> 설정*/}
            {/*    </div>*/}
            {/*</div>*/}
        </aside>
    );
}
