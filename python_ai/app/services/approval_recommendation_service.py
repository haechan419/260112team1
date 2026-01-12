"""
지출 내역 승인/반려 판단 AI 서비스

지출 내역과 영수증 정보를 종합 분석하여 승인/반려를 판단합니다.
"""
import logging
from pathlib import Path
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import PydanticOutputParser
from pydantic import BaseModel, Field
from typing import Literal, List
from app.core.config import settings

logger = logging.getLogger(__name__)


class ApprovalRecommendation(BaseModel):
    """AI 승인/반려 추천 결과 모델"""
    
    recommendation: Literal["APPROVE", "REJECT_CLEAR", "REJECT_SUSPECTED"] = Field(
        description="추천 결과: APPROVE(승인), REJECT_CLEAR(완전 반려 - 명백한 위반), REJECT_SUSPECTED(반려 의심 - 추가 검토 필요)"
    )
    confidence: float = Field(
        description="추천 신뢰도 (0.0 ~ 1.0)", 
        ge=0.0, 
        le=1.0
    )
    reason: str = Field(
        description="추천 사유 (상세한 분석 근거를 한국어로 작성)"
    )
    riskFactors: List[str] = Field(
        description="위험 요소 목록 (예: 금액 불일치, 날짜 불일치 등)"
    )
    positiveFactors: List[str] = Field(
        description="긍정 요소 목록 (예: 정보 일치, 영수증 신뢰도 높음 등)"
    )


