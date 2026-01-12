package com.Team1_Back.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tbl_product")
@Getter
@ToString(exclude = "imageList")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pno;

    @Column(length = 200, nullable = false)
    private String pname;

    private int price;

    @Column(length = 1000)
    private String pdesc;

    @Column(length = 50)
    private String category;

    private int stockQuantity;

    private boolean delFlag;

    // 기본값을 true(판매중)로 설정
    @Builder.Default
    private boolean status = true;

    @Column(columnDefinition = "int default 0")
    private int ord;

    @ElementCollection
    @Builder.Default
    private List<ProductImage> imageList = new ArrayList<>();

    public void changePrice(int price) {
        this.price = price;
    }

    public void changeDesc(String desc) {
        this.pdesc = desc;
    }

    public void changeName(String name) {
        this.pname = name;
    }

    public void changeCategory(String category) {
        this.category = category;
    }

    public void changeStock(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public void changeDel(boolean delFlag) {
        this.delFlag = delFlag;
    }

    // 순서 변경용 메서드
    public void changeOrd(int ord) {
        this.ord = ord;
    }

    public void addImage(ProductImage image) {
        image.setOrd(this.imageList.size());
        imageList.add(image);
    }

    public void addImageString(String fileName) {
        ProductImage productImage = ProductImage.builder().fileName(fileName).build();
        addImage(productImage);
    }

    public void clearList() {
        this.imageList.clear();
    }

    // 상태 변경 메서드
    public void changeStatus(boolean status) {
        this.status = status;
    } // 🚨 [수정] 여기가 빠져 있었습니다! 이제 에러가 사라질 겁니다.

    // 재고 감소 메서드 (비즈니스 로직)
    public void removeStock(int quantity) {
        int restStock = this.stockQuantity - quantity;

        if (restStock < 0) {
            // 재고가 부족하면 에러를 터뜨려서 승인을 막아버림
            throw new IllegalStateException("상품의 재고가 부족합니다. (현재 재고: " + this.stockQuantity + ")");
        }
        this.stockQuantity = restStock;
    }

    // 재고 증가 메서드 (혹시 나중에 승인 취소/반려 시 원복용)
    public void addStock(int quantity) {
        this.stockQuantity += quantity;
    }
}