package com.Team1_Back.client;
//지피티랑 제미나이 공통 응답 처리

import com.Team1_Back.dto.LlmResult;


public interface LlmClient {
    LlmResult ask(String prompt);
}


