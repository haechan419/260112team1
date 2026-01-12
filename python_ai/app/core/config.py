import os
from dotenv import load_dotenv

load_dotenv()

class Settings:
    """애플리케이션 설정 관리"""
    
    OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
    OPENAI_MODEL = os.getenv("OPENAI_MODEL", "gpt-4o-mini")

settings = Settings()