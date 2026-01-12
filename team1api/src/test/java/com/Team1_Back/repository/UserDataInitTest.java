package com.Team1_Back.repository;



import com.Team1_Back.domain.Role;
import com.Team1_Back.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@SpringBootTest
public class UserDataInitTest {

    @Autowired

    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @Transactional
    @Commit  // 테스트 후 롤백하지 않고 실제 DB에 저장
    public void insertTestUsers() {

        // 부서 목록
        List<String> departments = Arrays.asList(
                "개발1팀", "개발2팀", "인사팀", "재무팀", "영업팀", "마케팅팀", "기획팀", "디자인팀"
        );

        // 직급 목록
        List<String> positions = Arrays.asList(
                "사원", "주임", "대리", "과장", "차장", "부장"
        );

        // 성 목록
        List<String> lastNames = Arrays.asList(
                "김", "이", "박", "최", "정", "강", "조", "윤", "장", "임",
                "한", "오", "서", "신", "권", "황", "안", "송", "류", "홍"
        );

        // 이름 목록
        List<String> firstNames = Arrays.asList(
                "민준", "서준", "도윤", "예준", "시우", "하준", "주원", "지호", "지훈", "준서",
                "서연", "서윤", "지우", "서현", "민서", "하은", "하윤", "윤서", "지민", "채원",
                "수빈", "지원", "유진", "은서", "다은", "지영", "수현", "예진", "혜진", "소연"
        );

        Random random = new Random();
        String encodedPassword = passwordEncoder.encode("1234");

        int createdCount = 0;
        int skippedCount = 0;

        for (int i = 1; i <= 100; i++) {

            // 사번 생성
            String employeeNo = String.format("EMP%05d", i);

            // 사번 중복 체크
            if (userRepository.existsByEmployeeNo(employeeNo)) {
                System.out.println("⚠️ 이미 존재하는 사번 (건너뜀): " + employeeNo);
                skippedCount++;
                continue;
            }

            // 랜덤 데이터 생성
            String lastName = lastNames.get(random.nextInt(lastNames.size()));
            String firstName = firstNames.get(random.nextInt(firstNames.size()));
            String name = lastName + firstName;

            String department = departments.get(random.nextInt(departments.size()));
            String position = positions.get(random.nextInt(positions.size()));

            // 생년월일 (1970~2000년 사이)
            int year = 1970 + random.nextInt(31);
            int month = 1 + random.nextInt(12);
            int day = 1 + random.nextInt(28);
            LocalDate birthDate = LocalDate.of(year, month, day);

            // 연락처
            String phone = String.format("010-%04d-%04d",
                    1000 + random.nextInt(9000),
                    1000 + random.nextInt(9000));

            // 이메일
            String email = String.format("user%03d@company.com", i);

            // Role (5명은 ADMIN, 나머지는 USER)
            Role role = (i <= 5) ? Role.ADMIN : Role.USER;

            // isActive (90%는 재직중, 10%는 퇴사)
            boolean isActive = random.nextInt(100) < 90;

            User user = User.builder()
                    .employeeNo(employeeNo)
                    .password(encodedPassword)
                    .name(name)
                    .email(email)
                    .birthDate(birthDate)
                    .phone("콜")
                    .address("서울시 강남구 테헤란로 " + (100 + i) + "길")
                    .addressDetail((100 + i) + "호")
                    .departmentName(department)
                    .position(position)
                    .role(role)
                    .isActive(isActive)
                    .build();

            userRepository.save(user);
            System.out.println("✅ 생성됨: " + employeeNo + " - " + name + " (" + department + ", " + position + ")");
            createdCount++;
        }

        System.out.println("\n========================================");
        System.out.println("📊 결과: 생성 " + createdCount + "명, 건너뜀 " + skippedCount + "명");
        System.out.println("========================================");
    }
}