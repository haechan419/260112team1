package com.Team1_Back.security.listener;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 로그인 성공 이벤트
 */
@Getter
@AllArgsConstructor
public class LoginSuccessEvent {

    private final String employeeNo;
    private final String ipAddress;
}