package com.Team1_Back.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_notification")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long nno;

    private String receiver; // 알림 받는 사람 (requester)
    private String message;  // 알림 내용 (예: "노트북 구매 요청이 승인되었습니다.")
    
    @Builder.Default
    private boolean isRead = false; // 읽음 여부 (종 버튼 누르면 true로 변경 예정)

    @CreatedDate
    private LocalDateTime regDate; // 알림 생성 시간
    
    public void changeRead(boolean isRead){
        this.isRead = isRead;
    }
}