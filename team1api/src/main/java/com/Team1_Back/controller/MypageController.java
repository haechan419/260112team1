package com.Team1_Back.controller;

import com.Team1_Back.dto.AttendanceDTO;
import com.Team1_Back.dto.MypageDTO;
import com.Team1_Back.dto.UserDTO;
import com.Team1_Back.service.MypageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MypageController {

    private final MypageService mypageService;

    @GetMapping
    public ResponseEntity<MypageDTO> getMyInfo(@AuthenticationPrincipal UserDTO userDTO) {
        return ResponseEntity.ok(mypageService.getMyInfo(userDTO.getId()));
    }

    @GetMapping("/attendance")
    public ResponseEntity<List<AttendanceDTO>> getMonthlyAttendance(
            @AuthenticationPrincipal UserDTO userDTO,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(mypageService.getMonthlyAttendance(userDTO.getId(), year, month));
    }

    @PostMapping("/attendance/check-in")
    public ResponseEntity<AttendanceDTO> checkIn(@AuthenticationPrincipal UserDTO userDTO) {
        return ResponseEntity.ok(mypageService.checkIn(userDTO.getId()));
    }

    @PostMapping("/attendance/check-out")
    public ResponseEntity<AttendanceDTO> checkOut(@AuthenticationPrincipal UserDTO userDTO) {
        return ResponseEntity.ok(mypageService.checkOut(userDTO.getId()));
    }
}