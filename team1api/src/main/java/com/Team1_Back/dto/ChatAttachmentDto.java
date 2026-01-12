package com.Team1_Back.dto;

// package com.Team1_Back.chat.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatAttachmentDto {
    private Long attachmentId;
    private String originalName;
    private String mimeType;
    private Long size;
    private String url;
}
