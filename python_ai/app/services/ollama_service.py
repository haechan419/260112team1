"""
Ollama 연동 - 자연어 → JSON 의도 추출
"""
import requests
import json
import re
from datetime import datetime

class OllamaService:
    def __init__(self, base_url: str = "http://localhost:11434"):
        self.base_url = base_url
        self.model = "qwen2.5:3b"
    
    def analyze_intent(self, user_prompt: str) -> dict:
        """사용자 질문 → JSON 의도 추출"""
        
        system_prompt = self._get_system_prompt()
        full_prompt = f"{system_prompt}\n\n사용자 질문: {user_prompt}\n\nJSON 응답:"
        
        try:
            response = requests.post(
                f"{self.base_url}/api/generate",
                json={
                    "model": self.model,
                    "prompt": full_prompt,
                    "stream": False,
                    "options": {"temperature": 0.1}
                },
                timeout=60
            )
            
            result = response.json()
            ai_response = result.get("response", "")
            # 원본 사용자 질문도 함께 전달 (검증/보정에 필요)
            return self._parse_json_response(ai_response, user_prompt)
            
        except Exception as e:
            print(f"[Ollama Error] {e}")
            return self._default_intent()
    
    def _get_system_prompt(self) -> str:
        """AI에게 역할 부여"""
        current_year = datetime.now().year
        current_month = datetime.now().month
        
        return f"""너는 출결 관리 AI 파서야. 사용자 질문에서 정보를 정확히 추출해서 JSON으로만 응답해.

가능한 intent:
- ATTENDANCE_EXCEL_DEPARTMENT: 부서 출결 엑셀
- ATTENDANCE_EXCEL_EMPLOYEE: 개인 출결 엑셀  
- ATTENDANCE_SUMMARY_DEPARTMENT: 부서 통계
- ATTENDANCE_SUMMARY_EMPLOYEE: 개인 통계
- UNKNOWN: 출결 외 질문

부서 목록 (정확히 매칭):
- 개발1팀, 개발2팀, 인사팀, 재무팀, 영업팀, 마케팅팀, 기획팀, 디자인팀
- "개발 1팀", "개발1팀", "개발 1 팀" → 모두 "개발1팀"으로 정규화

JSON 형식 (반드시 이 형식):
{{"intent": "ATTENDANCE_EXCEL_DEPARTMENT", "department": "개발1팀", "employeeName": null, "year": 2025, "month": 1}}

추출 규칙:
1. 연도: "2025년", "2025", "25년" → 2025 (4자리 숫자로 변환)
   - "25년"은 2000 + 25 = 2025
   - 연도 미언급 시: {current_year}
   
2. 월: "1월", "01월", "1", "01" → 1 (1-12 숫자)
   - "일월", "이월" 같은 한글 월도 숫자로 변환
   - 월 미언급 시: {current_month}
   
3. 부서: 질문에서 부서명 찾아서 정확히 매칭
   - "개발 1팀" → "개발1팀" (공백 제거)
   - "개발1팀" → "개발1팀"
   - 부서 미언급 시: null
   
4. 직원명: 이름이 명시되면 추출, 없으면 null
   - 부서명이 아닌 한글 이름(2-4글자) → 직원명으로 추출
   - 부서명과 함께 나오면 → 부서명은 department, 이름은 employeeName
   - 부서명 없이 이름만 나오면 → employeeName만 설정

5. Intent 판단:
   - "엑셀", "다운로드", "뽑아", "추출", "뽑아줘", "엑셀로" → EXCEL
   - "현황", "어때", "통계", "보여줘", "알려줘", "알려주세요", "보여주세요" → SUMMARY

예시:
- "개발1팀 2025년 1월 출결 엑셀로 뽑아줘" 
  → {{"intent": "ATTENDANCE_EXCEL_DEPARTMENT", "department": "개발1팀", "employeeName": null, "year": 2025, "month": 1}}
  
- "윤서현 2025년 1월 출결 엑셀"
  → {{"intent": "ATTENDANCE_EXCEL_EMPLOYEE", "department": null, "employeeName": "윤서현", "year": 2025, "month": 1}}

- "재무팀 1월 출결 현황"
  → {{"intent": "ATTENDANCE_SUMMARY_DEPARTMENT", "department": "재무팀", "employeeName": null, "year": {current_year}, "month": 1}}

- "윤서현 25년 1월 출결 알려주세요"
  → {{"intent": "ATTENDANCE_SUMMARY_EMPLOYEE", "department": null, "employeeName": "윤서현", "year": 2025, "month": 1}}

- "재무팀 윤서현 25년 1월 출결 알려주세요"
  → {{"intent": "ATTENDANCE_SUMMARY_EMPLOYEE", "department": "재무팀", "employeeName": "윤서현", "year": 2025, "month": 1}}

중요: 
- 질문에서 명시된 연도/월을 정확히 추출하고, 미언급 시에만 현재 날짜 사용
- 부서명과 직원명이 모두 있으면 → EMPLOYEE intent 사용 (부서명은 필터링용)
- 직원명만 있으면 → EMPLOYEE intent, department는 null
- 부서명만 있으면 → DEPARTMENT intent"""

    def _parse_json_response(self, response: str, user_prompt: str = None) -> dict:
        """JSON 응답 파싱 및 검증/보정"""
        try:
            # JSON 추출 (더 정확한 패턴)
            # 먼저 완전한 JSON 객체 찾기
            json_match = None
            brace_count = 0
            start_idx = -1
            
            for i, char in enumerate(response):
                if char == '{':
                    if start_idx == -1:
                        start_idx = i
                    brace_count += 1
                elif char == '}':
                    brace_count -= 1
                    if brace_count == 0 and start_idx != -1:
                        json_match = response[start_idx:i+1]
                        break
            
            if json_match:
                parsed = json.loads(json_match)
            else:
                # 간단한 패턴으로 재시도
                json_match = re.search(r'\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\}', response)
                if json_match:
                    parsed = json.loads(json_match.group())
                else:
                    parsed = json.loads(response)
            
            # 파싱된 결과 검증 및 보정 (원본 사용자 질문 전달)
            original_text = user_prompt if user_prompt else response
            return self._validate_and_fix_intent(parsed, original_text)
        except Exception as e:
            print(f"[JSON Parse Error] {e}, response: {response[:200]}")
            # 파싱 실패 시 원본 텍스트에서 직접 추출 시도
            original_text = user_prompt if user_prompt else response
            return self._extract_from_text(original_text)
    
    def _validate_and_fix_intent(self, parsed: dict, original_text: str) -> dict:
        """파싱된 결과 검증 및 보정"""
        current_year = datetime.now().year
        current_month = datetime.now().month
        
        # 부서 목록
        valid_departments = ["개발1팀", "개발2팀", "인사팀", "재무팀", "영업팀", "마케팅팀", "기획팀", "디자인팀"]
        
        # 1. 연도 보정
        year = parsed.get("year")
        if year:
            if isinstance(year, str):
                # "2025년", "2025", "25년" 등 처리
                year_match = re.search(r'(\d{2,4})', year)
                if year_match:
                    year_num = int(year_match.group(1))
                    if year_num < 100:
                        year = 2000 + year_num
                    else:
                        year = year_num
                else:
                    year = current_year
            elif not isinstance(year, int) or year < 2000 or year > 2100:
                year = current_year
        else:
            # 원본 텍스트에서 연도 추출 시도
            year_match = re.search(r'(\d{4})년|(\d{2})년|(\d{4})', original_text)
            if year_match:
                year_str = year_match.group(1) or year_match.group(2) or year_match.group(3)
                year_num = int(year_str)
                if year_num < 100:
                    year = 2000 + year_num
                else:
                    year = year_num
            else:
                year = current_year
        
        # 2. 월 보정
        month = parsed.get("month")
        if month:
            if isinstance(month, str):
                # "1월", "01월", "1" 등 처리
                month_match = re.search(r'(\d{1,2})', month)
                if month_match:
                    month = int(month_match.group(1))
                else:
                    # 한글 월 변환
                    month_map = {"일": 1, "이": 2, "삼": 3, "사": 4, "오": 5, "육": 6,
                                "칠": 7, "팔": 8, "구": 9, "십": 10, "십일": 11, "십이": 12}
                    for key, val in month_map.items():
                        if key in month:
                            month = val
                            break
                    else:
                        month = current_month
            elif not isinstance(month, int) or month < 1 or month > 12:
                month = current_month
        else:
            # 원본 텍스트에서 월 추출 시도
            month_match = re.search(r'(\d{1,2})월|(\d{1,2})', original_text)
            if month_match:
                month = int(month_match.group(1) or month_match.group(2))
            else:
                month = current_month
        
        # 3. 부서명 보정
        department = parsed.get("department")
        if department:
            # 공백 제거 및 정규화
            department = department.replace(" ", "").replace("  ", "")
            # 부서 목록과 매칭
            for valid_dept in valid_departments:
                if valid_dept in department or department in valid_dept:
                    department = valid_dept
                    break
            else:
                # 매칭 실패 시 원본 텍스트에서 찾기
                for valid_dept in valid_departments:
                    if valid_dept.replace("팀", "") in original_text or valid_dept in original_text:
                        department = valid_dept
                        break
                else:
                    department = None
        else:
            # 원본 텍스트에서 부서 찾기
            for valid_dept in valid_departments:
                if valid_dept in original_text or valid_dept.replace("팀", "") in original_text:
                    department = valid_dept
                    break
        
        # 4. 직원명 보정 (원본 텍스트에서 직접 추출 우선 - 가장 정확함)
        employee_name = None
        
        print(f"[Name Extract] 원본 텍스트: '{original_text}'")
        
        # 패턴 1: 부서명 다음에 오는 한글 이름 (2-4글자) - 가장 일반적인 패턴
        # 예: "영업팀 정도윤 25년 1월" → "정도윤" 추출
        for dept in valid_departments:
            if dept not in original_text:
                continue
                
            # 부서명의 위치 찾기
            dept_idx = original_text.find(dept)
            if dept_idx == -1:
                continue
            
            # 부서명 다음 부분 추출
            after_dept = original_text[dept_idx + len(dept):].strip()
            print(f"[Name Extract] 부서 '{dept}' 다음 텍스트: '{after_dept}'")
            
            # 부서명 다음에 오는 한글 이름 추출 (2-4글자)
            # "영업팀소속 정도윤" 같은 경우도 처리
            # 패턴: (공백/없음) + (소속/직원 등의 단어 + 공백)? + 한글 이름 + 공백/숫자/키워드
            name_patterns = [
                r'^\s+([가-힣]{2,4})(?=\s+\d|\s+년|\s+출결|\s+엑셀|\s+알려|\s+보여|$)',  # 공백 + 이름 + 공백/숫자
                r'^([가-힣]{2,4})(?=\s+\d|\s+년|\s+출결|\s+엑셀|\s+알려|\s+보여|$)',  # 이름 + 공백/숫자 (공백 없이)
                r'^\s+([가-힣]{2,4})\s+',  # 공백 + 이름 + 공백
                r'^([가-힣]{2,4})\s+',  # 이름 + 공백 (공백 없이 시작)
                # "소속", "직원" 등의 단어 다음에 오는 이름
                r'(?:소속|직원|사원|멤버)\s+([가-힣]{2,4})(?=\s+\d|\s+년|\s+출결|\s+엑셀|\s+알려|\s+보여|$)',
                r'(?:소속|직원|사원|멤버)([가-힣]{2,4})(?=\s+\d|\s+년|\s+출결|\s+엑셀|\s+알려|\s+보여|$)',
            ]
            
            for pattern in name_patterns:
                match = re.search(pattern, after_dept)
                if match:
                    candidate_name = match.group(1)
                    print(f"[Name Extract] 패턴 매칭: '{pattern}' → '{candidate_name}'")
                    # 부서명의 일부가 아닌지 확인
                    is_dept_part = any(candidate_name in d or d.replace("팀", "") in candidate_name for d in valid_departments)
                    excluded_words = ["년", "월", "출결", "엑셀", "현황", "통계", "알려", "보여", "뽑아", "다운로드", "알려주세요", "보여주세요", "소속", "직원", "사원", "멤버"]
                    if not is_dept_part and candidate_name not in excluded_words:
                        employee_name = candidate_name
                        print(f"[Name Extract] 패턴1 성공: '{employee_name}'")
                        break
            if employee_name:
                break
        
        # 패턴 2: 부서명 다음에 오는 이름 (연도 없이, "소속" 같은 단어 포함)
        if not employee_name:
            for dept in valid_departments:
                if dept not in original_text:
                    continue
                    
                dept_idx = original_text.find(dept)
                after_dept = original_text[dept_idx + len(dept):].strip()
                
                # "소속", "직원" 등의 단어를 건너뛰고 이름 찾기
                patterns = [
                    r'(?:소속|직원|사원|멤버)\s+([가-힣]{2,4})(?=\s+출결|\s+엑셀|\s+알려|\s+보여|$)',
                    r'(?:소속|직원|사원|멤버)([가-힣]{2,4})(?=\s+출결|\s+엑셀|\s+알려|\s+보여|$)',
                    r'^\s+([가-힣]{2,4})(?=\s+출결|\s+엑셀|\s+알려|\s+보여|$)',
                    r'^([가-힣]{2,4})(?=\s+출결|\s+엑셀|\s+알려|\s+보여|$)',
                ]
                
                for pattern in patterns:
                    match = re.search(pattern, after_dept)
                    if match:
                        candidate_name = match.group(1)
                        is_dept_part = any(candidate_name in d or d.replace("팀", "") in candidate_name for d in valid_departments)
                        excluded_words = ["년", "월", "출결", "엑셀", "현황", "통계", "알려", "보여", "뽑아", "다운로드", "알려주세요", "보여주세요", "소속", "직원", "사원", "멤버"]
                        if not is_dept_part and candidate_name not in excluded_words:
                            employee_name = candidate_name
                            print(f"[Name Extract] 패턴2 성공: '{employee_name}'")
                            break
                if employee_name:
                    break
        
        # 패턴 3: 부서명 없이 이름만 있는 경우 (이름 다음에 연도/월이 오는 패턴)
        if not employee_name:
            name_pattern = r'([가-힣]{2,4})\s+\d{1,2}년'
            match = re.search(name_pattern, original_text)
            if match:
                candidate_name = match.group(1)
                # 부서명이 아니고, 키워드가 아닌 경우
                if candidate_name not in valid_departments:
                    excluded_words = ["년", "월", "출결", "엑셀", "현황", "통계", "알려", "보여", "뽑아", "다운로드", "알려주세요"]
                    is_dept_part = any(candidate_name in d or d.replace("팀", "") in candidate_name for d in valid_departments)
                    if not is_dept_part and candidate_name not in excluded_words:
                        employee_name = candidate_name
                        print(f"[Name Extract] 패턴3 성공: '{employee_name}'")
        
        # 원본에서 직접 추출 실패 시 파싱된 결과 사용 (하지만 엄격한 검증)
        if not employee_name:
            parsed_name = parsed.get("employeeName")
            if parsed_name and parsed_name != "null" and parsed_name != "":
                # 파싱된 이름이 원본 텍스트에 정확히 있는지 확인 (부분 문자열이 아닌 정확한 매칭)
                if parsed_name in original_text:
                    # 하지만 원본에서 직접 추출을 다시 시도
                    # 부서명 다음에 오는 이름 패턴으로 재확인
                    found_in_original = False
                    for dept in valid_departments:
                        if dept in original_text:
                            # 부서명 다음 부분에서 이름 찾기
                            dept_idx = original_text.find(dept)
                            after_dept = original_text[dept_idx + len(dept):]
                            # 공백 제거 후 이름 부분 추출
                            name_match = re.search(r'\s+([가-힣]{2,4})', after_dept)
                            if name_match:
                                extracted_name = name_match.group(1)
                                if extracted_name == parsed_name:
                                    employee_name = parsed_name
                                    found_in_original = True
                                    print(f"[Name Extract] 파싱된 결과 검증 성공: '{employee_name}'")
                                    break
                    
                    if not found_in_original:
                        print(f"[Name Extract] 경고: 파싱된 이름 '{parsed_name}'이 원본에서 직접 추출되지 않음. 무시함.")
                else:
                    print(f"[Name Extract] 경고: 파싱된 이름 '{parsed_name}'이 원본 텍스트에 없음. 무시함.")
        
        # 5. Intent 보정
        intent = parsed.get("intent", "UNKNOWN")
        if intent == "UNKNOWN":
            # Intent 재판단
            excel_keywords = ["엑셀", "다운로드", "뽑아", "뽑아줘", "추출", "엑셀로"]
            summary_keywords = ["현황", "어때", "통계", "보여줘", "알려줘", "알려주세요", "보여주세요", "알려"]
            
            has_excel = any(keyword in original_text for keyword in excel_keywords)
            has_summary = any(keyword in original_text for keyword in summary_keywords)
            
            if employee_name:
                # 직원명이 있으면 EMPLOYEE intent
                if has_excel:
                    intent = "ATTENDANCE_EXCEL_EMPLOYEE"
                elif has_summary or not has_excel:
                    intent = "ATTENDANCE_SUMMARY_EMPLOYEE"
            elif department:
                # 부서만 있으면 DEPARTMENT intent
                if has_excel:
                    intent = "ATTENDANCE_EXCEL_DEPARTMENT"
                elif has_summary or not has_excel:
                    intent = "ATTENDANCE_SUMMARY_DEPARTMENT"
        
        result = {
            "intent": intent,
            "department": department,
            "employeeName": employee_name,
            "year": year,
            "month": month
        }
        
        print(f"[Intent Result] {result}")
        return result
    
    def _extract_from_text(self, text: str) -> dict:
        """JSON 파싱 실패 시 원본 텍스트에서 직접 추출"""
        current_year = datetime.now().year
        current_month = datetime.now().month
        valid_departments = ["개발1팀", "개발2팀", "인사팀", "재무팀", "영업팀", "마케팅팀", "기획팀", "디자인팀"]
        
        # 연도 추출
        year_match = re.search(r'(\d{4})년|(\d{2})년|(\d{4})', text)
        year = current_year
        if year_match:
            year_str = year_match.group(1) or year_match.group(2) or year_match.group(3)
            year_num = int(year_str)
            if year_num < 100:
                year = 2000 + year_num
            else:
                year = year_num
        
        # 월 추출
        month_match = re.search(r'(\d{1,2})월|(\d{1,2})', text)
        month = current_month
        if month_match:
            month = int(month_match.group(1) or month_match.group(2))
        
        # 부서 추출
        department = None
        for dept in valid_departments:
            if dept in text or dept.replace("팀", "") in text:
                department = dept
                break
        
        # Intent 판단
        intent = "UNKNOWN"
        if department:
            if "엑셀" in text or "뽑" in text or "다운로드" in text:
                intent = "ATTENDANCE_EXCEL_DEPARTMENT"
            else:
                intent = "ATTENDANCE_SUMMARY_DEPARTMENT"
        
        return {
            "intent": intent,
            "department": department,
            "employeeName": None,
            "year": year,
            "month": month
        }
    
    def _default_intent(self) -> dict:
        return {
            "intent": "UNKNOWN",
            "department": None,
            "employeeName": None,
            "year": datetime.now().year,
            "month": datetime.now().month
        }