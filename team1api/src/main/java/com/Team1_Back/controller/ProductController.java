package com.Team1_Back.controller;

import com.Team1_Back.dto.PageRequestDTO;
import com.Team1_Back.dto.PageResponseDTO;
import com.Team1_Back.dto.ProductDTO;
import com.Team1_Back.service.ProductService;
import com.Team1_Back.util.CustomFileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final CustomFileUtil fileUtil;

    @Value("${com.team1.upload.path}")
    private String uploadPath;

    // 1. 이미지 조회
    @GetMapping("/view/{fileName}")
    public ResponseEntity<Resource> viewFileGET(@PathVariable("fileName") String fileName) {
        Resource resource = new FileSystemResource(uploadPath + File.separator + fileName);
        String resourceName = resource.getFilename();
        HttpHeaders headers = new HttpHeaders();

        try {
            if(!resource.exists()) return ResponseEntity.notFound().build();
            headers.add("Content-Type", Files.probeContentType(resource.getFile().toPath()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok().headers(headers).body(resource);
    }

    // 2. 목록 조회
    @GetMapping("/list")
    public PageResponseDTO<ProductDTO> list(PageRequestDTO pageRequestDTO){
        return productService.getList(pageRequestDTO);
    }

    // 3. 상품 등록
    @PostMapping("/")
    public Map<String, Long> register(ProductDTO productDTO) {
        log.info("상품 등록: " + productDTO);
        List<MultipartFile> files = productDTO.getFiles();
        List<String> uploadFileNames = fileUtil.saveFiles(files);
        productDTO.setUploadFileNames(uploadFileNames);
        
        Long pno = productService.register(productDTO);
        return Map.of("result", pno);
    }

    // 4. 상세 조회
    @GetMapping("/{pno}")
    public ProductDTO read(@PathVariable(name="pno") Long pno){
        return productService.get(pno);
    }

    // 5. 재고 수정

    @PutMapping("/{pno}")
    public Map<String, String> modify(@PathVariable(name="pno") Long pno, ProductDTO productDTO) {
        
        productDTO.setPno(pno);

        // 1. 기존(DB)에 저장된 상품 정보 가져오기 (비교를 위해)
        ProductDTO oldProductDTO = productService.get(pno);
        List<String> oldFileNames = oldProductDTO.getUploadFileNames(); // 기존 이미지들

        // 2. 새로 업로드된 파일이 있는지 확인 및 저장
        List<MultipartFile> files = productDTO.getFiles();
        List<String> newUploadedFileNames = fileUtil.saveFiles(files); // 새로 저장된 파일명들

        // 3. 프론트에서 "이거 유지해줘"라고 보낸 파일명 리스트
        List<String> uploadedFileNames = productDTO.getUploadFileNames(); 

        // 상황 A: 새 파일도 없고, 유지할 파일 리스트도 안 보냈다? -> "이미지 수정 안 함"으로 간주
        if ((files == null || files.isEmpty()) && (uploadedFileNames == null || uploadedFileNames.isEmpty())) {
            // 👉 "기존 이미지 그대로 유지해!"
            uploadedFileNames = oldFileNames; 
        } 
        // 상황 B: 뭔가 변화가 있다 (새 파일을 올렸거나, 기존 것 중 일부만 남김)
        else {
            if (uploadedFileNames == null) {
                uploadedFileNames = new java.util.ArrayList<>();
            }
            // 새 파일이 있다면 목록에 추가
            if (newUploadedFileNames != null && !newUploadedFileNames.isEmpty()) {
                uploadedFileNames.addAll(newUploadedFileNames);
            }
        }

        // 4. 최종 결정된 이미지 리스트를 DTO에 담기
        productDTO.setUploadFileNames(uploadedFileNames);

        // 5. 서비스 호출 (DB 업데이트)
        productService.modify(productDTO);

        // 6. 지워야 할 파일 정리 (기존 파일 중 최종 목록에 없는 것만 삭제)
        if(oldFileNames != null && oldFileNames.size() > 0){
            // 람다식에서 사용할 final 변수로 만들기 위해 재할당
            List<String> finalUploadedFileNames = uploadedFileNames; 
            
            List<String> removeFiles = oldFileNames.stream()
                    .filter(fileName -> finalUploadedFileNames == null || !finalUploadedFileNames.contains(fileName))
                    .collect(Collectors.toList());
            
            fileUtil.deleteFiles(removeFiles);
        }
        
        return Map.of("RESULT", "SUCCESS");
    }

    // 6. 삭제
    @DeleteMapping("/{pno}")
    public Map<String, String> remove(@PathVariable("pno") Long pno) {
        List<String> oldFileNames = productService.get(pno).getUploadFileNames();
        productService.remove(pno);
        fileUtil.deleteFiles(oldFileNames);
        return Map.of("RESULT", "SUCCESS");
    }

    // ✨ 7. 순서 변경 (서비스로 위임)
    @PutMapping("/order")
    public Map<String, String> changeOrder(@RequestBody List<Long> pnoList) {
        log.info("순서 변경 요청: " + pnoList);
        productService.changeOrder(pnoList); // 서비스가 알아서 함
        return Map.of("RESULT", "SUCCESS");
    }
}