package com.Team1_Back.repository;

import com.Team1_Back.domain.FaceAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FaceAuthRepository extends JpaRepository<FaceAuth, String> {

    //이 쿼리 메서드가 정의되어 있어야 컨트롤러가 부를 수 있습니다.
    @Query("SELECT f FROM FaceAuth f JOIN FETCH f.user")
    List<FaceAuth> findAllWithUser();

    //사번으로 찾기 기능 정의
    // (User 엔티티 안에 employeeNo 필드가 있어야 작동함)
    Optional<FaceAuth> findByUser_EmployeeNo(String employeeNo);
}