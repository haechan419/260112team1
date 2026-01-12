package com.Team1_Back.repository;

import com.Team1_Back.domain.Product;
import com.Team1_Back.domain.ProductImage;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.IntStream;

@SpringBootTest
@Slf4j
public class ProductInsertTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    @Transactional
    @Commit // ✨ 테스트가 끝나도 DB에 데이터를 남기는 마법의 주문!
    public void insertDummyData() {
        
        // 1. 카테고리 배열 준비
        String[] categories = {"사무용품", "전자기기", "가구", "탕비실"};
        
        // 2. 준비한 샘플 이미지 파일명 (실제 upload 폴더에 이 파일들이 있어야 함)
        String[] sampleImages = {"s1.jpg", "s2.jpg", "s3.jpg"};

        // 3. 카테고리별로 10개씩 생성 루프
        for (String category : categories) {
            
            IntStream.rangeClosed(1, 10).forEach(i -> {
                
                // 상품 기본 정보 생성
                Product product = Product.builder()
                        .pname(category + " 상품 " + i) // 예: 사무용품 상품 1
                        .pdesc("이것은 " + category + " 테스트 상품 " + i + "번에 대한 상세 설명입니다. 무한 스크롤 테스트 중!")
                        .price((i * 1000) + 500) // 가격도 다르게 (1500원, 2500원...)
                        .category(category) // 카테고리 설정
                        .status(true) // 판매 중
                        .build();

                // 이미지 추가 (이미지 리스트에 넣기)
                // (순서대로 s1, s2, s3 이미지를 번갈아가며 사용)
                String targetImage = sampleImages[i % sampleImages.length]; 
                
                product.addImageString(UUID.randomUUID().toString() + "_" + targetImage); // 실제 저장될 이름 형식

                // DB 저장
                productRepository.save(product);
            });
        }
        
        log.info("✅ 더미 데이터 생성 완료! (총 " + (categories.length * 10) + "개)");
    }
}