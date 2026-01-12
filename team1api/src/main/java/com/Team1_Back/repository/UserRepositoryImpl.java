package com.Team1_Back.repository;

import com.Team1_Back.domain.QUser;
import com.Team1_Back.dto.PageRequestDTO;
import com.Team1_Back.dto.UserListDTO;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<UserListDTO> searchUsers(PageRequestDTO request, Pageable pageable) {

        QUser user = QUser.user;

        // 1. 동적 조건 생성
        BooleanBuilder builder = new BooleanBuilder();

        // 검색 조건 (이름 or 사번 or 이메일)
        if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
            String keyword = request.getKeyword();
            String searchType = request.getSearchType();

            if ("name".equals(searchType)) {
                builder.and(user.name.contains(keyword));
            } else if ("employeeNo".equals(searchType)) {
                builder.and(user.employeeNo.contains(keyword));
            } else if ("email".equals(searchType)) {
                builder.and(user.email.contains(keyword));
            } else {
                // 기본: 이름으로 검색
                builder.and(user.name.contains(keyword));
            }
        }

        // 부서 필터
        if (request.getDepartment() != null && !request.getDepartment().isEmpty()) {
            builder.and(user.departmentName.eq(request.getDepartment()));
        }

        // 잠금 상태 필터
        if (request.getIsLocked() != null) {
            if (request.getIsLocked()) {
                builder.and(user.lockedAt.isNotNull());
            } else {
                builder.and(user.lockedAt.isNull());
            }
        }

        // 재직 상태 필터
        if (request.getIsActive() != null) {
            builder.and(user.isActive.eq(request.getIsActive()));
        }

        // 2. 데이터 조회
        List<UserListDTO> content = queryFactory
                .select(Projections.bean(UserListDTO.class,
                        user.id,
                        user.employeeNo,
                        user.name,
                        user.departmentName,
                        user.email,
                        user.phone,
                        user.createdUserAt,
                        user.lockedAt.isNotNull().as("locked"),
                        user.isActive.as("active")
                ))
                .from(user)
                .where(builder)
                .orderBy(user.createdUserAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 3. 전체 개수 조회
        Long total = queryFactory
                .select(user.count())
                .from(user)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public List<String> findAllDepartments() {
        QUser user = QUser.user;

        return queryFactory
                .select(user.departmentName)
                .distinct()
                .from(user)
                .where(user.departmentName.isNotNull())
                .orderBy(user.departmentName.asc())
                .fetch();
    }
}
