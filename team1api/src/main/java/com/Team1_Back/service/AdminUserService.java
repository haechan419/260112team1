package com.Team1_Back.service;

import com.Team1_Back.dto.*;

public interface AdminUserService {

    // 사원 목록 조회 (검색 + 필터 + 페이징)
    PageResponseDTO<UserListDTO> getUsers(PageRequestDTO request);

    // 사원 상세 조회
    UserDetailsDTO getUser(Long id);

    // 사원 등록
    Long createUser(UserCreateDTO dto);

    // 사원 수정
    void updateUser(Long id, UserUpdateDTO dto);

    // 퇴사 처리
    void resignUser(Long id);

    // 계정 잠금 해제
    void unlockUser(Long id);
}
