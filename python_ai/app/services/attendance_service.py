"""
출결 데이터 처리 + 엑셀 생성
"""
import requests
import pandas as pd
import os
import uuid
from datetime import datetime
from app.services.ollama_service import OllamaService

class AttendanceService:
    def __init__(self, ollama_service: OllamaService):
        self.ollama_service = ollama_service
        self.spring_boot_url = "http://localhost:8080"
        self.output_dir = "generated"
        os.makedirs(self.output_dir, exist_ok=True)
    
    def process_query(self, user_prompt: str) -> dict:
        """메인 처리 로직"""
        
        try:
            # 1. AI 의도 분석
            print(f"[AttendanceService] 사용자 질문: {user_prompt}")
            intent_data = self.ollama_service.analyze_intent(user_prompt)
            print(f"[AttendanceService] AI 의도 분석 결과: {intent_data}")
            
            intent = intent_data.get("intent", "UNKNOWN")
            
            if intent == "UNKNOWN":
                return {"ok": False, "message": "출결 관련 질문을 해주세요. (예: '재무팀 1월 출결 엑셀로 뽑아줘')", "hasFile": False}
            
            # 2. 엑셀 or 요약 처리
            if "EXCEL" in intent:
                return self._handle_excel_request(intent_data)
            if "SUMMARY" in intent:
                return self._handle_summary_request(intent_data)
            
            return {"ok": False, "message": f"처리 불가: intent={intent}", "hasFile": False}
        except Exception as e:
            print(f"[AttendanceService] 처리 중 오류: {e}")
            import traceback
            traceback.print_exc()
            return {"ok": False, "message": f"처리 중 오류가 발생했습니다: {str(e)}", "hasFile": False}
    
    def _handle_excel_request(self, intent_data: dict) -> dict:
        """엑셀 생성"""
        department = intent_data.get("department")
        employee_name = intent_data.get("employeeName")
        year = intent_data.get("year")
        month = intent_data.get("month")
        
        # Spring Boot에서 데이터 조회
        data = self._fetch_attendance_data(department, employee_name, year, month)
        if not data:
            return {"ok": False, "message": "데이터 없음", "hasFile": False}
        
        # 엑셀 생성
        filename = self._generate_excel(data, department, employee_name, year, month)
        summary = self._create_summary(data)
        target = employee_name or department or "전체"
        
        return {
            "ok": True,
            "message": f"{target}의 {year}년 {month}월 출결 현황입니다.",
            "summary": summary,
            "hasFile": True,
            "downloadUrl": f"/api/ai/attendance/download/{filename}",
            "fileName": filename
        }
    
    def _fetch_attendance_data(self, department, employee_name, year, month) -> list:
        """Spring Boot API 호출"""
        try:
            if not department:
                print(f"[API Error] 부서 정보가 없습니다. department={department}, employee_name={employee_name}")
                return []
            
            url = f"{self.spring_boot_url}/api/internal/attendance/detail"
            params = {"year": year, "month": month, "department": department}
            print(f"[API Request] {url}?year={year}&month={month}&department={department}")
            
            res = requests.get(url, params=params, timeout=10)
            print(f"[API Response] status={res.status_code}")
            
            if res.status_code == 200:
                response_json = res.json()
                print(f"[API Response] data keys: {response_json.keys()}")
                
                data_obj = response_json.get("data", {})
                details = data_obj.get("details", [])
                print(f"[API Response] details count: {len(details)}")
                
                if employee_name:
                    details = [d for d in details if employee_name in d.get("employeeName", "")]
                    print(f"[API Response] filtered by employee_name '{employee_name}': {len(details)}건")
                
                return details
            else:
                error_text = res.text
                print(f"[API Error] HTTP {res.status_code}: {error_text}")
                return []
        except requests.exceptions.RequestException as e:
            print(f"[API Error] 요청 실패: {e}")
            return []
        except Exception as e:
            print(f"[API Error] 예상치 못한 오류: {e}")
            import traceback
            traceback.print_exc()
            return []
    
    def _generate_excel(self, data, department, employee_name, year, month) -> str:
        """pandas로 엑셀 생성"""
        target = employee_name or department or "전체"
        filename = f"{target}_{year}년_{month}월_출결_{uuid.uuid4().hex[:8]}.xlsx"
        filepath = os.path.join(self.output_dir, filename)
        
        df = pd.DataFrame(data)
        df = df.rename(columns={
            "employeeNo": "사번", "employeeName": "이름",
            "date": "날짜", "statusKorean": "상태",
            "checkInTime": "출근시간", "checkOutTime": "퇴근시간"
        })
        
        df.to_excel(filepath, sheet_name='출결현황', index=False)
        return filename
    
    def _handle_summary_request(self, intent_data: dict) -> dict:
        """요약 통계 처리"""
        department = intent_data.get("department")
        employee_name = intent_data.get("employeeName")
        year = intent_data.get("year")
        month = intent_data.get("month")
        
        # 직원명만 있고 부서가 없으면, 부서를 찾아야 함
        if employee_name and not department:
            # Spring Boot에서 모든 부서 데이터를 조회하여 해당 직원 찾기
            data = self._fetch_attendance_data_for_employee(employee_name, year, month)
        else:
            # Spring Boot에서 데이터 조회
            data = self._fetch_attendance_data(department, employee_name, year, month)
        
        if not data:
            target = employee_name or department or "전체"
            return {"ok": False, "message": f"{target}의 {year}년 {month}월 출결 데이터가 없습니다.", "hasFile": False}
        
        # 통계 요약 생성
        summary = self._create_summary(data)
        target = employee_name or department or "전체"
        
        return {
            "ok": True,
            "message": f"{target}의 {year}년 {month}월 출결 통계입니다.",
            "summary": summary,
            "hasFile": False,
            "downloadUrl": None,
            "fileName": None
        }
    
    def _fetch_attendance_data_for_employee(self, employee_name, year, month) -> list:
        """직원명만 있을 때 모든 부서에서 검색"""
        valid_departments = ["개발1팀", "개발2팀", "인사팀", "재무팀", "영업팀", "마케팅팀", "기획팀", "디자인팀"]
        
        for dept in valid_departments:
            data = self._fetch_attendance_data(dept, employee_name, year, month)
            if data:
                return data
        return []
    
    def _create_summary(self, data) -> str:
        """통계 요약 텍스트"""
        total = len(data)
        present = sum(1 for d in data if d.get("statusKorean") == "출근")
        late = sum(1 for d in data if d.get("statusKorean") == "지각")
        absent = sum(1 for d in data if d.get("statusKorean") == "결근")
        leave = sum(1 for d in data if d.get("statusKorean") == "휴가")
        
        return f"""📊 출결 통계
- 총 기록: {total}건
- 출근: {present}건
- 지각: {late}건
- 결근: {absent}건
- 휴가: {leave}건"""