class ApprovalRecommendationService:
    """
    지출 내역 승인/반려 판단 서비스
    
    지출 내역과 영수증 OCR 결과를 종합 분석하여 승인/반려를 판단합니다.
    """
    
    def __init__(self):
        self.chat = ChatOpenAI(model=settings.OPENAI_MODEL, temperature=0.3)
        self.output_parser = PydanticOutputParser(pydantic_object=ApprovalRecommendation)
        
        # 규정 문서 경로 설정
        self.policy_path = Path(__file__).parent.parent.parent / "docs" / "policy.txt"
    
    def _load_policy(self) -> str:
        """
        규정 문서를 파일에서 읽어옵니다.
        
        Note: 현재는 파일 기반으로 구현했습니다.
        향후 규정 문서가 많아지면 벡터 DB 기반 검색으로 확장 가능합니다.
        """
        if not self.policy_path.exists():
            logger.warning(f"[ApprovalRecommendationService] 규정 문서를 찾을 수 없습니다: {self.policy_path}")
            return ""
        
        try:
            with open(self.policy_path, 'r', encoding='utf-8') as f:
                policy_text = f.read()
            logger.info(f"[ApprovalRecommendationService] 규정 문서 로드 완료: {len(policy_text)} bytes")
            return policy_text
        except Exception as e:
            logger.error(f"[ApprovalRecommendationService] 규정 문서 로드 실패: {str(e)}")
            return ""
    
    def analyze_expense(self, expense_data: dict, receipt_extraction: dict = None):
        """
        지출 내역과 영수증 정보를 종합 분석하여 승인/반려 추천
        
        Args:
            expense_data: 지출 내역 정보
                - receiptDate: 지출 일자 (YYYY-MM-DD)
                - merchant: 가맹점명
                - amount: 금액 (숫자)
                - category: 카테고리 (식비, 교통비, 비품, 기타)
                - description: 상세내용
            receipt_extraction: 영수증 OCR 추출 결과 (선택)
                - extractedDate: 영수증 날짜
                - extractedMerchant: 영수증 가맹점명
                - extractedAmount: 영수증 금액
                - extractedCategory: 영수증 카테고리
                - extractedDescription: 영수증 상세내용
                - confidence: OCR 신뢰도
        
        Returns:
            dict: AI 판단 결과
                - recommendation: APPROVE/REJECT
                - confidence: 신뢰도 (0.0 ~ 1.0)
                - reason: 추천 사유
                - riskFactors: 위험 요소 목록
                - positiveFactors: 긍정 요소 목록
        """
        logger.info("[ApprovalRecommendationService] 지출 내역 분석 시작")
        
        try:
            format_instructions = self.output_parser.get_format_instructions()
            
            # 규정 문서 로드
            policy_context = self._load_policy()
            
            # 지출 내역 정보 포맷팅
            expense_info = f"""
[지출 내역 정보]
- 지출 일자: {expense_data.get('receiptDate', 'N/A')}
- 가맹점명: {expense_data.get('merchant', 'N/A')}
- 금액: {expense_data.get('amount', 0):,}원
- 카테고리: {expense_data.get('category', 'N/A')}
- 상세내용: {expense_data.get('description', 'N/A')}
"""
            
            # 영수증 정보 포맷팅
            if receipt_extraction:
                receipt_info = f"""
[영수증 OCR 추출 결과]
- 영수증 날짜: {receipt_extraction.get('extractedDate', 'N/A')}
- 영수증 가맹점명: {receipt_extraction.get('extractedMerchant', 'N/A')}
- 영수증 금액: {receipt_extraction.get('extractedAmount', 0):,}원
- 영수증 카테고리: {receipt_extraction.get('extractedCategory', 'N/A')}
- 영수증 상세내용: {receipt_extraction.get('extractedDescription', 'N/A')}
- OCR 신뢰도: {receipt_extraction.get('confidence', 0) * 100:.1f}%
"""
            else:
                receipt_info = """
[영수증 OCR 추출 결과]
- 영수증 정보 없음 (영수증이 업로드되지 않았거나 OCR 처리가 완료되지 않았습니다)
"""
            
            # 분석 프롬프트 생성
            analysis_prompt = f"""당신은 회사의 지출 결재를 검토하는 전문 AI 에이전트입니다.
다음 지출 내역과 영수증 정보를 종합적으로 분석하여 승인/반려를 판단하세요.

{expense_info}

{receipt_info}

[회사 지출 규정]
{policy_context if policy_context else "규정 문서를 불러올 수 없습니다. 아래 기본 판단 기준을 따릅니다."}

[판단 기준]

1. 정보 일치성 검증
   - 지출 금액과 영수증 금액이 일치하는지 확인 (정확히 일치해야 함)
   - 지출 일자와 영수증 날짜가 일치하는지 확인 (같은 날이어야 함)
   - 가맹점명 일치 여부 (유사도 확인, 약간의 차이는 허용)
   - 카테고리 적절성 (영수증 내용과 카테고리가 일치하는지)

2. 비정상 패턴 감지
   - 과도하게 큰 금액 (비정상적으로 큰 지출)
   - 반복적인 동일 지출 (사기 의심)
   - 개인 용도로 보이는 지출 (예: 편의점 과자, 개인용품 등)
   - 영수증 없이 대금액 지출

3. 영수증 신뢰도
   - OCR 인식 신뢰도가 낮으면 (70% 미만) 낮은 신뢰도와 함께 REJECT_SUSPECTED 추천
   - 영수증 정보와 지출 내역 불일치 시 REJECT_CLEAR 추천
   - 영수증이 없는 경우:
     * 금액이 작고(예: 5만원 이하) 카테고리가 적절하면(교통비, 간단한 식비 등) APPROVE 가능 (신뢰도 0.6~0.7)
     * 금액이 크거나(예: 10만원 이상) 의심스러운 카테고리면 REJECT_SUSPECTED 추천 (신뢰도 0.4~0.6)
     * 영수증이 없어도 정상적인 업무 지출일 수 있음을 고려

4. 회사 정책 준수
   - 카테고리와 실제 지출 내용의 일치성
   - 업무 관련성 확인 (업무와 무관한 개인 지출은 반려)

[판단 결과 가이드]

- APPROVE: 모든 정보가 일치하고 정상적인 업무 지출인 경우
  * 금액, 날짜, 가맹점명이 모두 일치
  * 영수증 신뢰도가 높고 (70% 이상)
  * 업무 관련성이 명확함
  * 비정상 패턴이 없음
  * 영수증이 없어도 금액이 작고(5만원 이하) 카테고리가 적절하면(교통비, 간단한 식비 등) APPROVE 가능
  * 신뢰도: 0.7 이상 권장 (영수증 없으면 0.6~0.7)

- REJECT_CLEAR (완전 반려): 명백한 규정 위반, 정보 불일치, 사기 의심이 확실한 경우
  * 금액이나 날짜가 명백히 불일치
  * 개인 용도로 보이는 지출이 명확함
  * 영수증이 없는데 대금액 지출
  * 명백한 규정 위반
  * 신뢰도: 0.7 이상 권장

- REJECT_SUSPECTED (반려 의심): 정보가 불확실하거나 추가 검토가 필요한 경우
  * 영수증이 없는데 금액이 크거나(10만원 이상) 의심스러운 카테고리
  * OCR 신뢰도가 낮음 (70% 미만)
  * 정보 일부가 불일치하지만 명백한 위반은 아님
  * 추가 확인이 필요한 경우
  * 영수증과 지출 내역이 약간 다르지만 확실하지 않음
  * 신뢰도: 0.3~0.6 권장

[중요]
- REJECT_CLEAR는 확실한 반려 사유가 있을 때만 사용하세요
- REJECT_SUSPECTED는 불확실하거나 추가 검토가 필요할 때 사용하세요
- 관리자는 REJECT_SUSPECTED를 보고 수동 검토할 수 있습니다
- **절대로 REQUEST_MORE_INFO를 사용하지 마세요. 반드시 APPROVE, REJECT_CLEAR, REJECT_SUSPECTED 중 하나만 사용하세요.**

[출력 형식]
{format_instructions}

상세한 분석 근거와 위험 요소, 긍정 요소를 모두 포함하여 판단하세요.
한국어로 명확하고 전문적인 문장으로 작성하세요.
**반드시 recommendation 필드에는 APPROVE, REJECT_CLEAR, REJECT_SUSPECTED 중 하나만 사용하세요. 다른 값은 사용하지 마세요.**"""

            # OpenAI API 호출
            logger.info("[ApprovalRecommendationService] OpenAI API 호출 중...")
            response = self.chat.invoke([{"role": "user", "content": analysis_prompt}])
            
            logger.info(f"[ApprovalRecommendationService] AI 응답 원문 (일부): {response.content[:300]}")
            
            # REQUEST_MORE_INFO를 REJECT_SUSPECTED로 미리 변환 (파싱 전에)
            response_content = response.content
            if '"recommendation": "REQUEST_MORE_INFO"' in response_content or "'recommendation': 'REQUEST_MORE_INFO'" in response_content:
                logger.warning("[ApprovalRecommendationService] REQUEST_MORE_INFO 감지, REJECT_SUSPECTED로 변환")
                response_content = response_content.replace('"recommendation": "REQUEST_MORE_INFO"', '"recommendation": "REJECT_SUSPECTED"')
                response_content = response_content.replace("'recommendation': 'REQUEST_MORE_INFO'", "'recommendation': 'REJECT_SUSPECTED'")
            
            # 응답 파싱
            try:
                parsed_result = self.output_parser.parse(response_content)
                
                # 추가 안전장치: 파싱 후에도 REQUEST_MORE_INFO가 있으면 변환
                if hasattr(parsed_result, 'recommendation') and parsed_result.recommendation == "REQUEST_MORE_INFO":
                    logger.warning("[ApprovalRecommendationService] 파싱 후 REQUEST_MORE_INFO 감지, REJECT_SUSPECTED로 변환")
                    parsed_result.recommendation = "REJECT_SUSPECTED"
                    if parsed_result.confidence > 0.6:
                        parsed_result.confidence = 0.5  # 신뢰도 조정
                
                logger.info(
                    f"[ApprovalRecommendationService] 분석 완료: "
                    f"recommendation={parsed_result.recommendation}, "
                    f"confidence={parsed_result.confidence:.2f}"
                )
                
                # Pydantic 모델을 딕셔너리로 변환
                result = parsed_result.model_dump()
                result['modelName'] = settings.OPENAI_MODEL
                
                return result
            except Exception as parse_error:
                logger.error(f"[ApprovalRecommendationService] 파싱 오류: {str(parse_error)}")
                logger.error(f"[ApprovalRecommendationService] 응답 내용: {response.content}")
                # 파싱 실패 시 REJECT_SUSPECTED 반환
                return {
                    "error": f"응답 파싱 중 오류 발생: {str(parse_error)}",
                    "recommendation": "REJECT_SUSPECTED",
                    "confidence": 0.0,
                    "reason": "AI 분석 결과를 파싱하는 중 오류가 발생했습니다. 수동으로 검토해주세요.",
                    "riskFactors": ["AI 응답 파싱 실패"],
                    "positiveFactors": [],
                    "modelName": settings.OPENAI_MODEL
                }
            
        except Exception as e:
            logger.error(
                f"[ApprovalRecommendationService] 분석 중 오류: {str(e)}", 
                exc_info=True
            )
            return {
                "error": f"분석 중 오류 발생: {str(e)}",
                "recommendation": "REJECT_SUSPECTED",
                "confidence": 0.0,
                "reason": "AI 분석 중 오류가 발생했습니다. 수동으로 검토해주세요.",
                "riskFactors": ["AI 분석 실패"],
                "positiveFactors": [],
                "modelName": settings.OPENAI_MODEL
            }
