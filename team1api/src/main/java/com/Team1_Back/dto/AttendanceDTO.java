package com.Team1_Back.dto;

import com.Team1_Back.domain.Attendance;
import lombok.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceDTO {

    private LocalDate date;
    private String status;
    private String checkInTime;
    private String checkOutTime;

    public static AttendanceDTO fromEntity(Attendance a) {
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");

        return AttendanceDTO.builder()
                .date(a.getAttendanceDate())
                .status(a.getStatus().name())
                .checkInTime(a.getCheckInTime() != null ? a.getCheckInTime().format(tf) : null)
                .checkOutTime(a.getCheckOutTime() != null ? a.getCheckOutTime().format(tf) : null)
                .build();
    }
}