package com.Team1_Back.service;

import com.Team1_Back.domain.Product;
import com.Team1_Back.domain.ProductImage;
import com.Team1_Back.dto.PageRequestDTO;
import com.Team1_Back.dto.PageResponseDTO;
import com.Team1_Back.dto.ProductDTO;
import com.Team1_Back.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    // 1. 목록 조회 (모든 상품 무조건 노출)
    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<ProductDTO> getList(PageRequestDTO pageRequestDTO) {

        // 정렬 조건: 최신순(pno 내림차순)
        Pageable pageable = PageRequest.of(
                pageRequestDTO.getPage() < 0 ? 0 : pageRequestDTO.getPage() - 1, // 0보다 작으면 0으로 보정
                pageRequestDTO.getSize(),
                Sort.by("ord").ascending() //
        );

        // 검색 분기 처리
        Page<Product> result;
        String category = pageRequestDTO.getCategory();

        // 1) 카테고리 필터링 ("All"이 아니고 값이 있을 때)
        if (category != null && !category.equals("All") && !category.isEmpty()) {
            result = productRepository.findByCategory(category, pageable);
        } else {
            // 2) 전체 검색 (조건 없는 selectList 호출)
            result = productRepository.selectList(pageable);
        }

        // 결과 변환 (Entity -> DTO)
        List<ProductDTO> dtoList = result.getContent().stream()
                .map(product -> entityToDTO(product))
                .collect(Collectors.toList());

        return PageResponseDTO.<ProductDTO>of(
                dtoList,
                pageRequestDTO,
                result.getTotalElements());
    }

    // 2. 등록
    @Override
    public Long register(ProductDTO productDTO) {
        Product product = dtoToEntity(productDTO);
        Product result = productRepository.save(product);
        return result.getPno();
    }

    // 3. 상세 조회
    @Override
    public ProductDTO get(Long pno) {
        Optional<Product> result = productRepository.findById(pno);
        Product product = result.orElseThrow();
        return entityToDTO(product);
    }

    // 4. 수정
    @Override
    public void modify(ProductDTO productDTO) {
        Optional<Product> result = productRepository.findById(productDTO.getPno());
        Product product = result.orElseThrow();

        product.changeName(productDTO.getPname());
        product.changeDesc(productDTO.getPdesc());
        product.changePrice(productDTO.getPrice());
        product.changeCategory(productDTO.getCategory());
        product.changeStock(productDTO.getStockQuantity());

        product.clearList();
        List<String> uploadFileNames = productDTO.getUploadFileNames();
        if (uploadFileNames != null && !uploadFileNames.isEmpty()) {
            uploadFileNames.forEach(product::addImageString);
        }
        productRepository.save(product);
    }

    // 5. 삭제
    @Override
    public void remove(Long pno) {
        productRepository.deleteById(pno);
    }

    // 6. 순서 변경
    @Override
    public void changeOrder(List<Long> pnoList) {
        for (int i = 0; i < pnoList.size(); i++) {
            final int num = i; // ✨ [핵심] i를 'final' 변수에 담아서 고정시킴

            Long pno = pnoList.get(i);
            productRepository.findById(pno).ifPresent(product -> {
                product.changeOrd(num); // ✨ i 대신 num을 사용하면 에러 끝!
                productRepository.save(product);
            });
        }
    }

    // 변환 메서드들 (status 빨간 줄 해결)
    private ProductDTO entityToDTO(Product product) {
        ProductDTO productDTO = ProductDTO.builder()
                .pno(product.getPno())
                .pname(product.getPname())
                .pdesc(product.getPdesc())
                .price(product.getPrice())
                .category(product.getCategory())
                .stockQuantity(product.getStockQuantity())
                .delFlag(product.isDelFlag())
                .status(product.isStatus()) // ✨ 1, 2단계 적용하면 이제 인식됨!
                .build();

        // ... 이미지 처리 로직 (그대로 유지) ...
        List<String> fileNameList = new ArrayList<>();
        if (product.getImageList() != null && !product.getImageList().isEmpty()) {
            fileNameList = product.getImageList().stream()
                    .map(ProductImage::getFileName)
                    .collect(Collectors.toList());
        } else {
            fileNameList.add("default.jpg");
        }
        productDTO.setUploadFileNames(fileNameList);

        return productDTO;
    }

    private Product dtoToEntity(ProductDTO productDTO) {
        Product product = Product.builder()
                .pno(productDTO.getPno())
                .pname(productDTO.getPname())
                .pdesc(productDTO.getPdesc())
                .price(productDTO.getPrice())
                .category(productDTO.getCategory())
                .stockQuantity(productDTO.getStockQuantity())
                .delFlag(productDTO.isDelFlag())
                .status(productDTO.isStatus()) // ✨ 여기도 해결됨
                .build();

        // ... 이미지 처리 로직 (그대로 유지) ...
        List<String> uploadFileNames = productDTO.getUploadFileNames();
        if (uploadFileNames != null) {
            uploadFileNames.forEach(product::addImageString);
        }
        return product;
    }
}