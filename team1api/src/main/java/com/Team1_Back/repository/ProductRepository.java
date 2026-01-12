package com.Team1_Back.repository;

import com.Team1_Back.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 1. 상세 조회 (유지)
    @Query("select p from Product p left join fetch p.imageList pi where pi.ord = 0 and p.delFlag = false and p.pno = :pno")
    Optional<Product> selectOne(@Param("pno") Long pno);

    // 2. [전체] 목록 조회 (성공한 부분 - 유지)
    @Query("select p from Product p")
    Page<Product> selectList(Pageable pageable);

    // ---------------------------------------------------------
    // ✨ [여기가 범인!] 3. 카테고리별 목록 조회
    // ---------------------------------------------------------
    // [수정 전] 아마도 여기에 where ... pi.ord = 0 ... 이런 게 남아있었을 겁니다.
    // [수정 후] 조건 싹 빼고 "카테고리 이름"만 맞으면 다 가져오게 변경!
    @Query("select p from Product p where p.category = :category")
    Page<Product> findByCategory(@Param("category") String category, Pageable pageable);

    // 4. 삭제 처리 (유지)
    @Modifying
    @Query("UPDATE Product p SET p.delFlag = :flag WHERE p.pno = :pno")
    void updateToDelete(@Param("pno") Long pno, @Param("flag") boolean flag);
}