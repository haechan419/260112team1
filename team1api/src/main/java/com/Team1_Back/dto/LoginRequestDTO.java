package com.Team1_Back.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {

    // 로그인 요청 DTO 입니다
    // 로그인을 할때 데이터를 보냅니다.

    private String employeeNo;  // 사번
    private String password;    // 비밀번호
}