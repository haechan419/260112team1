package com.Team1_Back.util;

import com.Team1_Back.dto.UserDTO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    public static Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return null;

        Object p = auth.getPrincipal();
        if (p instanceof UserDTO dto) {
            return dto.getId();
        }
        return null;
    }
}

