import jwtAxios from "../util/jwtUtil";

const API_BASE = "/admin/attendance";

// 부서별 출결 통계 조회
export const getAttendanceList = async (year, month, department = "") => {
  const params = { year, month };
  if (department) {
    params.department = department;
  }

  const response = await jwtAxios.get(API_BASE, { params });
  return response.data;
};

// 부서 목록 조회
export const getDepartments = async () => {
  const response = await jwtAxios.get(`${API_BASE}/departments`);
  return response.data;
};

// 부서별 출결 상세 조회
export const getAttendanceDetail = async (year, month, department) => {
  const params = { year, month, department };
  const response = await jwtAxios.get(`${API_BASE}/detail`, { params });
  return response.data;
};

// 출결 엑셀 다운로드
export const downloadAttendanceExcel = async (year, month, department) => {
  const params = { year, month, department };
  try {
    const response = await jwtAxios.get(`${API_BASE}/download`, {
      params,
      responseType: "blob",
    });

    // 응답 상태 확인
    if (response.status !== 200) {
      throw new Error(`다운로드 실패: HTTP ${response.status}`);
    }

    // 응답이 성공이지만 빈 blob인 경우 확인
    if (!response.data || response.data.size === 0) {
      // 빈 blob을 텍스트로 변환하여 에러 메시지 확인 시도
      try {
        const text = await response.data.text();
        const errorJson = JSON.parse(text);
        throw new Error(errorJson.message || "다운로드할 파일이 없습니다.");
      } catch (e) {
        throw new Error("다운로드할 파일이 없습니다.");
      }
    }

    return response.data;
  } catch (error) {
    console.error("엑셀 다운로드 에러:", error);

    // blob 에러 응답 처리
    if (error.response && error.response.data instanceof Blob) {
      try {
        const text = await error.response.data.text();
        const errorJson = JSON.parse(text);
        const newError = new Error(errorJson.message || "엑셀 다운로드에 실패했습니다.");
        newError.response = { ...error.response, data: errorJson };
        throw newError;
      } catch (e) {
        // JSON 파싱 실패 시 원본 에러 throw
        throw error;
      }
    }

    // 네트워크 에러나 기타 에러
    if (!error.response) {
      throw new Error("네트워크 오류가 발생했습니다. 서버 연결을 확인해주세요.");
    }

    throw error;
  }
};
