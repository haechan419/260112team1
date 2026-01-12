"""
===========================================
SmartSpend AI Server (FastAPI)
===========================================
실행: python main.py
URL: http://localhost:8000
API 문서: http://localhost:8000/docs
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from pydantic import BaseModel
from typing import Optional
import os

from app.services.ollama_service import OllamaService
from app.services.attendance_service import AttendanceService
from app.services.performance_service import PerformanceService
# ✅ 추가: 영수증 OCR 라우터 import (영수증 OCR 통합)
from app.api.receipt_router import router as receipt_router

# FastAPI 앱 생성
app = FastAPI(
    title="SmartSpend AI Server",
    description="출결 관리 AI 챗봇 서버",
    version="1.0.0"
)

# CORS 설정 (React 3000번 포트 허용)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 서비스 인스턴스
ollama_service = OllamaService()
attendance_service = AttendanceService(ollama_service)
performance_service = PerformanceService(ollama_service)

# ✅ 추가: 영수증 OCR 라우터 등록 (영수증 OCR 통합)
# /api/ai/receipt/extract 엔드포인트가 추가됨
app.include_router(receipt_router)

# DTO 정의
class AttendanceAiRequest(BaseModel):
    prompt: str  # 사용자 입력

class AttendanceAiResponse(BaseModel):
    ok: bool
    message: str
    summary: Optional[str] = None
    hasFile: bool = False
    downloadUrl: Optional[str] = None
    fileName: Optional[str] = None

# 부서 실적 DTO
class PerformanceAiRequest(BaseModel):
    prompt: str  # 사용자 입력

class PerformanceAiResponse(BaseModel):
    ok: bool
    message: str
    summary: Optional[str] = None
    chartImage: Optional[str] = None  # Base64 이미지

# API 엔드포인트
@app.get("/")
def health_check():
    return {"status": "ok", "message": "SmartSpend AI Server is running!"}

@app.post("/api/ai/attendance", response_model=AttendanceAiResponse)
def process_attendance_query(request: AttendanceAiRequest):
    """출결 AI 메인 엔드포인트"""
    try:
        result = attendance_service.process_query(request.prompt)
        return AttendanceAiResponse(**result)
    except Exception as e:
        return AttendanceAiResponse(
            ok=False,
            message=f"오류: {str(e)}",
            hasFile=False
        )

@app.get("/api/ai/attendance/download/{filename}")
def download_file(filename: str):
    """엑셀 파일 다운로드"""
    file_path = f"generated/{filename}"
    if not os.path.exists(file_path):
        raise HTTPException(status_code=404, detail="파일 없음")
    return FileResponse(path=file_path, filename=filename)

# ========== 부서 실적 API ==========
@app.post("/api/ai/performance", response_model=PerformanceAiResponse)
def process_performance_query(request: PerformanceAiRequest):
    """부서 실적 비교 AI 엔드포인트"""
    try:
        result = performance_service.process_query(request.prompt)
        return PerformanceAiResponse(**result)
    except Exception as e:
        print(f"[Performance API Error] {e}")
        return PerformanceAiResponse(
            ok=False,
            message=f"오류: {str(e)}",
            summary=None,
            chartImage=None
        )

# 서버 실행
if __name__ == "__main__":
    import uvicorn
    print("🚀 SmartSpend AI Server: http://localhost:8000")
    uvicorn.run(app, host="0.0.0.0", port=8000)