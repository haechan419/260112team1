package com.Team1_Back.security;

import java.util.List;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.Team1_Back.domain.User;
import com.Team1_Back.dto.UserDTO;
import com.Team1_Back.repository.UserProfileImageRepository;
import com.Team1_Back.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserProfileImageRepository profileImageRepository;

    @Override
    public UserDetails loadUserByUsername(String employeeNo) throws UsernameNotFoundException {

        log.info("----------------loadUserByUsername-----------------------------");
        log.info("사번: {}", employeeNo);

        User user = userRepository.findByEmployeeNo(employeeNo)
                .orElse(null);

        if (user == null) {
            throw new UsernameNotFoundException("존재하지 않는 사번입니다: " + employeeNo);
        }

        // 프로필 이미지 URL 조회
        String profileImageUrl = profileImageRepository.findFileNameByUserId(user.getId())
                .map(fileName -> "/api/view/user_image/s_" + fileName)  // 썸네일 URL
                .orElse(null);

        UserDTO userDTO = new UserDTO(
                user.getId(),
                user.getEmployeeNo(),
                user.getPassword(),
                user.getName(),
                user.getEmail(),
                user.getDepartmentName(),
                user.getIsActive(),
                user.isLocked(),
                user.getFailedLoginCount(),
                List.of(user.getRole().name()),
                profileImageUrl  // 프로필 이미지 URL 전달
        );

        log.info("UserDTO: {}", userDTO);

        return userDTO;
    }
}