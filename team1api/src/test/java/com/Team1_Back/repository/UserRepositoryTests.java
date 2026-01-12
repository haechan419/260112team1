package com.Team1_Back.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.Team1_Back.domain.Role;
import com.Team1_Back.domain.User;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
@Slf4j
public class UserRepositoryTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    public void testUserInsert() {
        User user = User.builder()
                .employeeNo("11111111")
                .name("관리자")
                .email("test@aaa.com")
                .password(passwordEncoder.encode("1111"))
                .role(Role.ADMIN)
                .departmentName("관리자")
                .isActive(true)
                .failedLoginCount(0)
                .lockedAt(null)
                .deletedAt(null)
                .build();  // createdUserAt, updatedUserAt 제거 (자동 생성됨)

        userRepository.save(user);
        log.info("User saved: " + user.getId());
    }

    // 더미데이터 10개 임의로 만드는 함수
    @Test
    public void insertDummyUsers() {
        for (int i = 1; i <= 10; i++) {
            // 사번이 중복되지 않도록 인덱스 i를 활용 (ex: 20250001, 20250002...)
            String employeeNo = "2025" + String.format("%04d", i);

            // 짝수 인덱스는 ADMIN, 홀수 인덱스는 USER로 권한 분산 (선택 사항)
            Role role = (i % 2 == 0) ? Role.ADMIN : Role.USER;
            String deptName = (role == Role.ADMIN) ? "관리팀" : "영업팀";

            User user = User.builder()
                    .employeeNo(employeeNo)
                    .name("테스터" + i)
                    .email("test" + i + "@example.com")
                    .password(passwordEncoder.encode("1234")) // 비밀번호는 동일하게 설정
                    .role(role)
                    .departmentName(deptName)
                    .isActive(true)
                    .failedLoginCount(0)
                    .lockedAt(null)
                    .deletedAt(null)
                    .build();

            userRepository.save(user);
            log.info("Dummy User 생성 성공 - ID: {}, 사번: {}, 권한: {}", user.getId(), employeeNo, role);
        }
    }
}