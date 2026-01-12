package com.Team1_Back.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tbl_request")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "items")
@EntityListeners(AuditingEntityListener.class) // 날짜 자동 기록용
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long rno;

    private String requester; // 요청자
    private String reason; // 사유
    private int totalAmount; // 총 금액

    @Builder.Default
    private String status = "PENDING"; // 초기값: 대기중 (PENDING, APPROVED, REJECTED)

    private String rejectReason; // 반려 사유

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime regDate; // 등록 시간 자동생성

    // 주문 하나에 여러 상품이 달림 (1:N)
    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RequestItem> items = new ArrayList<>();

    public void addItem(RequestItem item) {
        item.setRequest(this); // 양방향 연결
        this.items.add(item);
    }

    // 상태 변경 메서드 (승인/반려 시 사용)
    public void changeStatus(String status, String rejectReason) {
        this.status = status;
        this.rejectReason = rejectReason;
    }
}