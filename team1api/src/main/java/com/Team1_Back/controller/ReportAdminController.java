package com.Team1_Back.controller;

import com.Team1_Back.repository.ReportJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class ReportAdminController {

    private final ReportJobRepository reportJobRepository; // 또는 ReportLookupRepository

    @GetMapping("/departments")
    public Map<String, Object> departments() {
        return Map.of("items", reportJobRepository.findDistinctDepartmentNames());
    }
}

