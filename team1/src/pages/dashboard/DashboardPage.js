import React, { useEffect, useState, useMemo, useCallback } from "react";
import { useDispatch, useSelector } from "react-redux";
import AppLayout from "../../components/layout/AppLayout";
import TodoCalendar from "../../components/todo/TodoCalendar";
import TodoList from "../../components/todo/TodoList";
import MeetingNoteUpload from "../../components/todo/MeetingNoteUpload";
import { fetchTodos, fetchActiveTodos, fetchOverdueTodos } from "../../slices/todoSlice";
import { getMonthlyExpenseTrend } from "../../api/accountingApi";
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler,
} from "chart.js";
import { Line } from "react-chartjs-2";
import "../../styles/layout.css";
import "../../styles/dashboard.css";
import "./DashboardPage.css";
import "./MonthlyExpenseChart.css";

// Chart.js 등록
ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler
);

export default function DashboardPage() {
  const dispatch = useDispatch();
  const { todos, activeTodos, overdueTodos } = useSelector((state) => state.todo);
  const loginState = useSelector((state) => state.loginSlice);
  const [monthlyExpenseData, setMonthlyExpenseData] = useState([]);
  const [loading, setLoading] = useState(false);

  // ADMIN 권한 체크
  const isAdmin = useMemo(() => {
    return loginState?.roleNames?.includes("ADMIN") ||
      loginState?.role === "ADMIN" ||
      loginState?.roleName === "ADMIN" ||
      false;
  }, [loginState]);

  // 월별 지출 추이 데이터 로드 함수
  const loadMonthlyExpenseTrend = useCallback(async () => {
    // ADMIN이 아닌 경우 데이터 로드하지 않음
    if (!isAdmin) {
      return;
    }

    setLoading(true);
    try {
      const data = await getMonthlyExpenseTrend({ status: "APPROVED" });
      setMonthlyExpenseData(data || []);
    } catch (error) {
      console.error("월별 지출 추이 조회 실패:", error);
      setMonthlyExpenseData([]);
    } finally {
      setLoading(false);
    }
  }, [isAdmin]);

  useEffect(() => {
    // 초기 데이터 로드
    dispatch(fetchTodos());
    dispatch(fetchActiveTodos());
    dispatch(fetchOverdueTodos());

    // ADMIN인 경우에만 월별 지출 추이 데이터 로드
    if (isAdmin) {
      loadMonthlyExpenseTrend();
    }
  }, [dispatch, loadMonthlyExpenseTrend, isAdmin]);

  // 차트 데이터 준비 (useMemo로 최적화)
  const chartData = useMemo(() => ({
    labels: monthlyExpenseData.map((item) => {
      const [year, month] = item.yearMonth.split("-");
      return `${year}년 ${parseInt(month)}월`;
    }),
    datasets: [
      {
        label: "월별 지출액",
        data: monthlyExpenseData.map((item) => item.amount),
        borderColor: "rgb(59, 130, 246)",
        backgroundColor: "rgba(59, 130, 246, 0.1)",
        borderWidth: 2,
        fill: true,
        tension: 0.4,
        pointRadius: 4,
        pointHoverRadius: 6,
        pointBackgroundColor: "rgb(59, 130, 246)",
        pointBorderColor: "#fff",
        pointBorderWidth: 2,
      },
    ],
  }), [monthlyExpenseData]);

  // 차트 옵션 (useMemo로 최적화)
  const chartOptions = useMemo(() => ({
    responsive: true,
    maintainAspectRatio: false, // 컨테이너 크기에 맞춤
    plugins: {
      legend: {
        display: true,
        position: "top",
      },
      tooltip: {
        callbacks: {
          label: function (context) {
            const value = context.parsed.y;
            return `지출액: ${new Intl.NumberFormat("ko-KR").format(value)}원`;
          },
        },
      },
    },
    scales: {
      y: {
        beginAtZero: true,
        ticks: {
          callback: function (value) {
            return new Intl.NumberFormat("ko-KR").format(value) + "원";
          },
        },
        grid: {
          color: "rgba(0, 0, 0, 0.05)",
        },
      },
      x: {
        grid: {
          display: false,
        },
      },
    },
  }), []);

  return (
    <AppLayout>
      <div className="report-page">
        <div className="page-meta">SmartSpend ERP</div>
        <h1 className="page-title">Dashboard</h1>

        {/* 상단 통계 카드 */}
        <div className="dashboard-grid">
          <div className="panel stat-card">
            <div>
              <div className="stat-title">전체 Todo</div>
              <div className="stat-value">{todos.length}건</div>
            </div>
            <div className="stat-footer text-muted">전체 할 일</div>
          </div>

          <div className="panel stat-card">
            <div>
              <div className="stat-title">진행 중 Todo</div>
              <div className="stat-value">{activeTodos.length}건</div>
            </div>
            <div className="stat-footer text-muted">미완료 할 일</div>
          </div>

          <div className="panel stat-card">
            <div>
              <div className="stat-title">마감일 지난 Todo</div>
              <div className={`stat-value ${overdueTodos.length > 0 ? "stat-value-warning" : ""}`}>
                {overdueTodos.length}건
              </div>
            </div>
            <div className="stat-footer text-muted">처리 필요</div>
          </div>

          <div className="panel stat-card">
            <div>
              <div className="stat-title">완료된 Todo</div>
              <div className="stat-value">
                {todos.filter((t) => t.status === "DONE").length}건
              </div>
            </div>
            <div className="stat-footer text-muted">이번 달 누적</div>
          </div>
        </div>

        {/* Todo 관련 컴포넌트 - 같은 줄에 배치 */}
        <div className="dashboard-todo-row">
          <div className="panel dashboard-todo-panel">
            <TodoCalendar />
          </div>

          <div className="panel dashboard-todo-panel">
            <TodoList />
          </div>

          <div className="panel dashboard-todo-panel">
            <MeetingNoteUpload />
          </div>
        </div>

        {/* 월별 지출 추이 - ADMIN만 보이도록 조건부 렌더링 */}
        {isAdmin && (
          <div className="monthly-expense-row">
            <div className="panel monthly-expense-chart-panel">
              <div className="section-title">월별 지출 추이</div>
              <div className="monthly-expense-chart-container">
                {loading ? (
                  <div className="monthly-expense-chart-loading">
                    로딩 중...
                  </div>
                ) : monthlyExpenseData.length === 0 ? (
                  <div className="monthly-expense-chart-empty">
                    데이터가 없습니다.
                  </div>
                ) : (
                  <Line data={chartData} options={chartOptions} />
                )}
              </div>
            </div>
          </div>
        )}
      </div>
    </AppLayout>
  );
}