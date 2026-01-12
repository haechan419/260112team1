package com.Team1_Back.domain;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductImage {
    private String fileName;
    private int ord;
    public void setOrd(int ord){ this.ord = ord; }
}