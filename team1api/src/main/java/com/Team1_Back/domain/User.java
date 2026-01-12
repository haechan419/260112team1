package com.Team1_Back.domain;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_no", nullable = false, unique = true, length = 50)
    private String employeeNo;

    @Column(length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String address;

    @Column(name = "address_detail", length = 255)
    private String addressDetail;

    @Column(name = "department_name", length = 100)
    private String departmentName;

    @Column(length = 50)
    private String position;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "failed_login_count", nullable = false)
    @Builder.Default
    private Integer failedLoginCount = 0;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_user_at", nullable = false, updatable = false)
    private LocalDateTime createdUserAt;

    @UpdateTimestamp
    @Column(name = "updated_user_at", nullable = false)
    private LocalDateTime updatedUserAt;

    // 계정 잠금 여부 확인
    public boolean isLocked() {
        return this.lockedAt != null;
    }

    // 퇴사 여부를 확인
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    // 로그인 실패 시 로그인 실패 횟수를 증가시킨다.
    public void increaseFailedLoginCount() {
        this.failedLoginCount++;
    }

    // 로그인 실패 횟수를 초기화한다.(로그인 성공 시)
    public void resetFailedLoginCount() {
        this.failedLoginCount = 0;
    }

    // 계정 잠금 시 계정 잠금 시각을 저장한다.
    public void lock() {
        this.lockedAt = LocalDateTime.now();
    }

    // 계정 잠금 해제 시 로그인 실패 횟수를 초기화한다.
    public void unlock() {
        this.lockedAt = null;
        this.failedLoginCount = 0;
    }

    // 관리자 권한 여부를 확인합니다.
    public boolean isAdmin() {
        return Role.ADMIN.equals(this.role);
    }
}
