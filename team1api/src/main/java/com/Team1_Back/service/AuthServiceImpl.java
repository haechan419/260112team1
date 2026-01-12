package com.Team1_Back.service;

import com.Team1_Back.domain.User;
import com.Team1_Back.dto.LoginResponseDTO;
import com.Team1_Back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;

    // 계정 잠금 해제
    @Override
    public void unlockAccount(String employeeNo) {
        User user = userRepository.findByEmployeeNo(employeeNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사번입니다."));

        user.unlock();
        userRepository.save(user);

        log.info("계정 잠금 해제: {}", employeeNo);
    }

    // 사용자 정보 조회
    @Override
    public LoginResponseDTO getUserInfo(String employeeNo) {
        User user = userRepository.findByEmployeeNo(employeeNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사번입니다."));

        return LoginResponseDTO.builder()
                .id(user.getId())
                .employeeNo(user.getEmployeeNo())
                .name(user.getName())
                .email(user.getEmail())
                .departmentName(user.getDepartmentName())
                .role(user.getRole())
                .build();
    }
}
