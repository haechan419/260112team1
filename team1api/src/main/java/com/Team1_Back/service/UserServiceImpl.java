package com.Team1_Back.service;

import com.Team1_Back.domain.User;
import com.Team1_Back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 사용자 서비스 구현체
 * 
 * <p>사용자 조회 및 권한 확인 기능을 구현합니다.
 * 
 * @author Team1
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmployeeNo(String employeeNo) {
        return userRepository.findByEmployeeNo(employeeNo);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. ID: " + userId));
        return user.isAdmin();
    }
}

