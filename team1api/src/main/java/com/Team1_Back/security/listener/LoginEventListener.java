package com.Team1_Back.security.listener;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.Team1_Back.domain.LoginAttempt;
import com.Team1_Back.domain.User;
import com.Team1_Back.repository.LoginAttemptRepository;
import com.Team1_Back.repository.UserRepository;
import com.Team1_Back.service.MypageService; // ✅ 추가

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 로그인 이벤트 리스너
 *
 * Handler에서 발행한 이벤트를 받아서 DB 작업을 처리합니다.
 * 
 * @Component로 등록되어 Repository 주입이 가능합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LoginEventListener {

    private final UserRepository userRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final MypageService mypageService; // ✅ 추가

    private static final int MAX_FAILED_ATTEMPTS = 5;

    /**
     * 로그인 성공 이벤트 처리
     */
    @EventListener
    // @Transactional
    // Transaction silently rolled back because it has been marked as rollback-only
    // 문제 발생
    public void handleLoginSuccess(LoginSuccessEvent event) {

        log.info("로그인 성공 이벤트 처리 - 사번: {}", event.getEmployeeNo());

        User user = userRepository.findByEmployeeNo(event.getEmployeeNo())
                .orElse(null);

        if (user != null) {
            // 실패 횟수 초기화
            user.resetFailedLoginCount();
            userRepository.save(user);

            // ✅ 자동 출석체크 추가 (별도 트랜잭션으로 실행)
            try {
                // 별도 트랜잭션으로 실행하여 메인 트랜잭션에 영향 없도록
                mypageService.checkIn(user.getId());
                log.info("자동 출석체크 완료 - 사번: {}", event.getEmployeeNo());
            } catch (RuntimeException e) {
                // 이미 출석 처리된 경우나 기타 오류는 무시 (로그인은 성공)
                log.warn("자동 출석체크 실패 (무시) - 사번: {}, 오류: {}", event.getEmployeeNo(), e.getMessage());
            } catch (Exception e) {
                // 예상치 못한 오류도 무시 (로그인은 성공)
                log.error("자동 출석체크 중 예외 발생 (무시) - 사번: {}", event.getEmployeeNo(), e);
            }
        }

        // 로그인 성공 기록 저장
        saveLoginAttempt(event.getEmployeeNo(), event.getIpAddress(), true);
    }

    // 로그인 실패 로직
    @EventListener
    @Transactional
    public void handleLoginFailure(LoginFailEvent event) {

        log.info("로그인 실패 이벤트 처리 - 사번: {}", event.getEmployeeNo());

        User user = userRepository.findByEmployeeNo(event.getEmployeeNo())
                .orElse(null);

        if (user == null) {
            // 존재하지 않는 사번
            saveLoginAttempt(event.getEmployeeNo(), event.getIpAddress(), false);
            event.setErrorMessage("사번 또는 비밀번호가 올바르지 않습니다.");
            return;
        }

        // 실패 횟수 증가
        user.increaseFailedLoginCount();

        // 5회 초과 시 계정 잠금
        if (user.getFailedLoginCount() >= MAX_FAILED_ATTEMPTS) {
            user.lock();
            log.warn("계정 잠금 처리: {} ({}회 실패)", event.getEmployeeNo(), MAX_FAILED_ATTEMPTS);
            userRepository.save(user);
            saveLoginAttempt(event.getEmployeeNo(), event.getIpAddress(), false);
            event.setErrorMessage("로그인 5회 실패로 계정이 잠겼습니다. 관리자에게 문의하세요.");
            return;
        }

        userRepository.save(user);
        saveLoginAttempt(event.getEmployeeNo(), event.getIpAddress(), false);

        int remainingAttempts = MAX_FAILED_ATTEMPTS - user.getFailedLoginCount();
        event.setErrorMessage("사번 또는 비밀번호가 올바르지 않습니다. (남은 시도: " + remainingAttempts + "회)");
    }

    /**
     * 로그인 시도 기록 저장
     */
    private void saveLoginAttempt(String employeeNo, String ipAddress, boolean success) {
        LoginAttempt loginAttempt = LoginAttempt.builder()
                .employeeNo(employeeNo)
                .ipAddress(ipAddress)
                .success(success)
                .build();
        loginAttemptRepository.save(loginAttempt);
    }
}