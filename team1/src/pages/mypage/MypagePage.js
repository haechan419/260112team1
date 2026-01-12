import React, { useState, useEffect } from "react";
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

import axios from "axios";
import { API_SERVER_HOST } from "../../util/jwtUtil";
import useCustomLogin from "../../hooks/useCustomLogin";
import FaceRegister from "../../components/face/FaceRegister";

const MypagePage = () => {
  // 기존 상태들
  const [myInfo, setMyInfo] = useState(null);
  const [attendance, setAttendance] = useState([]);
  const [todayAttendance, setTodayAttendance] = useState(null);
  const [selectedDate, setSelectedDate] = useState(new Date());
  const [loading, setLoading] = useState(true);
  const [viewYear, setViewYear] = useState(new Date().getFullYear());
  const [viewMonth, setViewMonth] = useState(new Date().getMonth() + 1);

  const { loginState } = useCustomLogin(); // 현재 로그인한 사번 가져오기 위함
  const [isRegistered, setIsRegistered] = useState(false); // 얼굴 등록 여부
  const [isFaceLoading, setIsFaceLoading] = useState(true); // 얼굴 상태 로딩

  const today = new Date();
  const todayYear = today.getFullYear();
  const todayMonth = today.getMonth() + 1;
  const todayStr = today.toISOString().split("T")[0];

  // 초기 데이터 로드 (내정보 + 출결)
  useEffect(() => {
    loadInitialData();
  }, []);

  // Face ID 등록 여부 확인
  useEffect(() => {
    if (loginState.employeeNo) {
      checkFaceStatus();
    }
  }, [loginState.employeeNo]);

  // --- Face ID 상태 확인 함수 ---
  const checkFaceStatus = async () => {
    try {
      const res = await axios.get(`${API_SERVER_HOST}/api/face/check`, {
        params: { userId: loginState.employeeNo },
      });
      setIsRegistered(res.data); // true or false
    } catch (error) {
      console.error("Face ID 상태 확인 실패:", error);
    } finally {
      setIsFaceLoading(false);
    }
  };

  // ---얼굴 데이터 삭제 함수 ---
  const handleDeleteFace = async () => {
    if (
      !window.confirm(
        "정말 얼굴 인증 데이터를 삭제하시겠습니까?\n삭제 후엔 Face ID 로그인을 할 수 없습니다."
      )
    ) {
      return;
    }

    try {
      const res = await axios.delete(`${API_SERVER_HOST}/api/face/remove`, {
        params: { userId: loginState.employeeNo },
      });

      if (res.data.result === "success") {
        alert("삭제되었습니다.");
        setIsRegistered(false); // 상태 변경 -> 등록 화면으로 전환
      }
    } catch (error) {
      console.error("삭제 실패:", error);
      alert("삭제 중 오류가 발생했습니다.");
    }
  };

  // --- 등록 성공 시 실행될 콜백 ---
  const onRegisterSuccess = () => {
    alert("얼굴 등록이 완료되었습니다!");
    setIsRegistered(true); // 상태 변경 -> 삭제 버튼 화면으로 전환
  };

  const loadInitialData = async () => {
    try {
      setLoading(true);
      const [infoData, attendanceData] = await Promise.all([
        getMyInfo(),
        getMonthlyAttendance(todayYear, todayMonth),
      ]);
      setMyInfo(infoData);
      setAttendance(attendanceData);

      const todayRecord = attendanceData.find((a) => a.date === todayStr);
      setTodayAttendance(todayRecord || null);
    } catch (error) {
      console.error("데이터 로드 실패:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleMonthChange = async ({ activeStartDate }) => {
    const year = activeStartDate.getFullYear();
    const month = activeStartDate.getMonth() + 1;
    setViewYear(year);
    setViewMonth(month);

    try {
      const data = await getMonthlyAttendance(year, month);
      setAttendance(data);
    } catch (error) {
      console.error("출결 조회 실패:", error);
    }
  };

  const handleCheckIn = async () => {
    try {
      const result = await checkIn();
      alert("출근 처리되었습니다!");
      setTodayAttendance(result);

      if (viewYear === todayYear && viewMonth === todayMonth) {
        const data = await getMonthlyAttendance(todayYear, todayMonth);
        setAttendance(data);
      }
    } catch (error) {
      console.error("출근 에러:", error);
      const message = error.response?.data || "출근 처리 실패";
      alert(typeof message === "string" ? message : "출근 처리 실패");
    }
  };

  const handleCheckOut = async () => {
    try {
      const result = await checkOut();
      alert("퇴근 처리되었습니다!");
      setTodayAttendance(result);

      if (viewYear === todayYear && viewMonth === todayMonth) {
        const data = await getMonthlyAttendance(todayYear, todayMonth);
        setAttendance(data);
      }
    } catch (error) {
      console.error("퇴근 에러:", error);
      const message = error.response?.data || "퇴근 처리 실패";
      alert(typeof message === "string" ? message : "퇴근 처리 실패");
    }
  };

  const canCheckIn = !todayAttendance;
  const canCheckOut = todayAttendance && !todayAttendance.checkOutTime;

  const calculateSummary = (data) => {
    return {
      present: data.filter((d) => d.status === "PRESENT").length,
      late: data.filter((d) => d.status === "LATE").length,
      absent: data.filter((d) => d.status === "ABSENT").length,
      leave: data.filter((d) => d.status === "LEAVE").length,
    };
  };

  const summary = calculateSummary(attendance);

  const tileClassName = ({ date }) => {
    const dateStr = date.toISOString().split("T")[0];
    const record = attendance.find((a) => a.date === dateStr);
    if (!record) return null;
    return record.status.toLowerCase();
  };

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
          {/* 왼쪽 컬럼 */}
          <div className="mypage-left">
            {/* 내정보 카드*/}
            <div className="panel info-card">
              <div className="section-title">내정보</div>
              <div className="info-grid">
                <span className="info-label">이름</span>
                <span className="info-value">{myInfo?.name || "-"}</span>
                <span className="info-label">생년월일</span>
                <span className="info-value">{myInfo?.birthDate || "-"}</span>
                <span className="info-label">연락처</span>
                <span className="info-value">{myInfo?.phone || "-"}</span>
                <span className="info-label">이메일</span>
                <span className="info-value">{myInfo?.email || "-"}</span>
                <span className="info-label">사번</span>
                <span className="info-value">{myInfo?.employeeNo || "-"}</span>
                <span className="info-label">주소</span>
                <span className="info-value">{myInfo?.address || "-"}</span>
                <span className="info-label">상세주소</span>
                <span className="info-value">
                  {myInfo?.addressDetail || "-"}
                </span>
                <span className="info-label">부서</span>
                <span className="info-value">
                  {myInfo?.departmentName || "-"}
                </span>
                <span className="info-label">직급</span>
                <span className="info-value">{myInfo?.position || "-"}</span>
                <span className="info-label">입사일</span>
                <span className="info-value">{myInfo?.hireDate || "-"}</span>
              </div>
            </div>

            {/* 출결현황 그래프 */}
            <div className="panel chart-card">
              <div className="section-title">출결 현황 ({viewMonth}월)</div>
              <div className="chart-container">
                <div className="chart-row">
                  <div className="chart-indicator present"></div>
                  <span className="chart-label">출근</span>
                  <span className="chart-value">{summary.present}회</span>
                </div>
                <div className="chart-row">
                  <div className="chart-indicator late"></div>
                  <span className="chart-label">지각</span>
                  <span className="chart-value">{summary.late}회</span>
                </div>
                <div className="chart-row">
                  <div className="chart-indicator absent"></div>
                  <span className="chart-label">결근</span>
                  <span className="chart-value">{summary.absent}회</span>
                </div>
                <div className="chart-row">
                  <div className="chart-indicator leave"></div>
                  <span className="chart-label">휴가</span>
                  <span className="chart-value">{summary.leave}회</span>
                </div>
              </div>
            </div>

            {/* 3. 출퇴근 버튼 (기존 유지) */}
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

            {/* Face ID 등록/관리 카드 */}
            <div className="panel face-card" style={{ marginTop: "20px" }}>
              <div className="section-title">🔐 보안 설정 (Face ID)</div>

              {isFaceLoading ? (
                <div
                  style={{
                    textAlign: "center",
                    padding: "20px",
                    color: "#666",
                  }}
                >
                  상태 확인 중...
                </div>
              ) : (
                <>
                  {/* 등록 여부에 따라 다른 화면 송출 */}
                  {isRegistered ? (
                    // 등록된 경우: 확인 메시지 + 삭제 버튼
                    <div style={{ textAlign: "center", padding: "10px" }}>
                      <div
                        style={{
                          color: "#27ae60",
                          fontWeight: "bold",
                          marginBottom: "15px",
                          fontSize: "1.1rem",
                        }}
                      >
                        ✅ Face ID가 등록되어 있습니다.
                      </div>
                      <p
                        style={{
                          fontSize: "13px",
                          color: "#7f8c8d",
                          marginBottom: "20px",
                        }}
                      >
                        얼굴 인증으로 간편하게 로그인하세요.
                        <br />
                        재등록 하려면 삭제 후 다시 진행해주세요.
                      </p>
                      <button
                        onClick={handleDeleteFace}
                        style={{
                          padding: "8px 16px",
                          backgroundColor: "#ff6b6b",
                          color: "white",
                          border: "none",
                          borderRadius: "4px",
                          cursor: "pointer",
                          fontWeight: "bold",
                        }}
                      >
                        🗑️ 데이터 삭제
                      </button>
                    </div>
                  ) : (
                    // 미등록 경우: 등록 컴포넌트(카메라) 표시
                    <FaceRegister onSuccess={onRegisterSuccess} />
                  )}
                </>
              )}
            </div>
          </div>

          {/* 오른쪽: 달력 (기존 유지) */}
          <div className="mypage-right">
            <div className="panel calendar-card">
              <div className="section-title">
                {viewYear}년 {viewMonth}월
              </div>
              <Calendar
                onChange={setSelectedDate}
                value={selectedDate}
                locale="ko-KR"
                calendarType="gregory"
                tileClassName={tileClassName}
                onActiveStartDateChange={handleMonthChange}
              />
              <div className="calendar-legend">
                <div className="legend-item">
                  <span className="legend-dot present"></span>
                  <span>출근</span>
                </div>
                <div className="legend-item">
                  <span className="legend-dot late"></span>
                  <span>지각</span>
                </div>
                <div className="legend-item">
                  <span className="legend-dot absent"></span>
                  <span>결근</span>
                </div>
                <div className="legend-item">
                  <span className="legend-dot leave"></span>
                  <span>휴가</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </AppLayout>
  );
};

export default MypagePage;
