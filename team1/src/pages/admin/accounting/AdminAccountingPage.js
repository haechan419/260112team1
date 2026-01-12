import React from "react";
import StatisticsDashboard from "./StatisticsDashboard";
import "./AdminAccountingPage.css";
import AppLayout from "../../../components/layout/AppLayout";

const AdminAccountingPage = () => {
    return (
        <AppLayout>
            <div className="admin-accounting-page">
                <div className="page-header">
                    <h1 className="page-title">회계 통계</h1>
                    <p className="page-description">
                        예산 집행 현황을 확인할 수 있습니다.
                    </p>
                </div>

                {/* 탭 컨텐츠 */}
                <div className="tab-content">
                    <StatisticsDashboard/>
                </div>
            </div>
        </AppLayout>
    );
};

export default AdminAccountingPage;

