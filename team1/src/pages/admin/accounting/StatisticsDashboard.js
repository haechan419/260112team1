import React, { useEffect, useState } from "react";
import "../../../styles/layout.css";
import "../../../styles/dashboard.css";
import FetchingModal from "../../../components/common/FetchingModal";
import { getAllStatistics } from "../../../api/accountingApi";
import { getCookie } from "../../../util/cookieUtil";

// Chart.js 관련 임포트
import {
  Chart as ChartJS,
  ArcElement,
  Tooltip,
  Legend,
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
} from "chart.js";
import { Doughnut, Bar } from "react-chartjs-2";

ChartJS.register(
  ArcElement,
  Tooltip,
  Legend,
  CategoryScale,
  LinearScale,
  BarElement,
  Title
);

const StatisticsDashboard = () => {
  const [loading, setLoading] = useState(false);
  const [summary, setSummary] = useState({
    totalBudgetExecutionRate: 0,
    totalPendingCount: 0,
    monthlyTotalExpense: 0,
    overBudgetCount: 0,
    // 추가된 상세 지표들
    todaySubmittedCount: 0,
    todayProcessedCount: 0,
    monthlyExpenseChangeRate: 0,
    todayApprovedCount: 0,
    todayRejectedCount: 0,
    todayRequestMoreInfoCount: 0,
  });
  const [departmentChart, setDepartmentChart] = useState([]);
  const [categoryChart, setCategoryChart] = useState([]);
  const [overBudgetList, setOverBudgetList] = useState([]);

  useEffect(() => {
    loadStatistics();
  }, []);

  const loadStatistics = async () => {
    const memberInfo = getCookie("member");
    if (!memberInfo || !memberInfo.accessToken) return;

    setLoading(true);
    try {
      const response = await getAllStatistics();
      const s = response.summary;

      // 1. 모든 요약 데이터 매핑 (전월 대비 등 포함)
      setSummary({
        totalBudgetExecutionRate: s.totalBudgetExecutionRate || 0,
        totalPendingCount: s.totalPendingCount || 0, // 미결재
        monthlyTotalExpense: s.monthlyTotalExpense || 0,
        overBudgetCount: s.overBudgetCount || 0,
        todaySubmittedCount: s.todaySubmittedCount || 0,
        todayProcessedCount: s.todayProcessedCount || 0,
        monthlyExpenseChangeRate: s.monthlyExpenseChangeRate || 0, // 👈 전월 대비 증감률
        todayApprovedCount: s.todayApprovedCount || 0,
        todayRejectedCount: s.todayRejectedCount || 0,
        todayRequestMoreInfoCount: s.todayRequestMoreInfoCount || 0,
      });

      // 2. 차트 데이터 가공
      setDepartmentChart(
        (response.department || []).map((d) => ({
          name: d.departmentName || "기타",
          amount: d.totalAmount || 0,
        }))
      );
      setCategoryChart(response.category || []);
      setOverBudgetList(response.overBudget || []);
    } catch (error) {
      console.error("통계 조회 실패:", error);
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (val) =>
    new Intl.NumberFormat("ko-KR", {
      style: "currency",
      currency: "KRW",
    }).format(val);

  // 차트 설정 데이터
  const deptChartData = {
    labels: departmentChart.map((d) => d.name),
    datasets: [
      {
        data: departmentChart.map((d) => d.amount),
        backgroundColor: [
          "#3b82f6",
          "#10b981",
          "#f59e0b",
          "#ef4444",
          "#8b5cf6",
          "#ec4899",
        ],
        borderWidth: 2,
      },
    ],
  };

  const catChartData = {
    labels: categoryChart.map((c) => c.name || "기타"),
    datasets: [
      {
        label: "지출액",
        data: categoryChart.map((c) => c.amount || 0),
        backgroundColor: "rgba(59, 130, 246, 0.8)",
      },
    ],
  };

  return (
    <div className="statistics-dashboard">
      {loading && <FetchingModal />}

      {/* 상단 통계 카드 (전월 대비 표시 추가) */}
      <div className="dashboard-grid">
        <div className="panel stat-card">
          <div className="stat-title">부서 총 예산 집행률</div>
          <div className="stat-value">{summary.totalBudgetExecutionRate}%</div>
          <div className="stat-footer">
            <span
              className={
                summary.totalBudgetExecutionRate >= 80
                  ? "trend-up"
                  : "trend-down"
              }
            >
              {summary.totalBudgetExecutionRate >= 80 ? "⚠️ 주의" : "✅ 안정적"}
            </span>
          </div>
        </div>

        <div className="panel stat-card">
          <div className="stat-title">총 미결재 건수</div>
          <div className="stat-value">{summary.totalPendingCount}건</div>
          <div className="stat-footer text-muted">처리 대기 중</div>
        </div>

        <div className="panel stat-card">
          <div className="stat-title">이번 달 총 지출액</div>
          <div className="stat-value">
            {formatCurrency(summary.monthlyTotalExpense)}
          </div>
          <div className="stat-footer">
            <span
              className={
                summary.monthlyExpenseChangeRate >= 0
                  ? "trend-up"
                  : "trend-down"
              }
            >
              {summary.monthlyExpenseChangeRate >= 0 ? "▲" : "▼"}{" "}
              {Math.abs(summary.monthlyExpenseChangeRate).toFixed(1)}%
            </span>
            <span className="text-muted"> 전월 대비</span>
          </div>
        </div>

        <div className="panel stat-card">
          <div className="stat-title">예산 초과 주의 인원</div>
          <div className="stat-value">{summary.overBudgetCount}명</div>
          <div className="stat-footer text-muted">80% 이상 소진</div>
        </div>
      </div>

      {/* 오늘의 결재 현황 (두 번째 코드의 핵심 기능) */}
      <div className="panel" style={{ marginTop: "24px" }}>
        <div className="section-title">오늘의 결재 현황</div>
        <div
          className="today-status-grid"
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(4, 1fr)",
            gap: "15px",
            marginTop: "15px",
          }}
        >
          {[
            {
              label: "신규 상신",
              value: summary.todaySubmittedCount,
              color: "#1f2937",
            },
            {
              label: "처리 완료",
              value: summary.todayProcessedCount,
              color: "#1f2937",
            },
            {
              label: "승인",
              value: summary.todayApprovedCount,
              color: "#059669",
            },
            {
              label: "반려",
              value: summary.todayRejectedCount,
              color: "#dc2626",
            },
          ].map((item, i) => (
            <div
              key={i}
              style={{
                textAlign: "center",
                padding: "15px",
                background: "#f9fafb",
                borderRadius: "10px",
              }}
            >
              <div
                style={{
                  fontSize: "12px",
                  color: "#6b7280",
                  marginBottom: "5px",
                }}
              >
                {item.label}
              </div>
              <div
                style={{
                  fontSize: "20px",
                  fontWeight: "700",
                  color: item.color,
                }}
              >
                {item.value}건
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* 차트 영역 */}
      <div className="dashboard-row" style={{ marginTop: "24px" }}>
        <div className="panel" style={{ height: "400px" }}>
          <div className="section-title">부서별 지출 비중</div>
          <div style={{ height: "300px" }}>
            {departmentChart.length > 0 ? (
              <Doughnut
                data={deptChartData}
                options={{ maintainAspectRatio: false }}
              />
            ) : (
              "데이터 없음"
            )}
          </div>
        </div>
        <div className="panel" style={{ height: "400px" }}>
          <div className="section-title">항목별 지출 비중</div>
          <div style={{ height: "300px" }}>
            {categoryChart.length > 0 ? (
              <Bar
                data={catChartData}
                options={{ maintainAspectRatio: false }}
              />
            ) : (
              "데이터 없음"
            )}
          </div>
        </div>
      </div>

      {/* 예산 초과 리스트 */}
      <div className="panel" style={{ marginTop: "24px" }}>
        <div className="section-title">예산 초과 주의 인원 리스트</div>
        <table className="dashboard-table">
          <thead>
            <tr>
              <th>사원명</th>
              <th>부서</th>
              <th>예산 소진율</th>
              <th>잔여 예산</th>
            </tr>
          </thead>
          <tbody>
            {overBudgetList.map((p, i) => (
              <tr key={i}>
                <td>{p.name}</td>
                <td>{p.department}</td>
                <td>
                  <span
                    style={{
                      fontWeight: "700",
                      color: p.executionRate >= 80 ? "#e11d48" : "#333",
                    }}
                  >
                    {p.executionRate}%
                  </span>
                </td>
                <td>{formatCurrency(p.remaining)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default StatisticsDashboard;
