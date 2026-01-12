import React, { useEffect, useRef, useState } from "react";
import * as faceapi from "face-api.js";
import jwtAxios, { API_SERVER_HOST } from "../../util/jwtUtil";

// props로 onSuccess 받기
const FaceRegister = ({ onSuccess }) => {
  const videoRef = useRef();

  // 상태 관리
  const [isCameraOpen, setIsCameraOpen] = useState(false); // 카메라 열림 여부
  const [isModelLoaded, setIsModelLoaded] = useState(false); // 모델 로딩 여부
  const [status, setStatus] = useState("");
  const [stream, setStream] = useState(null); // 스트림 저장 (끌 때 필요)

  // 등록 시작 버튼 클릭 시 실행
  const startRegistration = async () => {
    setIsCameraOpen(true);
    setStatus("AI 모델 로딩 중...");

    try {
      const MODEL_URL = "/models";

      // 모델 로딩 (이미 로딩됐으면 스킵)
      if (!isModelLoaded) {
        await Promise.all([
          faceapi.loadSsdMobilenetv1Model(MODEL_URL),
          faceapi.loadFaceLandmarkModel(MODEL_URL),
          faceapi.loadFaceRecognitionModel(MODEL_URL),
        ]);
        setIsModelLoaded(true);
      }

      setStatus("카메라 권한을 허용해주세요.");
      startVideo();
    } catch (err) {
      console.error("모델 로드 실패:", err);
      setStatus("모델 로딩 실패 (새로고침 필요)");
    }
  };

  // 카메라 켜기
  const startVideo = () => {
    navigator.mediaDevices
      .getUserMedia({ video: true })
      .then((currentStream) => {
        setStream(currentStream);
        if (videoRef.current) {
          videoRef.current.srcObject = currentStream;
        }
        setStatus("준비 완료! 정면을 응시하고 저장하세요.");
      })
      .catch((err) => {
        console.error("카메라 에러:", err);
        setStatus("카메라를 켤 수 없습니다.");
      });
  };

  // 카메라 끄기 (창 닫기)
  const stopVideo = () => {
    if (stream) {
      stream.getTracks().forEach((track) => track.stop()); // 실제 카메라 불 끄기
      setStream(null);
    }
    setIsCameraOpen(false); // UI 닫기
    setStatus("");
  };

  // 얼굴 캡처 및 전송
  const handleCapture = async () => {
    if (!videoRef.current) return;
    setStatus("얼굴 분석 중...");

    try {
      const detections = await faceapi
        .detectSingleFace(videoRef.current)
        .withFaceLandmarks()
        .withFaceDescriptor();

      if (detections) {
        const descriptorArray = Array.from(detections.descriptor);

        // 서버 전송
        await jwtAxios.put(`${API_SERVER_HOST}/api/face/register`, {
          descriptor: JSON.stringify(descriptorArray),
        });

        alert("✅ 얼굴 등록이 완료되었습니다!");

        stopVideo(); // 카메라 끄기

        // 부모 컴포넌트(MypagePage)에게 성공 신호 보내기
        if (onSuccess) {
          onSuccess();
        }
      } else {
        alert("❌ 얼굴을 인식하지 못했습니다. 정면을 봐주세요.");
        setStatus("재시도 대기 중...");
      }
    } catch (err) {
      console.error("등록 에러:", err);
      alert(
        "저장 실패: " +
          (err.response?.status === 404 ? "주소 오류" : "서버 오류")
      );
    }
  };

  // --- 렌더링 ---
  return (
    <div style={{ textAlign: "center" }}>
      {/* 카메라가 꺼져있을 때 (대기 화면) */}
      {!isCameraOpen && (
        <div
          style={{
            padding: "30px",
            background: "#fff",
            border: "1px dashed #ccc",
            borderRadius: "10px",
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            gap: "10px",
          }}
        >
          {/* 렌즈 아이콘 느낌의 UI */}
          <div
            style={{
              width: "60px",
              height: "60px",
              background: "#f0f2f5",
              borderRadius: "50%",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              fontSize: "30px",
              marginBottom: "10px",
              border: "2px solid #ddd",
            }}
          >
            👁️
          </div>
          <p style={{ margin: 0, color: "#666", fontSize: "14px" }}>
            로그인 시 사용할 얼굴을 등록합니다.
          </p>
          <button
            onClick={startRegistration}
            style={{
              marginTop: "10px",
              padding: "10px 20px",
              background: "#4A90E2",
              color: "white",
              border: "none",
              borderRadius: "6px",
              cursor: "pointer",
              fontWeight: "bold",
            }}
          >
            Face ID 등록/수정 시작
          </button>
        </div>
      )}

      {/* 카메라가 켜졌을 때 (등록 화면) */}
      {isCameraOpen && (
        <div
          style={{
            padding: "20px",
            background: "#f9f9f9",
            borderRadius: "10px",
            border: "1px solid #ddd",
            animation: "fadeIn 0.3s ease-in-out",
          }}
        >
          <h3 style={{ fontSize: "16px", marginBottom: "10px" }}>
            📸 얼굴 스캔 중
          </h3>
          <p style={{ fontSize: "12px", color: "#888", marginBottom: "10px" }}>
            {status}
          </p>

          <div
            style={{
              position: "relative",
              display: "inline-block",
              marginBottom: "15px",
            }}
          >
            <video
              ref={videoRef}
              autoPlay
              muted
              width="320"
              height="240"
              style={{
                borderRadius: "12px",
                backgroundColor: "#000",
                transform: "scaleX(-1)", // 거울모드 (좌우반전)
              }}
            />
            {/* 렌즈 가이드라인 */}
            <div
              style={{
                position: "absolute",
                top: "50%",
                left: "50%",
                transform: "translate(-50%, -50%)",
                width: "180px",
                height: "220px",
                border: "2px dashed rgba(255,255,255,0.5)",
                borderRadius: "50%",
                pointerEvents: "none",
              }}
            ></div>
          </div>

          <div
            style={{ display: "flex", gap: "10px", justifyContent: "center" }}
          >
            <button
              onClick={stopVideo}
              style={{
                padding: "8px 15px",
                background: "#95a5a6",
                color: "white",
                border: "none",
                borderRadius: "5px",
                cursor: "pointer",
              }}
            >
              취소
            </button>
            <button
              onClick={handleCapture}
              style={{
                padding: "8px 20px",
                background: "#27ae60",
                color: "white",
                border: "none",
                borderRadius: "5px",
                cursor: "pointer",
                fontWeight: "bold",
              }}
            >
              얼굴 저장하기
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default FaceRegister;
