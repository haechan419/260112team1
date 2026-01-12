package com.Team1_Back.repository;

import com.Team1_Back.domain.Attendance;
import com.Team1_Back.domain.AttendanceStatus;
import com.Team1_Back.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@SpringBootTest
public class AttendanceDataGeneratorTest {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * ⚠️ 특정 사용자의 오늘 날짜 출결 데이터 삭제 (트랜잭션 문제 해결용)
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void deleteTodayAttendanceForUser() {
        String employeeNo = "EMP00002"; // 문제가 있는 사용자
        User user = userRepository.findByEmployeeNo(employeeNo).orElse(null);

        if (user != null) {
            LocalDate today = LocalDate.now();
            attendanceRepository.findByUserIdAndAttendanceDate(user.getId(), today)
                    .ifPresent(attendance -> {
                        attendanceRepository.delete(attendance);
                        attendanceRepository.flush();
                        System.out.println("✅ " + employeeNo + " 사용자의 오늘 출결 데이터 삭제 완료");
                    });
        } else {
            System.out.println("⚠️  사용자를 찾을 수 없습니다: " + employeeNo);
        }
    }

    /**
     * 🔍 사용자 및 부서명 확인 (디버깅용)
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void checkUsersAndDepartments() {
        List<User> allUsers = userRepository.findAll();
        List<User> activeUsers = allUsers.stream()
                .filter(User::getIsActive)
                .toList();
        List<User> usersWithDept = activeUsers.stream()
                .filter(u -> u.getDepartmentName() != null && !u.getDepartmentName().isEmpty())
                .toList();

        System.out.println("=== 사용자 및 부서명 확인 ===");
        System.out.println("전체 사용자: " + allUsers.size() + "명");
        System.out.println("재직 중 사용자: " + activeUsers.size() + "명");
        System.out.println("부서명 있는 사용자: " + usersWithDept.size() + "명");
        System.out.println();

        System.out.println("부서명 있는 사용자 목록:");
        usersWithDept.forEach(u -> System.out
                .println("  - " + u.getEmployeeNo() + " / " + u.getName() + " / " + u.getDepartmentName()));

        System.out.println();
        System.out.println("부서명 없는 사용자 목록:");
        activeUsers.stream()
                .filter(u -> u.getDepartmentName() == null || u.getDepartmentName().isEmpty())
                .forEach(u -> System.out.println("  - " + u.getEmployeeNo() + " / " + u.getName() + " / 부서명: " +
                        (u.getDepartmentName() == null ? "NULL" : "빈 문자열")));
    }

    /**
     * ✅ 2025년 7월 ~ 12월 데이터 생성 (7월~12월 전체, 부서명 있는 사용자만)
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void generateJulyToDecember2025Data() {
        Random random = new Random();

        List<User> users = userRepository.findAll().stream()
                .filter(User::getIsActive)
                .filter(u -> u.getDepartmentName() != null && !u.getDepartmentName().isEmpty()) // 부서명이 있는 사용자만
                .toList();

        if (users.isEmpty()) {
            System.out.println("⚠️  부서명이 있는 사용자가 없습니다!");
            System.out.println("사용자 관리 페이지에서 사용자들의 부서명을 설정해주세요.");
            return;
        }

        // 7월부터 12월까지 각 월별로 데이터 생성
        int[] months = { 7, 8, 9, 10, 11, 12 };
        int totalSaved = 0;

        for (int month : months) {
            LocalDate startDate = LocalDate.of(2025, month, 1);
            LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth()); // 해당 월의 마지막 날

            System.out.println("\n=== 2025년 " + month + "월 출결 데이터 생성 (" + users.size() + "명) ===");
            System.out.println("기간: " + startDate + " ~ " + endDate);

            // 기존 데이터 삭제 (해당 기간)
            List<Attendance> existingData = attendanceRepository.findByDateRange(startDate, endDate);
            if (!existingData.isEmpty()) {
                System.out.println("⚠️  기존 데이터 발견: " + existingData.size() + "건 - 삭제 중...");
                attendanceRepository.deleteAll(existingData);
                attendanceRepository.flush();
                System.out.println("✅ 기존 데이터 삭제 완료");
            }

            List<Attendance> toSave = new ArrayList<>();

            for (User user : users) {
                LocalDate date = startDate;
                while (!date.isAfter(endDate)) {
                    if (date.getDayOfWeek() != DayOfWeek.SATURDAY &&
                            date.getDayOfWeek() != DayOfWeek.SUNDAY) {
                        Attendance attendance = createRandomAttendance(user, date, random);
                        toSave.add(attendance);
                    }
                    date = date.plusDays(1);
                }
            }

            // 한번에 저장
            if (!toSave.isEmpty()) {
                attendanceRepository.saveAll(toSave);
                attendanceRepository.flush(); // 즉시 DB에 반영
                System.out.println("✅ " + month + "월 생성 완료: " + toSave.size() + "건");
                totalSaved += toSave.size();

                // 부서별 통계 출력
                toSave.stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                a -> a.getUser().getDepartmentName(),
                                java.util.stream.Collectors.counting()))
                        .forEach((dept, count) -> System.out.println("  - " + dept + ": " + count + "건"));
            } else {
                System.out.println("⚠️  " + month + "월 생성할 데이터가 없습니다.");
            }
        }

        System.out.println("\n===========================================");
        System.out.println("✅ 전체 생성 완료: 총 " + totalSaved + "건");
        System.out.println("===========================================");

        // 최종 확인
        LocalDate finalStartDate = LocalDate.of(2025, 7, 1);
        LocalDate finalEndDate = LocalDate.of(2025, 12, 31);
        long finalSavedCount = attendanceRepository.findByDateRange(finalStartDate, finalEndDate).size();
        System.out.println("📊 실제 DB에 저장된 데이터 (7월~12월): " + finalSavedCount + "건");

        // 부서별로 실제 조회되는지 확인
        Map<String, Long> deptCounts = attendanceRepository.findByDateRange(finalStartDate, finalEndDate).stream()
                .filter(a -> a.getUser() != null && a.getUser().getDepartmentName() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        a -> a.getUser().getDepartmentName(),
                        java.util.stream.Collectors.counting()));
        System.out.println("📊 부서별 실제 조회 가능한 데이터:");
        deptCounts.forEach((dept, count) -> System.out.println("  - " + dept + ": " + count + "건"));
    }

    /**
     * ✅ 2025년 1월 데이터 생성 (1월 전체, 부서명 있는 사용자만)
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void generateJanuary2025Data() {
        Random random = new Random();

        List<User> users = userRepository.findAll().stream()
                .filter(User::getIsActive)
                .filter(u -> u.getDepartmentName() != null && !u.getDepartmentName().isEmpty()) // 부서명이 있는 사용자만
                .toList();

        if (users.isEmpty()) {
            System.out.println("⚠️  부서명이 있는 사용자가 없습니다!");
            System.out.println("사용자 관리 페이지에서 사용자들의 부서명을 설정해주세요.");
            return;
        }

        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 31); // ✅ 1월 전체

        System.out.println("=== 2025년 1월 출결 데이터 생성 (" + users.size() + "명) ===");
        System.out.println("기간: " + startDate + " ~ " + endDate);

        // 기존 데이터 삭제 (해당 기간)
        List<Attendance> existingData = attendanceRepository.findByDateRange(startDate, endDate);
        if (!existingData.isEmpty()) {
            System.out.println("⚠️  기존 데이터 발견: " + existingData.size() + "건 - 삭제 중...");
            attendanceRepository.deleteAll(existingData);
            System.out.println("✅ 기존 데이터 삭제 완료");
        }

        List<Attendance> toSave = new ArrayList<>();

        for (User user : users) {
            LocalDate date = startDate;
            while (!date.isAfter(endDate)) {
                if (date.getDayOfWeek() != DayOfWeek.SATURDAY &&
                        date.getDayOfWeek() != DayOfWeek.SUNDAY) {
                    Attendance attendance = createRandomAttendance(user, date, random);
                    toSave.add(attendance);
                }
                date = date.plusDays(1);
            }
        }

        // 한번에 저장
        if (!toSave.isEmpty()) {
            attendanceRepository.saveAll(toSave);
            attendanceRepository.flush(); // 즉시 DB에 반영
            System.out.println("✅ 생성 완료: " + toSave.size() + "건");

            // 부서별 통계 출력
            toSave.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            a -> a.getUser().getDepartmentName(),
                            java.util.stream.Collectors.counting()))
                    .forEach((dept, count) -> System.out.println("  - " + dept + ": " + count + "건"));

            // 데이터 확인
            attendanceRepository.flush(); // 한번 더 flush
            long savedCount = attendanceRepository.findByDateRange(startDate, endDate).size();
            System.out.println("📊 실제 DB에 저장된 데이터: " + savedCount + "건");

            // 부서별로 실제 조회되는지 확인
            Map<String, Long> deptCounts = attendanceRepository.findByDateRange(startDate, endDate).stream()
                    .filter(a -> a.getUser() != null && a.getUser().getDepartmentName() != null)
                    .collect(java.util.stream.Collectors.groupingBy(
                            a -> a.getUser().getDepartmentName(),
                            java.util.stream.Collectors.counting()));
            System.out.println("📊 부서별 실제 조회 가능한 데이터:");
            deptCounts.forEach((dept, count) -> System.out.println("  - " + dept + ": " + count + "건"));
        } else {
            System.out.println("⚠️  생성할 데이터가 없습니다. (부서명이 있는 사용자가 없습니다)");
        }
    }

    /**
     * 전체 기간 데이터 생성 (2024.10 ~ 2025.1)
     */
    @Test
    public void generateAttendanceDataForAllUsers() {
        Random random = new Random();

        List<User> users = userRepository.findAll().stream()
                .filter(User::getIsActive)
                .toList();

        System.out.println("=== 총 " + users.size() + "명의 유저에 대한 출결 데이터 생성 시작 ===");

        LocalDate startDate = LocalDate.of(2024, 10, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 31);

        List<Attendance> toSave = new ArrayList<>();

        for (User user : users) {
            System.out.println("Processing: " + user.getName() + " (" + user.getDepartmentName() + ")");

            LocalDate date = startDate;
            while (!date.isAfter(endDate)) {
                if (date.getDayOfWeek() != DayOfWeek.SATURDAY &&
                        date.getDayOfWeek() != DayOfWeek.SUNDAY) {

                    if (!date.isAfter(LocalDate.now())) {
                        if (attendanceRepository.findByUserIdAndAttendanceDate(user.getId(), date).isEmpty()) {
                            Attendance attendance = createRandomAttendance(user, date, random);
                            toSave.add(attendance);
                        }
                    }
                }
                date = date.plusDays(1);
            }
        }

        // 한번에 저장
        attendanceRepository.saveAll(toSave);

        System.out.println("===========================================");
        System.out.println("✅ 생성 완료: " + toSave.size() + "건");
        System.out.println("===========================================");
    }

