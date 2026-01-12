package com.Team1_Back.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class ChatRoomMetaResponse {
    private Long roomId;
    private Long meLastReadMessageId;
    //private Long otherLastReadMessageId;
    Map<Long, Long> memberLastReadMessageIdMap;// userId -> lastReadMessageId (null 가능)
    int memberCount;
}
