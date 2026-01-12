package com.Team1_Back.ai.service;

import com.Team1_Back.ai.ReceiptAiProperties;
import com.Team1_Back.ai.dto.ReceiptExtractionResponseDTO;
import com.Team1_Back.dto.ReceiptExtractionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * ✅ 새로 생성: 영수증 OCR 통합 - 영수증 AI 추출 서비스 구현체
 * Python AI 서비스를 호출하여 영수증 이미지에서 정보를 추출합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReceiptAiServiceImpl implements ReceiptAiService {

    private final RestClient receiptAiRestClient;
    private final ReceiptAiProperties props;

    @Override
    public ReceiptExtractionDTO extractReceipt(MultipartFile imageFile) {
        try {
            log.info("[ReceiptAI] extractReceipt 메서드 호출됨: filename={}, size={} bytes", 
                     imageFile.getOriginalFilename(), imageFile.getSize());
            
            // MultipartFile을 MultiValueMap으로 변환
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource resource = new ByteArrayResource(imageFile.getBytes()) {
                @Override
                public String getFilename() {
                    return imageFile.getOriginalFilename() != null 
                        ? imageFile.getOriginalFilename() 
                        : "receipt.jpg";
                }
            };
            body.add("file", resource);
            
            log.info("[ReceiptAI] 요청 바디 준비 완료: filename={}, bodySize={} bytes", 
                     resource.getFilename(), imageFile.getSize());

            // Python AI 서비스 호출
            String fullUrl = props.getBaseUrl() + "/api/ai/receipt/extract";
            log.info("[ReceiptAI] Python AI 서비스 호출 시작: baseUrl={}, fullUrl={}", props.getBaseUrl(), fullUrl);
            
            ReceiptExtractionResponseDTO response = receiptAiRestClient.post()
                    .uri("/api/ai/receipt/extract")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(ReceiptExtractionResponseDTO.class);

            log.info("[ReceiptAI] Python AI 서비스 응답 받음: response={}", response);

            if (response == null) {
                log.error("[ReceiptAI] AI 서비스 응답이 null입니다!");
                throw new RuntimeException("AI 서비스 응답이 null입니다.");
            }

            // 에러 체크
            if (response.getError() != null && !response.getError().isBlank()) {
                log.error("[ReceiptAI] AI 서비스 에러: {}", response.getError());
                throw new RuntimeException("AI 서비스 에러: " + response.getError());
            }

            log.info("[ReceiptAI] Python AI 서비스 호출 성공: merchant={}, amount={}, date={}, category={}", 
                     response.getExtractedMerchant(),
                     response.getExtractedAmount(),
                     response.getExtractedDate(),
                     response.getExtractedCategory());

            // ReceiptExtractionResponseDTO를 ReceiptExtractionDTO로 변환
            ReceiptExtractionDTO result = convertToDTO(response);
            log.info("[ReceiptAI] DTO 변환 완료: merchant={}, amount={}, date={}", 
                     result.getExtractedMerchant(), result.getExtractedAmount(), result.getExtractedDate());
            
            return result;

        } catch (Exception e) {
            log.error("[ReceiptAI] Python AI 서비스 호출 실패: error={}, message={}", 
                     e.getClass().getSimpleName(), e.getMessage(), e);
            throw new RuntimeException("영수증 AI 추출 실패: " + e.getMessage(), e);
        }
    }

    // Python API 응답을 내부 DTO로 변환
    private ReceiptExtractionDTO convertToDTO(ReceiptExtractionResponseDTO response) {
        log.info("[ReceiptAI] convertToDTO 시작: response={}", response);
        
        ReceiptExtractionDTO.ReceiptExtractionDTOBuilder builder = ReceiptExtractionDTO.builder()
                .modelName(response.getModelName())
                .extractedMerchant(response.getExtractedMerchant())
                .extractedAmount(response.getExtractedAmount())
                .extractedCategory(response.getExtractedCategory())
                .extractedDescription(response.getExtractedDescription())
                .confidence(response.getConfidence())
                .extractedJson(response.getExtractedJson());

        // extractedDate 문자열을 LocalDate로 변환
        if (response.getExtractedDate() != null && !response.getExtractedDate().isBlank()) {
            try {
                LocalDate date = LocalDate.parse(response.getExtractedDate(), DateTimeFormatter.ISO_LOCAL_DATE);
                builder.extractedDate(date);
                log.info("[ReceiptAI] 날짜 파싱 성공: {} -> {}", response.getExtractedDate(), date);
            } catch (DateTimeParseException e) {
                log.warn("[ReceiptAI] 날짜 파싱 실패: {}, error={}", response.getExtractedDate(), e.getMessage());
                builder.extractedDate(null);
            }
        } else {
            log.warn("[ReceiptAI] extractedDate가 null이거나 빈 문자열: {}", response.getExtractedDate());
        }

        ReceiptExtractionDTO result = builder.build();
        log.info("[ReceiptAI] convertToDTO 완료: result={}", result);
        
        return result;
    }
}

