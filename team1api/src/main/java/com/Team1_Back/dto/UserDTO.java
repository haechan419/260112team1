package com.Team1_Back.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UserDTO extends User {

    private Long id;
    private String employeeNo;
    private String password;
    private String name;
    private String email;
    private String departmentName;
    private boolean active;
    private boolean locked;
    private int failedLoginCount;
    private List<String> roleNames = new ArrayList<>();
    private String profileImageUrl;  // 프로필 이미지 URL 추가

    public UserDTO(Long id, String employeeNo, String password, String name,
                   String email, String departmentName, boolean active,
                   boolean locked, int failedLoginCount, List<String> roleNames) {
        this(id, employeeNo, password, name, email, departmentName, active, locked, failedLoginCount, roleNames, null);
    }

    public UserDTO(Long id, String employeeNo, String password, String name,
                   String email, String departmentName, boolean active,
                   boolean locked, int failedLoginCount, List<String> roleNames,
                   String profileImageUrl) {

        super(
                employeeNo,
                password,
                true,                    // enabled (isEnabled()에서 재정의)
                true,                    // accountNonExpired
                true,                    // credentialsNonExpired
                !locked,                 // accountNonLocked
                (roleNames == null ? List.<String>of() : roleNames).stream()
                        .map(str -> str.startsWith("ROLE_") ? str : "ROLE_" + str) // ✅ 중복 방지
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList())
        );

        this.id = id;
        this.employeeNo = employeeNo;
        this.password = password;
        this.name = name;
        this.email = email;
        this.departmentName = departmentName;
        this.active = active;
        this.locked = locked;
        this.failedLoginCount = failedLoginCount;
        this.roleNames = (roleNames == null) ? new ArrayList<>() : roleNames;
        this.profileImageUrl = profileImageUrl;
    }

    // 응답용 데이터
    public Map<String, Object> getClaims() {
        Map<String, Object> dataMap = new HashMap<>();

        dataMap.put("id", id);
        dataMap.put("employeeNo", employeeNo);
        dataMap.put("name", name);
        dataMap.put("email", email);
        dataMap.put("departmentName", departmentName);
        dataMap.put("roleNames", roleNames);
        dataMap.put("profileImageUrl", profileImageUrl);  // 프로필 이미지 URL 추가

        return dataMap;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
