package com.Team1_Back.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_request_item")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "request")
public class RequestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ino;

    private Long pno; // 상품 번호 (나중에 상품이 삭제돼도 기록은 남아야 하므로 ID만 저장하거나 이름도 같이 저장)
    private String pname; // 당시 상품명
    private int quantity; // 수량
    private int price; // 당시 가격

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_rno")
    @Setter
    private Request request; // 어떤 주문서에 속해있는지
}