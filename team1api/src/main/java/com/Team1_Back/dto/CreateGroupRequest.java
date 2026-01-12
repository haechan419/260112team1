package com.Team1_Back.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class CreateGroupRequest {
    private List<Long> memberUserIds;
}

