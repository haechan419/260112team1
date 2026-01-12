// src/pages/member/MyPage.js

import React from "react";
// 👇 1. 우리가 만든 컴포넌트 불러오기
import FaceRegister from "../../components/member/FaceRegister";

const MyPage = () => {
  return (
    <div className="p-4">
      <h1 className="text-2xl font-bold">마이페이지</h1>

      {/* ... 기존 내 정보 보여주는 코드들 ... */}

      <hr className="my-6" />

      {/* 👇 2. 여기에 얼굴 등록 컴포넌트 배치! */}
      <div className="bg-gray-100 p-6 rounded-lg shadow-md">
        <FaceRegister />
      </div>
    </div>
  );
};

export default MyPage;
