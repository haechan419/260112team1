package com.Team1_Back.controller;

import com.Team1_Back.dto.ReceiptDTO;
import com.Team1_Back.dto.ReceiptExtractionDTO;
import com.Team1_Back.dto.UserDTO;
import com.Team1_Back.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 영수증 관리를 위한 REST API 컨트롤러
 * 
 * <p>사용자가 영수증을 업로드하고 조회하는 기능을 제공합니다.
 * 모든 엔드포인트는 인증이 필요하며, 사용자는 본인의 영수증만 접근할 수 있습니다.
 * 
 * @author Team1
 * @since 1.0
 */
@RestController
@RequestMapping("/api/receipt/receipts")
@Slf4j
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    /**
     * 영수증 이미지를 업로드합니다.
     * 
     * <p>지출 내역에 영수증 이미지를 첨부합니다.
     * 업로드된 이미지는 AI로 분석되어 정보가 추출됩니다.
     * 
     * @param expenseId 영수증을 첨부할 지출 내역 ID
     * @param file 업로드할 영수증 이미지 파일
     * @param principal 인증된 사용자 정보
     * @return 업로드된 영수증의 ID를 포함한 Map
     */
    @PostMapping("/upload")
    public Map<String, Long> upload(
            @RequestParam("expenseId") Long expenseId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDTO principal) {
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        Long userId = principal.getId();
        ReceiptDTO dto = receiptService.upload(expenseId, userId, file);
        return Map.of("result", dto.getId());
    }

    /**
     * 특정 영수증의 상세 정보를 조회합니다.
     * 
     * <p>본인이 업로드한 영수증만 조회할 수 있습니다.
     * 
     * @param id 조회할 영수증 ID
     * @param principal 인증된 사용자 정보
     * @return 영수증 상세 정보
     */
    @GetMapping("/{id}")
    public ReceiptDTO get(@PathVariable(name="id") Long id, @AuthenticationPrincipal UserDTO principal) {
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        Long userId = principal.getId();
        return receiptService.get(id, userId);
    }

    /**
     * 영수증 이미지를 조회합니다.
     * 
     * <p>본인이 업로드한 영수증 이미지만 조회할 수 있습니다.
     * 
     * @param id 조회할 영수증 ID
     * @param principal 인증된 사용자 정보
     * @return 영수증 이미지 리소스
     */
    @GetMapping("/{id}/image")
    public ResponseEntity<Resource> getImage(@PathVariable(name="id") Long id, @AuthenticationPrincipal UserDTO principal) {
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        Long userId = principal.getId();
        Resource resource = receiptService.getImage(id, userId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    /**
     * 영수증에서 추출된 정보를 조회합니다.
     * 
     * <p>AI로 추출된 영수증 정보(가맹점, 금액, 카테고리 등)를 반환합니다.
     * 본인이 업로드한 영수증의 정보만 조회할 수 있습니다.
     * 
     * @param id 조회할 영수증 ID
     * @param principal 인증된 사용자 정보
     * @return 영수증 추출 정보
     */
    @GetMapping("/{id}/extraction")
    public ReceiptExtractionDTO getExtraction(@PathVariable(name="id") Long id, @AuthenticationPrincipal UserDTO principal) {
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        Long userId = principal.getId();
        return receiptService.getExtraction(id, userId);
    }

    /**
     * 영수증을 삭제합니다.
     * 
     * <p>본인이 업로드한 영수증만 삭제할 수 있습니다.
     * 
     * @param id 삭제할 영수증 ID
     * @param principal 인증된 사용자 정보
     * @return 삭제 결과를 포함한 Map
     */
    @DeleteMapping("/{id}")
    public Map<String, String> remove(@PathVariable(name="id") Long id, @AuthenticationPrincipal UserDTO principal) {
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        Long userId = principal.getId();
        receiptService.remove(id, userId);
        return Map.of("RESULT", "SUCCESS");
    }
}

