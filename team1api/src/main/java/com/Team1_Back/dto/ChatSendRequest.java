package com.Team1_Back.dto;


import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ChatSendRequest {
    private Long roomId;
    private String content;
}
