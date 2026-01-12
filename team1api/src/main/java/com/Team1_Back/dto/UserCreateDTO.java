package com.Team1_Back.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateDTO {

    private String employeeNo;
    private String password;
    private String name;
    private String email;
    private LocalDate birthDate;
    private String phone;
    private String address;
    private String addressDetail;
    private String departmentName;
    private String positionName;
    private String role;
}
