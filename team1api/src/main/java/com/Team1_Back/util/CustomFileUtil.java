package com.Team1_Back.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class CustomFileUtil {

    //    @Value("${file.upload.dir:./uploads}")
    @Value("${com.team1.upload.path}") // application.properties 확인 필요
    private String uploadDir;

    // 시작 시 업로드 폴더 자동 생성
    @PostConstruct
    public void init() {
        File tempFolder = new File(uploadDir);
        if (!tempFolder.exists()) {
            tempFolder.mkdir();
        }
        uploadDir = tempFolder.getAbsolutePath();
        log.info("-------------------------------------");
        log.info("Upload Path: " + uploadDir);
    }

    /**
     * 파일을 저장하고 절대 경로를 반환합니다.
     *
     * @param file    저장할 파일
     * @param subPath 하위 디렉토리 (예: "receipts")
     * @return 저장된 파일의 절대 경로
     */
    // 단일 파일을 저장하고 절대 경로를 반환
    public String saveFile(MultipartFile file, String subPath) {
        if (file.isEmpty()) {
            throw new RuntimeException("파일이 비어있습니다.");
        }

        try {
            // 업로드 디렉토리 생성
            Path uploadPath = Paths.get(uploadDir, subPath);
            Files.createDirectories(uploadPath);

            // 파일명 생성 (UUID + 원본 파일명)
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String savedFilename = UUID.randomUUID().toString() + extension;

            // 파일 저장
            Path filePath = uploadPath.resolve(savedFilename);
            Files.copy(file.getInputStream(), filePath);

            // 절대 경로 반환 (Paths.get()으로 사용할 수 있도록)
            return filePath.toAbsolutePath().toString();

        } catch (IOException e) {
            log.error("파일 저장 실패: {}", e.getMessage());
            throw new RuntimeException("파일 저장에 실패했습니다.", e);
        }
    }

    // 다중 파일 업로드 및 이미지 썸네일 생성
    public List<String> saveFiles(List<MultipartFile> files) throws RuntimeException {
        if (files == null || files.size() == 0) {
            return null;

        }

        List<String> uploadNames = new ArrayList<>();
        for (MultipartFile multipartFile : files) {
            String savedName = UUID.randomUUID().toString() + "_" + multipartFile.getOriginalFilename();
            Path savePath = Paths.get(uploadDir, savedName);

            try {
                Files.copy(multipartFile.getInputStream(), savePath);
                String contentType = multipartFile.getContentType();

                if (contentType != null && contentType.startsWith("image")) {
                    Path thumbnailPath = Paths.get(uploadDir, "s_" + savedName);
                    Thumbnails.of(savePath.toFile())
                            .size(200, 200)
                            .toFile(thumbnailPath.toFile());
                }
                uploadNames.add(savedName);
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage());
            }
        }
        return uploadNames;
    }

    // 파일 조회를 위한 ResponseEntity 반환
    public ResponseEntity<Resource> getFile(String fileName) {
        Resource resource = new FileSystemResource(uploadDir + File.separator + fileName);
        if (!resource.isReadable()) {
            resource = new FileSystemResource(uploadDir + File.separator + "default.jpg"); // 기본 이미지 필요 시
        }
        HttpHeaders headers = new HttpHeaders();
        try {
            headers.add("Content-Type", Files.probeContentType(resource.getFile().toPath()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok().headers(headers).body(resource);
    }

    /**
     * 파일 경로를 Resource로 변환합니다.
     * 파일이 없으면 NoSuchElementException을 던져서 ControllerAdvice가 404로 처리할 수 있도록 합니다.
     *
     * @param filePath 파일 경로
     * @return Resource 객체
     * @throws java.util.NoSuchElementException 파일이 없을 때
     */
    public Resource getFileAsResource(Path filePath) {
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                // 파일이 없을 때 NoSuchElementException을 던져서 ControllerAdvice가 404로 처리
                throw new java.util.NoSuchElementException("파일을 찾을 수 없습니다: " + filePath);
            }
        } catch (java.util.NoSuchElementException e) {
            // NoSuchElementException은 그대로 전파
            throw e;
        } catch (Exception e) {
            log.error("파일 로드 실패: {}", e.getMessage());
            // 기타 예외는 RuntimeException으로 래핑
            throw new RuntimeException("파일을 로드할 수 없습니다.", e);
        }
    }

    /**
     * 파일을 삭제합니다.
     *
     * @param filePath 삭제할 파일 경로
     */
    // 단일 파일 삭제
    public void deleteFile(Path filePath) {
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("파일 삭제 실패: {}", e.getMessage());
            throw new RuntimeException("파일 삭제에 실패했습니다.", e);
        }
    }

    // 다중 파일 삭제(썸네일 포함)
    public void deleteFiles(List<String> fileNames) {
        if (fileNames == null || fileNames.size() == 0) return;

        fileNames.forEach(fileName -> {
            String thumbnailFileName = "s_" + fileName;
            Path thumbnailPath = Paths.get(uploadDir, thumbnailFileName);
            Path filePath = Paths.get(uploadDir, fileName);
            try {
                Files.deleteIfExists(filePath);
                Files.deleteIfExists(thumbnailPath);
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage());
            }
        });
    }

    /**
     * 파일의 SHA-256 해시값을 생성합니다.
     *
     * @param file 해시를 생성할 파일
     * @return SHA-256 해시값 (64자리 hex 문자열), 실패 시 null
     */
    public String generateFileHash(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            log.error("파일 해시 생성 실패: {}", e.getMessage());
            return null;
        }
    }
}

