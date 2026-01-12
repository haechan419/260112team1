package com.Team1_Back.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateDTO {

    private Long id;
    private String name;
    private String email;
    private LocalDate birthDate;
    private String phone;
    private String address;
    private String addressDetail;
    private String departmentName;
    private String position;
    private String role;

    private String newPassword;
}
