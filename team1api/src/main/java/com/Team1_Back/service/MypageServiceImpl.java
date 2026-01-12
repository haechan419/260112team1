package com.Team1_Back.service;

import com.Team1_Back.domain.Attendance;
import com.Team1_Back.domain.AttendanceStatus;
import com.Team1_Back.domain.User;
import com.Team1_Back.dto.AttendanceDTO;
import com.Team1_Back.dto.MypageDTO;
import com.Team1_Back.repository.AttendanceRepository;
import com.Team1_Back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MypageServiceImpl implements MypageService {

    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;

    private static final LocalTime WORK_START_TIME = LocalTime.of(9, 0);

    @Override
    public MypageDTO getMyInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        return MypageDTO.fromEntity(user);
    }

    @Override
    public List<AttendanceDTO> getMonthlyAttendance(Long userId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        return attendanceRepository.findByUserIdAndAttendanceDateBetween(userId, start, end)
                .stream()
                .map(AttendanceDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AttendanceDTO checkIn(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        if (attendanceRepository.findByUserIdAndAttendanceDate(userId, today).isPresent()) {
            throw new RuntimeException("이미 출근 처리되었습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        AttendanceStatus status = now.toLocalTime().isBefore(WORK_START_TIME)
                ? AttendanceStatus.PRESENT
                : AttendanceStatus.LATE;

        Attendance attendance = Attendance.builder()
                .user(user)
                .attendanceDate(today)
                .checkInTime(now)
                .status(status)
                .build();

        return AttendanceDTO.fromEntity(attendanceRepository.save(attendance));
    }

    @Override
    @Transactional
    public AttendanceDTO checkOut(Long userId) {
        LocalDate today = LocalDate.now();

        Attendance attendance = attendanceRepository.findByUserIdAndAttendanceDate(userId, today)
                .orElseThrow(() -> new RuntimeException("출근 기록이 없습니다."));

        if (attendance.getCheckOutTime() != null) {
            throw new RuntimeException("이미 퇴근 처리되었습니다.");
        }

        attendance.setCheckOutTime(LocalDateTime.now());

        return AttendanceDTO.fromEntity(attendanceRepository.save(attendance));
    }
}