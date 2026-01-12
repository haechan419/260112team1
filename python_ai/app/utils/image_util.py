import cv2
import numpy as np

def resize_image(image_bytes, max_width=1024):
    """이미지 크기 조정 - API 비용 및 처리 시간 최적화"""
    try:
        # 바이트 데이터를 OpenCV 이미지로 변환
        nparr = np.frombuffer(image_bytes, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        
        if img is None:
            raise ValueError("이미지 디코딩 실패: 지원하지 않는 이미지 형식이거나 손상된 파일입니다.")
        
        h, w = img.shape[:2]
        
        # 최대 크기 초과 시 비율 유지하며 축소
        if w > max_width:
            ratio = max_width / w
            new_size = (max_width, int(h * ratio))
            img = cv2.resize(img, new_size, interpolation=cv2.INTER_AREA)
        
        # JPEG 형식으로 인코딩하여 반환
        _, buffer = cv2.imencode('.jpg', img)
        return buffer.tobytes()
    except Exception as e:
        raise ValueError(f"이미지 처리 실패: {str(e)}")