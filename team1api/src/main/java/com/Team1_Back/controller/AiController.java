package com.Team1_Back.controller;

import com.Team1_Back.ai.dto.AiGenerateRequestDTO;
import com.Team1_Back.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    @PostMapping("/generate")
    public Map<String, Object> generate(@RequestBody AiGenerateRequestDTO req) {
        String result = aiService.generate(req.getPrompt());
        return Map.of(
                "ok", true,
                "result", result
        );
    }
}