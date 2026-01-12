import base64
import os
import logging
from dotenv import load_dotenv
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate  
from langchain_core.output_parsers import PydanticOutputParser
from langchain_core.messages import HumanMessage
from pydantic import BaseModel, Field
from typing import Literal
from app.core.config import settings

load_dotenv()

logger = logging.getLogger(__name__)

# Pydantic 모델 정의 (최신 langchain 버전 호환)
class ReceiptExtraction(BaseModel):
    """영수증에서 추출한 정보"""
    extractedMerchant: str = Field(description="가맹점 이름 (상호명, 가맹점명, 매장명, 지점명을 모두 포함하여 자세하게 추출. 예: '7-ELEVEN 강남제일점', '스타벅스 강남점' 등)")
    extractedAmount: int = Field(description="금액 (숫자만, 예: 15000)")
    extractedDate: str = Field(description="결제일 (YYYY-MM-DD 형식, 예: 2024-01-15)")
    extractedCategory: Literal["식비", "교통비", "비품", "기타"] = Field(description="카테고리 [식비, 교통비, 비품, 기타] 중 하나")
    extractedDescription: str = Field(description="구매한 상품명/물건 목록 (영수증에 표시된 모든 구매 품목을 쉼표로 구분하여 나열. 예: '삼각김밥, 콜라, 라면, 과자' 등)")
    confidence: float = Field(description="OCR 인식 신뢰도 (0.0 ~ 1.0)", ge=0.0, le=1.0)
    extractedJson: str = Field(description="영수증에서 추출한 모든 텍스트 (상세 주소, 제품명, 품목 등 모든 정보 포함)")

