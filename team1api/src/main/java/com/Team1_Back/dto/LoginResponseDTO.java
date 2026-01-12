package com.Team1_Back.dto;


import com.Team1_Back.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder

// 로그인 응답 성공시
// 아래와 같은 정보들을 반환합니다

public class LoginResponseDTO {

    // 사용자 id (프론트에서 식별용으로 사용)
    private Long id;
    // 사용자 사번 (로그인 아이디)
    private String employeeNo;
    // 사용자 이름
    private String name;
    // 사용자 이메일
    private String email;
    // 사용자 부서명
    private String departmentName;
    // 권한 정보
    private Role role;
}