    /**
     * 랜덤 출결 데이터 생성
     */
    private Attendance createRandomAttendance(User user, LocalDate date, Random random) {
        int rand = random.nextInt(100);

        if (rand < 75) {
            // 정상 출근 - 75%
            int minute = random.nextInt(60);
            LocalTime checkInTime = LocalTime.of(8, minute);
            LocalTime checkOutTime = LocalTime.of(18, random.nextInt(30));

            return Attendance.builder()
                    .user(user)
                    .attendanceDate(date)
                    .checkInTime(LocalDateTime.of(date, checkInTime))
                    .checkOutTime(LocalDateTime.of(date, checkOutTime))
                    .status(AttendanceStatus.PRESENT)
                    .build();

        } else if (rand < 87) {
            // 지각 - 12%
            int hour = 9 + random.nextInt(2);
            int minute = (hour == 9) ? 5 + random.nextInt(55) : random.nextInt(31);
            LocalTime checkInTime = LocalTime.of(hour, minute);
            LocalTime checkOutTime = LocalTime.of(18, random.nextInt(60));

            return Attendance.builder()
                    .user(user)
                    .attendanceDate(date)
                    .checkInTime(LocalDateTime.of(date, checkInTime))
                    .checkOutTime(LocalDateTime.of(date, checkOutTime))
                    .status(AttendanceStatus.LATE)
                    .build();

        } else if (rand < 95) {
            // 휴가 - 8%
            return Attendance.builder()
                    .user(user)
                    .attendanceDate(date)
                    .status(AttendanceStatus.LEAVE)
                    .build();

        } else {
            // 결근 - 5%
            return Attendance.builder()
                    .user(user)
                    .attendanceDate(date)
                    .status(AttendanceStatus.ABSENT)
                    .build();
        }
    }
}