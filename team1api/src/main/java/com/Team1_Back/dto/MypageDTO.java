package com.Team1_Back.dto;

import com.Team1_Back.domain.User;
import lombok.*;

import java.time.format.DateTimeFormatter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MypageDTO {

    private String name;
    private String birthDate;
    private String phone;
    private String email;
    private String employeeNo;
    private String address;
    private String addressDetail;
    private String departmentName;
    private String position;
    private String hireDate;

    public static MypageDTO fromEntity(User user) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        return MypageDTO.builder()
                .name(user.getName())
                .birthDate(user.getBirthDate() != null ? user.getBirthDate().format(formatter) : null)
                .phone(user.getPhone())
                .email(user.getEmail())
                .employeeNo(user.getEmployeeNo())
                .address(user.getAddress())
                .addressDetail(user.getAddressDetail())
                .departmentName(user.getDepartmentName())
                .position(user.getPosition())
                .hireDate(user.getCreatedUserAt() != null ? user.getCreatedUserAt().format(formatter) : null)
                .build();
    }
}