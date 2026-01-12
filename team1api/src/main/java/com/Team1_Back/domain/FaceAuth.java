package com.Team1_Back.domain;

import lombok.*;
import jakarta.persistence.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "user")
@Table(name = "face_auth")
public class FaceAuth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fno;

    //핵심: User는 가만히 있고, FaceAuth가 User를 쳐다봅니다.
    @OneToOne(fetch = FetchType.EAGER) // EAGER: "얼굴 데이터 가져올 때 유저 정보(이름)도 무조건 챙겨와!"
    @JoinColumn(name = "user_id")
    private User user;

    @Lob // 대용량 텍스트
    @Column(columnDefinition = "TEXT")
    private String faceDescriptor;

    public void changeDescriptor(String descriptor) {
        this.faceDescriptor = descriptor;
    }
}