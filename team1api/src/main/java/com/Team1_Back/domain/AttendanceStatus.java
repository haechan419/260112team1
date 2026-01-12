package com.Team1_Back.domain;

public enum AttendanceStatus {
    PRESENT,    // 출근 (09:00 이전)
    LATE,       // 지각 (09:00 이후)
    ABSENT,     // 결근
    LEAVE       // 휴가/병가
}