package com.Team1_Back.dto;


import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadMessageWithAttachmentsResponse {

    private Long messageId;

    @Builder.Default
    private List<ChatAttachmentDto> attachments = List.of();
}


