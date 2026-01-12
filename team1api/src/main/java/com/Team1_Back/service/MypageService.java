package com.Team1_Back.service;

import com.Team1_Back.dto.AttendanceDTO;
import com.Team1_Back.dto.MypageDTO;

import java.util.List;

public interface MypageService {

    MypageDTO getMyInfo(Long userId);

    List<AttendanceDTO> getMonthlyAttendance(Long userId, int year, int month);

    AttendanceDTO checkIn(Long userId);

    AttendanceDTO checkOut(Long userId);
}