import React, { useState, useEffect, useMemo, useCallback } from "react";
import Calendar from "react-calendar";
import "react-calendar/dist/Calendar.css";
import "./MypagePage.css";
import AppLayout from "../../components/layout/AppLayout";
import {
  getMyInfo,
  getMonthlyAttendance,
  checkIn,
  checkOut,
} from "../../api/mypageApi";

// 로컬 시간 기준으로 날짜를 YYYY-MM-DD 형식으로 포맷 (컴포넌트 외부)
const formatLocalDate = (date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

// 내정보 필드 정의
const INFO_FIELDS = [
  { key: "name", label: "이름" },
  { key: "birthDate", label: "생년월일" },
  { key: "phone", label: "연락처" },
  { key: "email", label: "이메일" },
  { key: "employeeNo", label: "사번" },
  { key: "address", label: "주소" },
  { key: "addressDetail", label: "상세주소" },
  { key: "departmentName", label: "부서" },
  { key: "position", label: "직급" },
  { key: "hireDate", label: "입사일" },
];

// 출결 상태 정의
const ATTENDANCE_TYPES = [
  { key: "present", label: "출근", status: "PRESENT" },
  { key: "late", label: "지각", status: "LATE" },
  { key: "absent", label: "결근", status: "ABSENT" },
  { key: "leave", label: "휴가", status: "LEAVE" },
];

const MypagePage = () => {
  const [myInfo, setMyInfo] = useState(null);
  const [attendance, setAttendance] = useState([]);
  const [todayAttendance, setTodayAttendance] = useState(null);
  const [selectedDate, setSelectedDate] = useState(new Date());
  const [loading, setLoading] = useState(true);
  const [viewYear, setViewYear] = useState(new Date().getFullYear());
  const [viewMonth, setViewMonth] = useState(new Date().getMonth() + 1);

  // 오늘 날짜 정보 (메모이제이션)
  const todayInfo = useMemo(() => {
    const today = new Date();
    return {
      year: today.getFullYear(),
      month: today.getMonth() + 1,
      str: formatLocalDate(today),
    };
  }, []);

  // 출결 데이터 새로고침 함수
  const refreshAttendance = useCallback(async (year, month) => {
    try {
      const data = await getMonthlyAttendance(year, month);
      setAttendance(data);
      return data;
    } catch (error) {
      console.error("출결 조회 실패:", error);
      return [];
    }
  }, []);

  // 초기 데이터 로드
  useEffect(() => {
    const loadInitialData = async () => {
      try {
        setLoading(true);
        const [infoData, attendanceData] = await Promise.all([
          getMyInfo(),
          getMonthlyAttendance(todayInfo.year, todayInfo.month),
        ]);
        setMyInfo(infoData);
        setAttendance(attendanceData);

        const todayRecord = attendanceData.find(
          (a) => a.date === todayInfo.str
        );
        setTodayAttendance(todayRecord || null);
      } catch (error) {
        console.error("데이터 로드 실패:", error);
      } finally {
        setLoading(false);
      }
    };

    loadInitialData();
  }, [todayInfo]);

  // 월 변경 핸들러
  const handleMonthChange = useCallback(
    async ({ activeStartDate }) => {
      const year = activeStartDate.getFullYear();
      const month = activeStartDate.getMonth() + 1;
      setViewYear(year);
      setViewMonth(month);
      await refreshAttendance(year, month);
    },
    [refreshAttendance]
  );

  // 에러 메시지 추출 헬퍼
  const getErrorMessage = (error, defaultMsg) => {
    const message = error.response?.data || defaultMsg;
    return typeof message === "string" ? message : defaultMsg;
  };

  // 출근 핸들러
  const handleCheckIn = useCallback(async () => {
    try {
      const result = await checkIn();
      alert("출근 처리되었습니다!");
      setTodayAttendance(result);

      if (viewYear === todayInfo.year && viewMonth === todayInfo.month) {
        await refreshAttendance(todayInfo.year, todayInfo.month);
      }
    } catch (error) {
      console.error("출근 에러:", error);
      alert(getErrorMessage(error, "출근 처리 실패"));
    }
  }, [viewYear, viewMonth, todayInfo, refreshAttendance]);

  // 퇴근 핸들러
  const handleCheckOut = useCallback(async () => {
    try {
      const result = await checkOut();
      alert("퇴근 처리되었습니다!");
      setTodayAttendance(result);

      if (viewYear === todayInfo.year && viewMonth === todayInfo.month) {
        await refreshAttendance(todayInfo.year, todayInfo.month);
      }
    } catch (error) {
      console.error("퇴근 에러:", error);
      alert(getErrorMessage(error, "퇴근 처리 실패"));
    }
  }, [viewYear, viewMonth, todayInfo, refreshAttendance]);

  // 오늘로 이동 핸들러
  const handleGoToday = useCallback(async () => {
    const now = new Date();
    setSelectedDate(now);
    setViewYear(now.getFullYear());
    setViewMonth(now.getMonth() + 1);
    await refreshAttendance(now.getFullYear(), now.getMonth() + 1);
  }, [refreshAttendance]);

  // 버튼 활성화 상태
  const canCheckIn = !todayAttendance;
  const canCheckOut = todayAttendance && !todayAttendance.checkOutTime;

  // 출결 요약 계산 (메모이제이션)
  const summary = useMemo(() => {
    return ATTENDANCE_TYPES.reduce((acc, type) => {
      acc[type.key] = attendance.filter((d) => d.status === type.status).length;
      return acc;
    }, {});
  }, [attendance]);

  // 최대 카운트 (막대그래프 비율 계산용)
  const maxCount = useMemo(() => {
    return Math.max(...Object.values(summary), 1);
  }, [summary]);

  // 달력 타일 클래스 (메모이제이션)
  const tileClassName = useCallback(
    ({ date }) => {
      const dateStr = formatLocalDate(date);
      const record = attendance.find((a) => a.date === dateStr);
      if (!record) return null;
      return record.status.toLowerCase();
    },
    [attendance]
  );

  if (loading) {
    return (
      <AppLayout>
        <div className="mypage-loading">로딩 중...</div>
      </AppLayout>
    );
  }

  return (
    <AppLayout>
      <div className="mypage-wrapper">
        <div className="page-meta">SmartSpend ERP</div>
        <h1 className="page-title">마이페이지</h1>

        <div className="mypage-content">
          {/* 왼쪽: 내정보 + 버튼 */}
          <div className="mypage-left">
            {/* 내정보 카드 */}
            <div className="panel info-card">
              <div className="section-title">내정보</div>
              <div className="info-grid">
                {INFO_FIELDS.map(({ key, label }) => (
                  <React.Fragment key={key}>
                    <span className="info-label">{label}</span>
                    <span className="info-value">{myInfo?.[key] || "-"}</span>
                  </React.Fragment>
                ))}
              </div>
            </div>

            {/* 출퇴근 버튼 */}
            <div className="check-buttons">
              <button
                className="check-btn check-in"
                onClick={handleCheckIn}
                disabled={!canCheckIn}
              >
                출근하기
              </button>
              <button
                className="check-btn check-out"
                onClick={handleCheckOut}
                disabled={!canCheckOut}
              >
                퇴근하기
              </button>
            </div>
          </div>

          {/* 오른쪽: 달력 + 출결현황 */}
          <div className="mypage-right">
            {/* 달력 카드 */}
            <div className="panel calendar-card">
              <div className="calendar-header">
                <div className="section-title">
                  {myInfo?.name || "사용자"} 님 출결현황
                </div>
                <button className="today-btn" onClick={handleGoToday}>
                  오늘
                </button>
              </div>
              <Calendar
                onChange={setSelectedDate}
                value={selectedDate}
                locale="ko-KR"
                calendarType="gregory"
                tileClassName={tileClassName}
                onActiveStartDateChange={handleMonthChange}
                activeStartDate={new Date(viewYear, viewMonth - 1, 1)}
              />
              <div className="calendar-legend">
                {ATTENDANCE_TYPES.map(({ key, label }) => (
                  <div key={key} className="legend-item">
                    <span className={`legend-dot ${key}`}></span>
                    <span>{label}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* 출결현황 막대그래프 */}
            <div className="panel chart-card">
              <div className="section-title">출결 현황 ({viewMonth}월)</div>
              <div className="chart-container">
                {ATTENDANCE_TYPES.map(({ key, label }) => (
                  <div key={key} className="chart-row">
                    <span className="chart-label">{label}</span>
                    <div className="chart-bar-wrapper">
                      <div
                        className={`chart-bar ${key}`}
                        style={{ width: `${(summary[key] / maxCount) * 100}%` }}
                      ></div>
                    </div>
                    <span className="chart-value">{summary[key]}회</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </AppLayout>
  );
};

export default MypagePage;
