package com.Team1_Back.dto; // ✨ 하위 패키지로 분리

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RequestItemDTO {
    private Long pno; // 상품 번호
    private String pname; // 상품명
    private int quantity; // 수량
    private int price; // 단가
}