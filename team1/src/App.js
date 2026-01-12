import React from "react";
import store from "./store";
import {BrowserRouter, Routes, Route, useLocation} from "react-router-dom";
import {Provider, useSelector} from "react-redux";

import DashboardPage from "./pages/dashboard/DashboardPage";
import ReadyPage from "./pages/ReadyPage";
import ShopPage from "./pages/shop/ShopPage";
import LoginPage from "./pages/login/LoginPage";
import FinanceListPage from "./pages/finance/FinanceListPage";
import ExpenseDetailPage from "./pages/finance/ExpenseDetailPage";
import ExpenseAddPage from "./pages/finance/ExpenseAddPage";
import ReceiptDetailPage from "./pages/finance/ReceiptDetailPage";

// 리포트 페이지
import ReportAnalyticsPage1 from "./pages/report/ReportAnalyticsPage";

// 관리자
import AdminExpenseApprovalPage from "./pages/admin/approval/AdminExpenseApprovalPage";
import AdminExpenseApprovalDetailPage from "./pages/admin/approval/AdminExpenseApprovalDetailPage";
import AdminAccountingPage from "./pages/admin/accounting/AdminAccountingPage";

// 내 결재함
import RequestHistoryPage from "./pages/history/RequestHistoryPage";

// 장바구니 Provider
import {CartProvider} from "./context/CartContext";

// HR
import UserListPage from "./pages/admin/hr/UserListPage";
import UserDetailPage from "./pages/admin/hr/UserDetailPage";
import UserCreatePage from "./pages/admin/hr/UserCreatePage";
import UserEditPage from "./pages/admin/hr/UserEditPage";

// ✅ 날아다니는 AI 버튼 (FloatingAI 컴포넌트)
import FloatingAI from "./pages/FloatingAI";
import { FloatingAIProvider } from "./context/FloatingAIContext";
import MypagePage from "./pages/mypage/MypagePage";
import AdminShopPage from "./pages/admin/shop/AdminShopPage";
import AdminApprovalPage from "./pages/admin/AdminApprovalPage";
import ForbiddenPage from "./pages/ForbiddenPage";
import AdminLayout from "./components/layout/AdminLayout";
import AttendanceManagePage from "./pages/admin/hr/AttendanceManagePage";

// (기존 FloatingUI는 지금 안 쓰면 import 제거해도 됨)
// import FloatingUI from "./components/common/FloatingUI";

function AppInner() {
    const location = useLocation();

    // ✅ 로그인 상태: loginSlice.employeeNo 있으면 로그인
    const loginState = useSelector((state) => state.loginSlice);
    const isLogin = Boolean(loginState?.employeeNo);

    // ✅ 로그인 페이지에서는 숨기고, 로그인 상태일 때만 노출
    const showFloatingAI = isLogin && location.pathname !== "/";

    return (
        <FloatingAIProvider>
            {/* 페이지 라우팅 */}
            <Routes>

                {/* 진입 시 */}
                <Route path="/" element={<LoginPage/>}/>

                {/* 권한이 없고 임의 접속 시 */}
                <Route path="/forbidden" element={<ForbiddenPage/>}/>

                {/* 일반 사용자용 */}
                <Route path="/dashboard" element={<DashboardPage/>}/>

                {/* 내 결재함 */}
                <Route path="/approval" element={<ReadyPage title="내 결재함/작성"/>}/>

                {/* 쇼핑몰 */}
                <Route path="/shop" element={<ShopPage/>}/>

                {/* 장바구니 (페이지 버전 유지) */}
                <Route path="/cart" element={<ReadyPage title="내 지출 내역"/>}/>

                {/* 마이페이지 */}
                <Route path="/mypage" element={<MypagePage title="마이페이지"/>}/>

                {/* 내 지출 내역 */}
                <Route path="/expenses" element={<FinanceListPage/>}/>
                <Route path="/expenses/new" element={<ExpenseAddPage/>}/>
                <Route path="/expenses/:id" element={<ExpenseDetailPage/>}/>
                <Route path="/receipt/receipts/:id" element={<ReceiptDetailPage/>}/>

                {/* 내 업무 */}
                <Route path="/tasks" element={<ReportAnalyticsPage1/>}/>

                {/* ---------------------------------------- */}
                {/* 관리자 전용 (URL: /admin/...) */}

                <Route path="/admin" element={<AdminLayout/>}>
                    {/* 사원 관리 */}
                    <Route path="hr" element={<UserListPage/>}/>
                    <Route path="hr/users/:id" element={<UserDetailPage/>}/>
                    <Route path="hr/users/create" element={<UserCreatePage/>}/>
                    <Route path="hr/users/:id/edit" element={<UserEditPage/>}/>

                    {/* 통합 결재 관리 */}
                    <Route path="approval" element={<AdminExpenseApprovalPage/>}/>
                    <Route path="approval/:id" element={<AdminExpenseApprovalDetailPage/>}/>

                    <Route path="attendance" element={<AttendanceManagePage />} />

                    {/* 비품 재고 / 상품 관리 */}
                    <Route path="shop" element={<AdminShopPage title="[관리자] 상품/재고 관리"/>}/>

                    {/* 관리자용: 상품 승인 관리 */}
                    <Route
                        path="product-approval" element={<AdminApprovalPage/>}/>

                    {/* 회계 통계 */}
                    <Route path="accounting" element={<AdminAccountingPage/>}/>

                    {/* 전체 업무 모니터링 */}
                    <Route path="tasks" element={<ReadyPage title="[관리자] 팀 업무 현황"/>}/>

                    {/* 기존 경로 호환성 유지 */}
                    <Route path="approvals/expense" element={<AdminExpenseApprovalPage/>}/>
                    <Route path="approvals/expense/:id" element={<AdminExpenseApprovalDetailPage/>}/>
                </Route>







                {/* 내 결재함(주문 내역 확인) */}
                <Route path="/history" element={<RequestHistoryPage/>}/>

                {/* 기존 경로 호환성 유지 */}
                <Route path="/receipt/expenses" element={<FinanceListPage/>}/>
                <Route path="/receipt/expenses/new" element={<ExpenseAddPage/>}/>
                <Route path="/receipt/expenses/:id/edit" element={<ExpenseAddPage/>}/>
                <Route path="/receipt/expenses/:id" element={<ExpenseDetailPage/>}/>

            </Routes>

            {/* ✅ 로그인 후에만, 전체 페이지에서 항상 떠있게 */}
            {showFloatingAI && <FloatingAI />}
        </FloatingAIProvider>
    );
}

function App() {
    return (
        <Provider store={store}>
            <CartProvider>
                <BrowserRouter>
                    <AppInner/>
                </BrowserRouter>
            </CartProvider>
        </Provider>
    );
}

export default App;