class ReceiptService:
    """영수증 이미지 분석 서비스 - OpenAI Vision API 사용"""
    
    def __init__(self):
        # temperature=0: 일관된 결과를 위해 설정
        self.chat = ChatOpenAI(model=settings.OPENAI_MODEL, temperature=0)
        
        # PydanticOutputParser 초기화
        self.output_parser = PydanticOutputParser(pydantic_object=ReceiptExtraction)

    def analyze(self, image_bytes):
        """영수증 이미지 분석 및 구조화된 데이터 추출"""
        logger.info(f"[ReceiptService] analyze 시작: image_size={len(image_bytes)} bytes")
        try:
            # Vision API용 base64 인코딩
            image_base64 = base64.b64encode(image_bytes).decode('utf-8')
            logger.info(f"[ReceiptService] base64 인코딩 완료: base64_length={len(image_base64)}")
            
            # JSON 출력 형식 지시사항 생성
            format_instructions = self.output_parser.get_format_instructions()
            logger.info("[ReceiptService] 프롬프트 준비 완료")
            receipt_template = """이 영수증 이미지를 매우 정밀하게 분석하여 다음 정보를 추출하여 JSON 형식으로 출력하세요.
            [추출해야 할 정보]
            1. 가맹점 이름 (extractedMerchant): 영수증 상단의 상호명, 가맹점명, 매장명, 지점명을 모두 포함하여 자세하게 추출하세요.
               - 예시: "7-ELEVEN"만 있는 경우 → "7-ELEVEN"
               - 예시: "7-ELEVEN 강남제일점"이 있는 경우 → "7-ELEVEN 강남제일점"
               - 예시: "스타벅스 강남점"이 있는 경우 → "스타벅스 강남점"
               - 예시: "CU 서초구청점"이 있는 경우 → "CU 서초구청점"
               - 지점명, 매장명, 상세 위치 정보가 있으면 반드시 포함하세요.
               - 영수증에 표시된 가맹점 관련 모든 정보를 빠짐없이 포함하세요.
            2. 금액 (extractedAmount): 총 결제 금액을 숫자만 추출하세요 (예: "15,000원" → 15000, "₩20,000" → 20000)
            3. 결제일 (extractedDate): 날짜를 YYYY-MM-DD 형식으로 변환하세요 (예: "2024.01.15" → "2024-01-15", "2024/1/5" → "2024-01-05")
            4. 카테고리 (extractedCategory): 다음 중 하나로 분류하세요 [식비, 교통비, 비품, 기타]
               - 식비: 음식점, 카페, 배달 등
               - 교통비: 택시, 버스, 지하철, 주유소 등
               - 비품: 문구점, 편의점, 마트 등
               - 기타: 위에 해당하지 않는 경우
            5. 구매 상품명 (extractedDescription): 영수증에 표시된 구매한 모든 상품명/물건을 쉼표로 구분하여 나열하세요.
               - 영수증 하단의 품목 리스트에서 모든 상품명을 정확히 읽어서 추출하세요.
               - 예시: "삼각김밥, 콜라, 라면, 과자"
               - 예시: "아메리카노, 크로와상, 샌드위치"
               - 예시: "볼펜, 노트, 스테이플러"
               - 상품명이 여러 개인 경우 쉼표와 공백으로 구분하세요 (예: "상품1, 상품2, 상품3")
               - 상품명이 없거나 불명확하면 "알 수 없음"으로 표시하세요.
               - 수량, 단가, 합계 등 숫자 정보는 제외하고 상품명만 추출하세요.
            6. 신뢰도 (confidence): 이미지 인식 신뢰도 (0.0 ~ 1.0)
            7. 전체 텍스트 (extractedJson): 영수증에 있는 모든 텍스트를 빠짐없이, 정확하게 추출하세요
               - 상세 주소: 도로명 주소, 지번 주소를 정확히 읽어서 포함 (시/도, 시/군/구, 동/읍/면, 번지, 상세주소 모두)
               - 제품명/품목명: 구매한 모든 상품의 이름을 정확히 읽어서 포함 (작은 글씨도 놓치지 마세요)
               - 수량, 단가, 합계 등 모든 숫자 정보
               - 전화번호, 사업자번호, 영업시간 등 기타 정보
               - 영수증의 모든 텍스트를 원문 그대로, 줄바꿈과 공백도 유지하여 포함

            [OCR 정확도 향상 지시사항]
            - 이미지를 확대하여 자세히 살펴보고, 작은 글씨도 정확히 읽으세요.
            - 상세 주소는 특히 주의 깊게 읽어서 오타 없이 정확히 추출하세요.
            - 제품명은 영수증 하단의 품목 리스트를 모두 읽어서 포함하세요.
            - 모호한 글자는 주변 맥락을 고려하여 추론하세요.
            - 숫자와 한글, 영문이 섞인 부분도 정확히 구분하여 읽으세요.
            - 텍스트를 요약하거나 생략하지 말고, 보이는 모든 내용을 그대로 포함하세요.

            [기본 규칙]
            - 금액은 콤마와 통화 기호를 제거하고 숫자만 추출하세요.
            - 날짜가 없거나 불명확하면 "알 수 없음"으로 표시하세요.
            - 가맹점명이 없으면 "알 수 없음"으로 표시하세요.
            - 구매 상품명이 없거나 불명확하면 "알 수 없음"으로 표시하세요.
            - 금액이 없거나 불명확하면 0으로 표시하세요.
            - 카테고리는 반드시 [식비, 교통비, 비품, 기타] 중 하나만 사용하세요.

            [출력 형식]
            {format_instructions}"""


            # 프롬프트 템플릿 생성 및 변수 치환
            prompt_template = ChatPromptTemplate.from_template(receipt_template)
            text_prompt = prompt_template.format_messages(
                format_instructions=format_instructions
            )[0].content
            
            # Vision API 메시지 생성 (텍스트 + 이미지)
            message = HumanMessage(
                content=[
                    {"type": "text", "text": text_prompt},
                    {
                        "type": "image_url",
                        "image_url": {
                            "url": f"data:image/jpeg;base64,{image_base64}"
                        }
                    }
                ]
            )
            
            try:
                # OpenAI API 호출 및 응답 파싱
                logger.info("[ReceiptService] OpenAI Vision API 호출 중...")
                response = self.chat.invoke([message])
                logger.info(f"[ReceiptService] OpenAI 응답 받음: response_length={len(response.content) if response.content else 0}")
                
                parsed_result = self.output_parser.parse(response.content)
                logger.info(f"[ReceiptService] 응답 파싱 완료: merchant={parsed_result.extractedMerchant}, amount={parsed_result.extractedAmount}, description={parsed_result.extractedDescription}")
                
                # Pydantic 모델을 딕셔너리로 변환
                result = parsed_result.model_dump()
                result['modelName'] = settings.OPENAI_MODEL
                
                logger.info(f"[ReceiptService] 최종 결과: {result}")
                return result
            except Exception as llm_error:
                logger.error(f"[ReceiptService] OpenAI 분석 중 오류: {str(llm_error)}", exc_info=True)
                return {
                    "error": f"OpenAI 분석 중 오류 발생: {str(llm_error)}"
                }
        except Exception as e:
            logger.error(f"[ReceiptService] 이미지 처리 중 오류: {str(e)}", exc_info=True)
            return {"error": f"이미지 처리 중 오류 발생: {str(e)}"}