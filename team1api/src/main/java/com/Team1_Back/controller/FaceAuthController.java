package com.Team1_Back.controller;

import com.Team1_Back.domain.FaceAuth;
import com.Team1_Back.domain.User;
import com.Team1_Back.repository.FaceAuthRepository;
import com.Team1_Back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/face")
@RequiredArgsConstructor
@Slf4j
public class FaceAuthController {

    private final FaceAuthRepository faceAuthRepository;
    private final UserRepository userRepository;

    // 얼굴 등록
    @PutMapping("/register")
    @Transactional
    public Map<String, String> register(@RequestBody Map<String, String> body, Principal principal) {
        String empNo = principal.getName(); 
        String descriptor = body.get("descriptor");

        log.info("📸 얼굴 등록 요청 사번: " + empNo);

        User user = userRepository.findByEmployeeNo(empNo)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. 사번: " + empNo));

        Optional<FaceAuth> existingAuth = faceAuthRepository.findByUser_EmployeeNo(empNo);

        if (existingAuth.isPresent()) {
            FaceAuth faceAuth = existingAuth.get();
            faceAuth.changeDescriptor(descriptor);
            faceAuthRepository.save(faceAuth);
        } else {
            FaceAuth faceAuth = FaceAuth.builder()
                    .user(user)
                    .faceDescriptor(descriptor)
                    .build();
            faceAuthRepository.save(faceAuth);
        }

        return Map.of("result", "SUCCESS");
    }

    // 얼굴 로그인용 전체 목록
    @GetMapping("/list")
    public List<Map<String, String>> getAllFaces() {
        return faceAuthRepository.findAllWithUser().stream()
                .map(face -> Map.of(
                        "userId", face.getUser().getEmployeeNo(), 
                        "faceDescriptor", face.getFaceDescriptor()
                ))
                .collect(Collectors.toList());
    }
    // [상태 확인] 내 얼굴이 등록되어 있는지 확인
    @GetMapping("/check")
    public ResponseEntity<Boolean> checkFaceStatus(@RequestParam("userId") String employeeNo) {        
        Optional<FaceAuth> faceAuth = faceAuthRepository.findByUser_EmployeeNo(employeeNo);
        return ResponseEntity.ok(faceAuth.isPresent());
    }

    // [삭제] 얼굴 데이터 삭제
    @DeleteMapping("/remove")
    @Transactional
    public ResponseEntity<Map<String, String>> removeFaceData(@RequestParam("userId") String employeeNo) {
        try {
            Optional<FaceAuth> faceAuth = faceAuthRepository.findByUser_EmployeeNo(employeeNo);

            if (faceAuth.isPresent()) {
                faceAuthRepository.delete(faceAuth.get());
                return ResponseEntity.ok(Map.of("result", "success", "message", "삭제되었습니다."));
            } else {
                return ResponseEntity.status(404).body(Map.of("result", "fail", "message", "등록된 데이터가 없습니다."));
            }
        } catch (Exception e) {
            log.error("삭제 중 에러 발생: ", e);
            return ResponseEntity.status(500).body(Map.of("result", "error", "message", e.getMessage()));
        }
    }
}