import React, { useEffect, useState } from "react";
import AppLayout from "../../../components/layout/AppLayout";
import {
  getAttendanceList,
  getDepartments,
  downloadAttendanceExcel,
} from "../../../api/attendanceApi";
import "./AttendanceManagePage.css";

const AttendanceManagePage = () => {
  // 현재 날짜 기준으로 연도/월 초기값 설정
  const today = new Date();
  const [selectedYear, setSelectedYear] = useState(today.getFullYear());
  const [selectedMonth, setSelectedMonth] = useState(today.getMonth() + 1);
  const [selectedDepartment, setSelectedDepartment] = useState("");
  const [loading, setLoading] = useState(false);
  const [downloading, setDownloading] = useState(null); // 다운로드 중인 부서명

  // 데이터
  const [attendanceData, setAttendanceData] = useState([]);
  const [departments, setDepartments] = useState([]);

  // 연도 옵션 (현재 연도 기준 ±2년)
  const yearOptions = [];
  for (let y = today.getFullYear() - 2; y <= today.getFullYear() + 1; y++) {
    yearOptions.push(y);
  }

  // 월 옵션
  const monthOptions = Array.from({ length: 12 }, (_, i) => i + 1);

  // 부서 목록 로딩 (최초 1회)
  useEffect(() => {
    loadDepartments();
  }, []);

  // 출결 데이터 로딩 (필터 변경 시)
  useEffect(() => {
    loadAttendanceData();
  }, [selectedYear, selectedMonth, selectedDepartment]);

  const loadDepartments = async () => {
    try {
      const response = await getDepartments();
      if (response.success) {
        setDepartments(response.departments || []);
      }
    } catch (error) {
      console.error("부서 목록 조회 실패:", error);
      setDepartments([]);
    }
  };

  const loadAttendanceData = async () => {
    setLoading(true);
    try {
      const response = await getAttendanceList(
        selectedYear,
        selectedMonth,
        selectedDepartment
      );

      if (response.success) {
        setAttendanceData(response.data || []);
      }
    } catch (error) {
      console.error("출결 데이터 조회 실패:", error);
      setAttendanceData([]);
    } finally {
      setLoading(false);
    }
  };

  // 엑셀 다운로드
  const handleDownload = async (item) => {
    setDownloading(item.department);
    try {
      console.log("엑셀 다운로드 시작:", item);
      
      const blob = await downloadAttendanceExcel(
        item.year,
        item.month,
        item.department
      );

      console.log("엑셀 다운로드 응답 받음:", blob);

      // 응답이 Blob인지 확인
      if (!(blob instanceof Blob)) {
        console.error("응답이 Blob이 아님:", typeof blob, blob);
        // JSON 에러 응답인 경우
        if (blob && typeof blob === 'object' && blob.message) {
          alert(`엑셀 다운로드 실패: ${blob.message}`);
          return;
        }
        throw new Error("예상치 못한 응답 형식입니다.");
      }

      // Blob이 비어있는지 확인
      if (blob.size === 0) {
        console.error("Blob이 비어있음");
        alert("다운로드할 파일이 없습니다.");
        return;
      }

      console.log("Blob 크기:", blob.size, "bytes");

      // Blob URL 생성 및 다운로드
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download =
        item.fileName ||
        `${item.year}년_${item.month}월_${item.department}_출결현황.xlsx`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      
      console.log("엑셀 다운로드 완료");
    } catch (error) {
      console.error("엑셀 다운로드 실패:", error);
      console.error("에러 상세:", {
        message: error.message,
        response: error.response,
        stack: error.stack
      });
      
      // 에러 응답에서 메시지 추출 시도
      let errorMessage = "엑셀 다운로드에 실패했습니다.";
      if (error.response) {
        // 백엔드에서 JSON 에러 응답을 보낸 경우
        if (error.response.data && typeof error.response.data === 'object') {
          try {
            // Blob 응답인 경우 텍스트로 변환 시도
            if (error.response.data instanceof Blob) {
              const text = await error.response.data.text();
              const json = JSON.parse(text);
              errorMessage = json.message || errorMessage;
            } else {
              errorMessage = error.response.data.message || errorMessage;
            }
          } catch (e) {
            // JSON 파싱 실패 시 기본 메시지 사용
            errorMessage = error.response.data.message || errorMessage;
          }
        }
      } else if (error.message) {
        errorMessage = error.message;
      }
      
      alert(errorMessage);
    } finally {
      setDownloading(null);
    }
  };

  // 초기화 버튼
  const handleReset = () => {
    setSelectedYear(today.getFullYear());
    setSelectedMonth(today.getMonth() + 1);
    setSelectedDepartment("");
  };

  return (
    <AppLayout>
      <div className="attendance-manage-page">
        {/* 헤더 */}
        <div className="page-header">
          <div className="page-title-section">
            <h1 className="page-title">출결 관리</h1>
          </div>
          <p className="page-description">
            부서별, 월별 출결 현황을 조회하고 엑셀 파일로 다운로드할 수
            있습니다.
          </p>
        </div>

        {/* 필터 영역 */}
        <div className="filter-section">
          <div className="filter-row">
            {/* 연도 선택 */}
            <div className="filter-item">
              <label>연도</label>
              <select
                className="form-select"
                value={selectedYear}
                onChange={(e) => setSelectedYear(Number(e.target.value))}
              >
                {yearOptions.map((year) => (
                  <option key={year} value={year}>
                    {year}년
                  </option>
                ))}
              </select>
            </div>

            {/* 월 선택 */}
            <div className="filter-item">
              <label>월</label>
              <select
                className="form-select"
                value={selectedMonth}
                onChange={(e) => setSelectedMonth(Number(e.target.value))}
              >
                {monthOptions.map((month) => (
                  <option key={month} value={month}>
                    {month}월
                  </option>
                ))}
              </select>
            </div>

            {/* 부서 선택 */}
            <div className="filter-item">
              <label>부서</label>
              <select
                className="form-select"
                value={selectedDepartment}
                onChange={(e) => setSelectedDepartment(e.target.value)}
              >
                <option value="">전체</option>
                {departments.map((dept) => (
                  <option key={dept} value={dept}>
                    {dept}
                  </option>
                ))}
              </select>
            </div>

            {/* 버튼 */}
            <div className="filter-actions">
              <button className="btn btn-secondary" onClick={handleReset}>
                초기화
              </button>
            </div>
          </div>
        </div>

        {/* 테이블 상단 정보 */}
        <div className="table-header">
          <span className="total-count">
            총 <strong>{attendanceData.length}</strong>개 부서
          </span>
        </div>

        {/* 테이블 */}
        <div className="table-container">
          {loading ? (
            <div className="loading">로딩 중...</div>
          ) : attendanceData.length === 0 ? (
            <div className="empty-state">
              해당 조건의 출결 데이터가 없습니다.
            </div>
          ) : (
            <table className="attendance-table">
              <thead>
                <tr>
                  <th>부서</th>
                  <th>기간</th>
                  <th>인원</th>
                  <th>출근</th>
                  <th>지각</th>
                  <th>결근</th>
                  <th>휴가</th>
                  <th>다운로드</th>
                </tr>
              </thead>
              <tbody>
                {attendanceData.map((item, index) => (
                  <tr key={index}>
                    <td>
                      <span className="department-badge">
                        {item.department}
                      </span>
                    </td>
                    <td>
                      {item.year}년 {item.month}월
                    </td>
                    <td>{item.totalEmployees}명</td>
                    <td>
                      <span className="status-badge status-present">
                        {item.presentCount}
                      </span>
                    </td>
                    <td>
                      <span className="status-badge status-late">
                        {item.lateCount}
                      </span>
                    </td>
                    <td>
                      <span className="status-badge status-absent">
                        {item.absentCount}
                      </span>
                    </td>
                    <td>
                      <span className="status-badge status-leave">
                        {item.leaveCount}
                      </span>
                    </td>
                    <td>
                      <button
                        className="btn btn-download"
                        onClick={() => handleDownload(item)}
                        disabled={downloading === item.department}
                      >
                        {downloading === item.department
                          ? "⏳ 생성중..."
                          : "📥 엑셀"}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {/* 안내 문구 */}
        <div className="info-box">
          <p>
            💡 출결 현황은 평일 09:00 ~ 18:00 사이에 매 시간 자동
            업데이트됩니다.
          </p>
          <p>💡 엑셀 파일은 다운로드 버튼 클릭 시 실시간으로 생성됩니다.</p>
        </div>
      </div>
    </AppLayout>
  );
};

export default AttendanceManagePage;
