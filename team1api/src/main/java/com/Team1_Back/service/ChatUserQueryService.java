package com.Team1_Back.service;

import com.Team1_Back.dto.UserSearchItemResponse;
import com.Team1_Back.repository.UserRepository;
import com.Team1_Back.repository.projection.UserSearchRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatUserQueryService {

    private final UserRepository userRepository;

    public List<UserSearchItemResponse> searchUsers(Long meId, String q, Integer limit) {
        String keyword = (q == null) ? "" : q.trim();
        int lim = (limit == null) ? 20 : Math.min(Math.max(limit, 1), 50);

        if (keyword.isEmpty()) return List.of();

        List<UserSearchRow> rows = userRepository.searchUsers(meId, keyword, lim);
        return rows.stream()
                .map(r -> new UserSearchItemResponse(
                        r.getUserId(),
                        r.getName(),
                        r.getEmployeeNo(),
                        r.getDepartmentName()
                ))
                .toList();
    }
}

