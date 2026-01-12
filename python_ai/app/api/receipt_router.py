from fastapi import APIRouter, UploadFile, File
from pydantic import BaseModel
from typing import Optional, Dict, Any
from app.services.receipt_service import ReceiptService
from app.services.approval_recommendation_service import ApprovalRecommendationService
from app.utils.image_util import resize_image
import asyncio
import logging

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/ai/receipt")
service = ReceiptService()
approval_service = ApprovalRecommendationService()

# 승인 추천 요청 DTO
class ApprovalRecommendationRequest(BaseModel):
    expense_data: Dict[str, Any]
    receipt_extraction: Optional[Dict[str, Any]] = None

@router.post("/extract")
async def extract_receipt(file: UploadFile = File(...)):
    """영수증 이미지 업로드 및 분석 API"""
    logger.info(f"[Python API] 영수증 OCR 요청 받음: filename={file.filename}, size={file.size} bytes")
    
    try:
        contents = await file.read()
        logger.info(f"[Python API] 파일 읽기 완료: {len(contents)} bytes")
        
        # 이미지 크기 최적화 (API 비용 절감)
        optimized_img = resize_image(contents)
        logger.info(f"[Python API] 이미지 최적화 완료: {len(optimized_img)} bytes")
        
        # 동기 함수를 비동기 컨텍스트에서 실행
        logger.info("[Python API] OpenAI Vision API 호출 시작...")
        result = await asyncio.to_thread(service.analyze, optimized_img)
        
        if result.get("error"):
            logger.error(f"[Python API] OCR 처리 중 오류: {result.get('error')}")
        else:
            logger.info(f"[Python API] OCR 결과: merchant={result.get('extractedMerchant')}, "
                       f"amount={result.get('extractedAmount')}, date={result.get('extractedDate')}, "
                       f"category={result.get('extractedCategory')}")
        
        return result
    except Exception as e:
        logger.error(f"[Python API] OCR 처리 중 예외 발생: {str(e)}", exc_info=True)
        return {"error": f"이미지 처리 중 오류 발생: {str(e)}"}


@router.post("/recommend-approval")
async def recommend_approval(request: ApprovalRecommendationRequest):
    """
    지출 내역 승인/반려 추천 API

    지출 내역과 영수증 정보를 분석하여 AI가 승인/반려를 추천합니다.
    """
    logger.info("[Python API] 승인 추천 요청 받음")

    try:
        # 동기 함수를 비동기 컨텍스트에서 실행
        result = await asyncio.to_thread(
            approval_service.analyze_expense,
            request.expense_data,
            request.receipt_extraction
        )

        if result.get("error"):
            logger.error(f"[Python API] 승인 추천 처리 중 오류: {result.get('error')}")
        else:
            logger.info(
                f"[Python API] 승인 추천 완료: "
                f"recommendation={result.get('recommendation')}, "
                f"confidence={result.get('confidence', 0):.2f}"
            )

        return result
    except Exception as e:
        logger.error(f"[Python API] 승인 추천 중 예외 발생: {str(e)}", exc_info=True)
        return {
            "error": f"처리 중 오류 발생: {str(e)}",
            "recommendation": "REQUEST_MORE_INFO",
            "confidence": 0.0,
            "reason": "AI 분석 중 오류가 발생했습니다.",
            "riskFactors": [],
            "positiveFactors": []
        }