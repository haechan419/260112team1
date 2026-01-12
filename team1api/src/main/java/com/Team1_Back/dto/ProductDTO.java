package com.Team1_Back.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductDTO {
    private Long pno;
    private String pname;
    private int price;
    private String pdesc;
    private boolean delFlag;

    // 추가된 필드
    private String category;
    private int stockQuantity;

    private int ord;

    // [추가] 판매 상태 (서비스 파일에서 이걸 찾고 있었습니다!)
    @Builder.Default
    private boolean status = true; // 기본값: true (판매중)

    @Builder.Default
    private List<MultipartFile> files = new ArrayList<>(); // 실제 파일 업로드용

    @Builder.Default
    private List<String> uploadFileNames = new ArrayList<>(); // 조회된 파일명 리스트
}