package com.Team1_Back.controller;

import com.Team1_Back.dto.ExpenseDTO;
import com.Team1_Back.dto.ExpenseSubmitDTO;
import com.Team1_Back.dto.PageRequestDTO;
import com.Team1_Back.dto.PageResponseDTO;
import com.Team1_Back.dto.UserDTO;
import com.Team1_Back.service.ExpenseService;
import com.Team1_Back.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * 지출 내역 관리를 위한 REST API 컨트롤러
 * 
 * <p>지출 내역의 조회, 등록, 수정, 삭제, 제출 기능을 제공합니다.
 * 모든 엔드포인트는 인증이 필요하며, userId는 @AuthenticationPrincipal에서 자동으로 주입됩니다.
 * 
 * @author Team1
 * @since 1.0
 */
@RestController
@RequestMapping("/api/receipt/expenses")
@Log4j2
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;
    private final UserService userService;

    /**
     * 지출 내역 목록을 조회합니다.
     * 
     * <p>사용자별 지출 내역을 페이지네이션하여 반환합니다.
     * 승인 상태, 날짜 범위로 필터링이 가능합니다.
     * 
     * @param status 필터링할 승인 상태 (DRAFT, SUBMITTED, APPROVED, REJECTED, REQUEST_MORE_INFO)
     * @param startDate 조회 시작 날짜 (ISO DATE 형식, 선택)
     * @param endDate 조회 종료 날짜 (ISO DATE 형식, 선택)
     * @param pageRequestDTO 페이지네이션 정보 (페이지 번호, 크기, 정렬)
     * @param principal 인증된 사용자 정보
     * @return 페이지네이션된 지출 내역 목록
     * @throws RuntimeException principal이 null인 경우 (인증 실패)
     */
    @GetMapping("/list")
    public PageResponseDTO<ExpenseDTO> getList(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            PageRequestDTO pageRequestDTO,
            @AuthenticationPrincipal UserDTO principal) {

        // 인증 체크
        if (principal == null) {
            log.error("principal이 null입니다. 인증이 필요합니다.");
            throw new RuntimeException("인증이 필요합니다.");
        }

        Long userId = principal.getId();
//        log.info("지출 목록 조회 요청 - userId: " + userId + ", status: " + status + ", startDate: " + startDate + ", endDate: " + endDate);
//        PageResponseDTO<ExpenseDTO> response = expenseService.getList(userId, pageRequestDTO, status, startDate, endDate);
//        log.info("지출 목록 조회 결과 - 총 " + response.getContent().size() + "건");
//        return response;

        // 유진님 추가
        return expenseService.getList(userId, pageRequestDTO, status, startDate, endDate);
    }

    /**
     * 특정 지출 내역의 상세 정보를 조회합니다.
     * 
     * <p>일반 사용자는 본인이 작성한 지출 내역만 조회할 수 있으며,
     * 관리자는 모든 지출 내역을 조회할 수 있습니다.
     * 
     * @param id 조회할 지출 내역 ID
     * @param principal 인증된 사용자 정보
     * @return 지출 내역 상세 정보
     * @throws RuntimeException principal이 null인 경우 (인증 실패)
     * @throws NoSuchElementException 해당 ID의 지출 내역이 없거나 권한이 없는 경우
     */
    @GetMapping("/{id}")
    public ExpenseDTO get(@PathVariable(name="id") Long id, @AuthenticationPrincipal UserDTO principal) {
        // 인증 체크
        if (principal == null) {
            log.error("principal이 null입니다. 인증이 필요합니다.");
            throw new RuntimeException("인증이 필요합니다.");
        }
        
        Long userId = principal.getId();
        // 관리자 여부 확인 (관리자는 모든 지출 내역 조회 가능)
        boolean isAdmin = userService.isAdmin(userId);
        
        return expenseService.get(id, userId, isAdmin);
    }

    /**
     * 새로운 지출 내역을 등록합니다.
     * 
     * <p>등록된 지출 내역은 기본적으로 DRAFT 상태로 생성됩니다.
     * 
     * @param expenseDTO 등록할 지출 내역 정보
     * @param principal 인증된 사용자 정보
     * @return 등록된 지출 내역의 ID를 포함한 Map
     */
    @PostMapping("/")
    public Map<String, Long> register(@RequestBody ExpenseDTO expenseDTO, @AuthenticationPrincipal UserDTO principal) {
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        Long userId = principal.getId();
        Long id = expenseService.register(expenseDTO, userId);
        return Map.of("result", id);
    }

    /**
     * 지출 내역을 수정합니다.
     * 
     * <p>DRAFT 상태의 지출 내역만 수정 가능합니다.
     * 본인이 작성한 지출 내역만 수정할 수 있습니다.
     * 
     * @param id 수정할 지출 내역 ID
     * @param expenseDTO 수정할 지출 내역 정보
     * @param principal 인증된 사용자 정보
     * @return 수정 결과를 포함한 Map
     * @throws IllegalStateException DRAFT 상태가 아니거나 권한이 없는 경우
     */
    @PutMapping("/{id}")
    public Map<String, String> modify(
            @PathVariable(name="id") Long id,
            @RequestBody ExpenseDTO expenseDTO,
            @AuthenticationPrincipal UserDTO principal) {
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        Long userId = principal.getId();
        expenseDTO.setId(id);
        expenseService.modify(expenseDTO, userId);
        return Map.of("RESULT", "SUCCESS");
    }

    /**
     * 지출 내역을 삭제합니다.
     * 
     * <p>DRAFT 상태의 지출 내역만 삭제 가능합니다.
     * 본인이 작성한 지출 내역만 삭제할 수 있습니다.
     * 
     * @param id 삭제할 지출 내역 ID
     * @param principal 인증된 사용자 정보
     * @return 삭제 결과를 포함한 Map
     * @throws IllegalStateException DRAFT 상태가 아니거나 권한이 없는 경우
     */
    @DeleteMapping("/{id}")
    public Map<String, String> remove(@PathVariable(name="id") Long id, @AuthenticationPrincipal UserDTO principal) {
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        Long userId = principal.getId();
        expenseService.remove(id, userId);
        return Map.of("RESULT", "SUCCESS");
    }

    /**
     * 지출 내역을 승인 요청 상태로 제출합니다.
     * 
     * <p>DRAFT 상태의 지출 내역을 SUBMITTED 상태로 변경합니다.
     * 제출 후에는 수정 및 삭제가 불가능합니다.
     * 
     * @param id 제출할 지출 내역 ID
     * @param submitDTO 제출 시 추가 정보 (선택, null 가능)
     * @param principal 인증된 사용자 정보
     * @return 제출 결과를 포함한 Map
     * @throws IllegalStateException DRAFT 상태가 아니거나 권한이 없는 경우
     */
    @PostMapping("/{id}/submit")
    public Map<String, String> submit(
            @PathVariable(name="id") Long id,
            @RequestBody(required = false) ExpenseSubmitDTO submitDTO,
            @AuthenticationPrincipal UserDTO principal) {
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        Long userId = principal.getId();
        if (submitDTO == null) {
            submitDTO = new ExpenseSubmitDTO();
        }
        expenseService.submit(id, userId, submitDTO);
        return Map.of("RESULT", "SUCCESS");
    }
}

