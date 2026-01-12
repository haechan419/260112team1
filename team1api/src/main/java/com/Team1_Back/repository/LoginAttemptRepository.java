package com.Team1_Back.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Team1_Back.domain.LoginAttempt;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long>{
    // 일단 기본적인 CRUD 만 사용 
    // 추후 추가 예정
}
