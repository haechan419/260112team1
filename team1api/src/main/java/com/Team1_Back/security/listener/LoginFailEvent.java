package com.Team1_Back.security.listener;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * 로그인 실패 이벤트
 */
@Getter
@RequiredArgsConstructor
public class LoginFailEvent {

    private final String employeeNo;
    private final String ipAddress;

    @Setter
    private String errorMessage = "사번 또는 비밀번호가 올바르지 않습니다.";
}