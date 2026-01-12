package com.Team1_Back.repository;

import com.Team1_Back.dto.PageRequestDTO;
import com.Team1_Back.dto.UserListDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserRepositoryCustom {

    // 사원 목록 조회 (검색 + 필터 + 페이징)
    Page<UserListDTO> searchUsers(PageRequestDTO request, Pageable pageable);

    // 부서 목록 조회 (필터용)
    List<String> findAllDepartments();
}
