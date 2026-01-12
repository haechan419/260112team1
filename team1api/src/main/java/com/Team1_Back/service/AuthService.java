package com.Team1_Back.service;

import com.Team1_Back.dto.LoginResponseDTO;

// 인증 관련 Service
public interface AuthService {

    // 계정 잠금 해제
    void unlockAccount(String employeeNo);

    // 사용자 정보 조회
    LoginResponseDTO getUserInfo(String employeeNo);
}
