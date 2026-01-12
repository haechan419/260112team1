import React, { useEffect, useRef, useState } from "react";
import * as faceapi from "face-api.js";
import axios from "axios";
import { API_SERVER_HOST } from "../../util/jwtUtil";

const FaceLogin = ({ onLoginSuccess, onCancel }) => {
  const videoRef = useRef();

  // 🔒 상태 관리
  const isProcessing = useRef(false);

  // ⏳ [추가] 타임아웃 카운터 (몇 번 검사했는지 센다)
  const attemptCount = useRef(0);
  // 최대 시도 횟수 (0.5초 * 20회 = 10초)
  const MAX_ATTEMPTS = 20;

  const [isModelLoaded, setIsModelLoaded] = useState(false);
  const [status, setStatus] = useState("Face ID 초기화 중...");
  const [faceMatcher, setFaceMatcher] = useState(null);
  const [userList, setUserList] = useState([]);

  // 1. 초기화
  useEffect(() => {
    isProcessing.current = false;
    attemptCount.current = 0; // 카운터 초기화

    const init = async () => {
      try {
        const MODEL_URL = "/models";

        await Promise.all([
          faceapi.loadSsdMobilenetv1Model(MODEL_URL),
          faceapi.loadFaceLandmarkModel(MODEL_URL),
          faceapi.loadFaceRecognitionModel(MODEL_URL),
        ]);

        const res = await axios.get(`${API_SERVER_HOST}/api/face/list`);
        setUserList(res.data);

        if (res.data.length > 0) {
          const labeledDescriptors = res.data.map((user) => {
            const descriptor = new Float32Array(
              JSON.parse(user.faceDescriptor)
            );
            return new faceapi.LabeledFaceDescriptors(user.userId, [
              descriptor,
            ]);
          });
          setFaceMatcher(new faceapi.FaceMatcher(labeledDescriptors, 0.3));
        }

        setIsModelLoaded(true);
        setStatus("카메라를 바라봐 주세요. (10초 제한)");

        const stream = await navigator.mediaDevices.getUserMedia({
          video: true,
        });
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
        }
      } catch (err) {
        console.error("Init Error:", err);
        setStatus("카메라 오류");
      }
    };
    init();

    return () => {
      if (videoRef.current && videoRef.current.srcObject) {
        videoRef.current.srcObject.getTracks().forEach((track) => track.stop());
      }
    };
  }, []);

  // 2. 감지 루프
  useEffect(() => {
    if (!isModelLoaded || !faceMatcher) return;

    const interval = setInterval(async () => {
      if (
        !videoRef.current ||
        videoRef.current.readyState !== 4 ||
        isProcessing.current
      )
        return;

      // ⏳ [추가] 타임아웃 체크 로직
      if (attemptCount.current >= MAX_ATTEMPTS) {
        clearInterval(interval); // 루프 정지
        alert(
          "⏳ 인증 시간이 초과되었습니다.\n일치하는 얼굴을 찾을 수 없습니다.\n아이디 로그인을 이용해주세요."
        );
        if (onCancel) onCancel(); // 강제로 닫기
        return;
      }

      // 시도 횟수 증가
      attemptCount.current += 1;

      isProcessing.current = true;

      try {
        const detection = await faceapi
          .detectSingleFace(videoRef.current)
          .withFaceLandmarks()
          .withFaceDescriptor();

        if (detection) {
          const match = faceMatcher.findBestMatch(detection.descriptor);
          const distance = match.distance;

          // 70% 이상 (오차 0.30 미만) 통과
          if (match.label !== "unknown" && distance < 0.3) {
            clearInterval(interval);

            const userId = match.label;
            const accuracy = ((1 - distance) * 100).toFixed(0);
            const matchedData = userList.find((u) => u.userId === userId);
            const userName =
              matchedData?.name || matchedData?.user?.name || userId;

            setTimeout(() => {
              if (
                window.confirm(
                  `[Face ID] ${userName}님 로그인 하시겠습니까?\n(일치율: ${accuracy}%)`
                )
              ) {
                if (onLoginSuccess) onLoginSuccess(matchedData || { userId });
              } else {
                if (onCancel) onCancel();
              }
            }, 100);
            return;
          }
        }
      } catch (e) {
        // 에러 무시
      } finally {
        isProcessing.current = false;
      }
    }, 500); // 0.5초 간격

    return () => clearInterval(interval);
  }, [isModelLoaded, faceMatcher, userList, onLoginSuccess, onCancel]);

  return (
    <div
      style={{
        textAlign: "center",
        padding: "10px",
        background: "#f9f9f9",
        borderRadius: "10px",
      }}
    >
      <h3 style={{ fontSize: "16px", fontWeight: "bold", marginBottom: "8px" }}>
        Face ID 스캔 중...
      </h3>
      <div style={{ position: "relative", display: "inline-block" }}>
        <video
          ref={videoRef}
          autoPlay
          muted
          width="300"
          height="225"
          style={{
            borderRadius: "12px",
            border: "3px solid #4A90E2",
            backgroundColor: "#000",
          }}
        />
        <div style={{ marginTop: "5px", fontSize: "12px", color: "#666" }}>
          {/* 진행 상황을 살짝 보여주면 좋습니다 */}
          {status}
        </div>

        <button
          onClick={onCancel}
          style={{
            marginTop: "10px",
            padding: "6px 15px",
            backgroundColor: "#95a5a6",
            color: "white",
            border: "none",
            borderRadius: "4px",
            cursor: "pointer",
            fontWeight: "bold",
          }}
        >
          취소
        </button>
      </div>
    </div>
  );
};

export default FaceLogin;
