package com.Team1_Back.security;

import com.Team1_Back.dto.UserDTO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {

    private CurrentUser() {}

    /** 로그인 유저의 DB PK(id) */
    public static Long id() {
        UserDTO u = principal();
        if (u.getId() == null) throw new IllegalStateException("UserDTO.id is null");
        return u.getId();
    }

    /** 로그인 유저의 사번(employeeNo) */
    public static String employeeNo() {
        return principal().getEmployeeNo();
    }

    /** 로그인 유저의 이름 */
    public static String name() {
        return principal().getName();
    }

    /** principal(UserDTO) 얻기 */
    public static UserDTO principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Unauthenticated");
        }

        Object principal = auth.getPrincipal();

        // 익명 사용자 등 방어
        if (principal == null || "anonymousUser".equals(principal)) {
            throw new IllegalStateException("Unauthenticated (anonymous)");
        }

        if (principal instanceof UserDTO user) {
            return user;
        }

        // 혹시 다른 타입으로 들어오는 경우 디버깅 도움
        throw new IllegalStateException("Unexpected principal type: " + principal.getClass().getName());
    }
}
