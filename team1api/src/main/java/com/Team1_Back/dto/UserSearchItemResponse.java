package com.Team1_Back.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserSearchItemResponse {
    private Long userId;
    private String name;
    private String employeeNo;      // 표시용
    private String departmentName;  // 표시용
}

