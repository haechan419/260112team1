package com.Team1_Back.security.listener;


import com.Team1_Back.dto.UserDTO;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AuthFacade {

    public UserDTO requireUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthenticated");
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof UserDTO dto) {
            if (dto.getId() == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "principal id is null");
            }
            return dto;
        }

        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Unsupported principal: " + principal.getClass().getName()
        );
    }

    public Long requireUserId() {
        return requireUser().getId();
    }
}
