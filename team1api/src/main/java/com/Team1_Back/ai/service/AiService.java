package com.Team1_Back.ai.service;

import com.Team1_Back.dto.TodoDTO;

import java.util.List;

public interface AiService {
    String generate(String prompt);

    // 회의록 파일을 분석하여 Todo 목록을 추출
    List<TodoDTO> analyzeMeetingNote(String fileContent);
}
