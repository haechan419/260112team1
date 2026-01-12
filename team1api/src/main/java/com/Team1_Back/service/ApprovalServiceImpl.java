package com.Team1_Back.service;

import com.Team1_Back.domain.*;
import com.Team1_Back.dto.*;
import com.Team1_Back.repository.ApprovalActionLogRepository;
import com.Team1_Back.repository.ApprovalRequestRepository;
import com.Team1_Back.repository.ExpenseRepository;
import com.Team1_Back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalServiceImpl implements ApprovalService {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalActionLogRepository approvalActionLogRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final ExpenseService expenseService;
    private final ModelMapper modelMapper;

    @Override
    public PageResponseDTO<ApprovalRequestDTO> getList(Long userId, boolean isAdmin, PageRequestDTO pageRequestDTO, String requestType, String status, LocalDate startDate, LocalDate endDate) {
        Pageable pageable = PageRequest.of(pageRequestDTO.getPage() - 1, pageRequestDTO.getSize());

        String statusString = null;
        if (status != null && !status.isEmpty()) {
            try {
                ApprovalStatus.valueOf(status);
                statusString = status;
            } catch (IllegalArgumentException e) {
                // Invalid status value ignored
            }
        }

        Page<ApprovalRequest> result;
        if (isAdmin) {
            boolean hasStartDate = startDate != null;
            boolean hasEndDate = endDate != null;
            boolean hasDateFilter = hasStartDate || hasEndDate;

            if (hasDateFilter) {
                if (requestType != null && !requestType.isEmpty() && statusString != null) {
                    if (hasStartDate && hasEndDate) {
                        result = approvalRequestRepository.findByRequestTypeAndStatusSnapshotAndDateRange(requestType, statusString, startDate, endDate, pageable);
                    } else if (hasStartDate) {
                        result = approvalRequestRepository.findByRequestTypeAndStatusSnapshotAndStartDate(requestType, statusString, startDate, pageable);
                    } else {
                        result = approvalRequestRepository.findByRequestTypeAndStatusSnapshotAndEndDate(requestType, statusString, endDate, pageable);
                    }
                } else if (requestType != null && !requestType.isEmpty()) {
                    if (hasStartDate && hasEndDate) {
                        result = approvalRequestRepository.findByRequestTypeAndDateRange(requestType, startDate, endDate, pageable);
                    } else if (hasStartDate) {
                        result = approvalRequestRepository.findByRequestTypeAndStartDate(requestType, startDate, pageable);
                    } else {
                        result = approvalRequestRepository.findByRequestTypeAndEndDate(requestType, endDate, pageable);
                    }
                } else if (statusString != null) {
                    if (hasStartDate && hasEndDate) {
                        result = approvalRequestRepository.findByStatusSnapshotAndDateRange(statusString, startDate, endDate, pageable);
                    } else if (hasStartDate) {
                        result = approvalRequestRepository.findByStatusSnapshotAndStartDate(statusString, startDate, pageable);
                    } else {
                        result = approvalRequestRepository.findByStatusSnapshotAndEndDate(statusString, endDate, pageable);
                    }
                } else {
                    if (hasStartDate && hasEndDate) {
                        result = approvalRequestRepository.findAllByDateRange(startDate, endDate, pageable);
                    } else if (hasStartDate) {
                        result = approvalRequestRepository.findAllByStartDate(startDate, pageable);
                    } else {
                        result = approvalRequestRepository.findAllByEndDate(endDate, pageable);
                    }
                }
            } else {
                if (requestType != null && !requestType.isEmpty() && statusString != null) {
                    result = approvalRequestRepository.findByRequestTypeAndStatusSnapshotOrderByReceiptDate(requestType, statusString, pageable);
                } else if (requestType != null && !requestType.isEmpty()) {
                    result = approvalRequestRepository.findByRequestTypeOrderByReceiptDate(requestType, pageable);
                } else if (statusString != null) {
                    result = approvalRequestRepository.findByStatusSnapshotOrderByReceiptDate(statusString, pageable);
                } else {
                    result = approvalRequestRepository.findAllOrderByReceiptDate(pageable);
                }
            }
        } else {
            if (statusString != null) {
                result = approvalRequestRepository.findByRequesterIdAndStatusSnapshotOrderByReceiptDate(userId, statusString, pageable);
            } else {
                result = approvalRequestRepository.findByRequesterIdOrderByReceiptDate(userId, pageable);
            }
        }

        List<ApprovalRequestDTO> dtoList = result.getContent().stream()
                .map(this::entityToDTO)
                .collect(Collectors.toList());

        long totalCount = result.getTotalElements();

        if ("EXPENSE".equals(requestType) || requestType == null) {
            List<Long> refIds = dtoList.stream()
                    .filter(dto -> "EXPENSE".equals(dto.getRequestType()) && dto.getRefId() != null)
                    .map(ApprovalRequestDTO::getRefId)
                    .distinct()
                    .collect(Collectors.toList());

            if (!refIds.isEmpty()) {
                java.util.Map<Long, ExpenseDTO> expenseMap = expenseService.getByIds(refIds);

                dtoList.forEach(dto -> {
                    if ("EXPENSE".equals(dto.getRequestType()) && dto.getRefId() != null) {
                        dto.setExpense(expenseMap.get(dto.getRefId()));
                    }
                });
            }
        }
        return PageResponseDTO.of(
                dtoList,
                pageRequestDTO,
                totalCount
        );
    }

    @Override
    public ApprovalRequestDTO get(Long id, Long userId, boolean isAdmin) {
        // requester, approver를 함께 로드 (LAZY 로딩 방지)
        ApprovalRequest approvalRequest = approvalRequestRepository.findByIdWithRelations(id)
                .orElseThrow();

        // 권한 확인
        if (!isAdmin && !approvalRequest.getRequester().getId().equals(userId)) {
            throw new RuntimeException("권한이 없습니다.");
        }

        return entityToDTO(approvalRequest);
    }

    @Override
    public List<ApprovalActionLogDTO> getLogs(Long id, Long userId, boolean isAdmin) {
        // requester, approver를 함께 로드 (LAZY 로딩 방지)
        ApprovalRequest approvalRequest = approvalRequestRepository.findByIdWithRelations(id)
                .orElseThrow();

        // 권한 확인
        if (!isAdmin && !approvalRequest.getRequester().getId().equals(userId)) {
            throw new RuntimeException("권한이 없습니다.");
        }

        List<ApprovalActionLog> logs = approvalActionLogRepository.findByApprovalRequestIdOrderByCreatedAtAsc(id);

        return logs.stream()
                .map(this::logEntityToDTO)
                .collect(Collectors.toList());
    }

    /**
     * ApprovalRequest 엔티티를 ApprovalRequestDTO로 변환
     *
     * @param entity 변환할 ApprovalRequest 엔티티
     * @return ApprovalRequestDTO (entity가 null이면 null 반환)
     */
    private ApprovalRequestDTO entityToDTO(ApprovalRequest entity) {
        if (entity == null) return null;

        return ApprovalRequestDTO.builder()
                .id(entity.getId())
                .requestType(entity.getRequestType())
                .refId(entity.getRefId())
                .requesterId(entity.getRequester() != null ? entity.getRequester().getId() : null)
                .requesterName(entity.getRequester() != null ? entity.getRequester().getName() : null)
                .approverId(entity.getApprover() != null ? entity.getApprover().getId() : null)
                .approverName(entity.getApprover() != null ? entity.getApprover().getName() : null)
                .statusSnapshot(entity.getStatusSnapshot() != null ? entity.getStatusSnapshot().name() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public ApprovalRequestDTO action(Long id, ApprovalActionDTO actionDTO, Long adminId) {
        try {
            if (id == null) {
                log.error("결재 요청 ID가 null입니다.");
                throw new RuntimeException("결재 요청 ID가 필요합니다.");
            }

            if (actionDTO == null) {
                log.error("ApprovalActionDTO가 null입니다.");
                throw new RuntimeException("결재 처리 정보가 필요합니다.");
            }

            ApprovalRequest approvalRequest = approvalRequestRepository.findByIdWithRelations(id)
                    .orElseThrow();

            User admin = userRepository.findById(adminId)
                    .orElseThrow();

            if (!admin.isAdmin()) {
                log.warn("관리자 권한이 없습니다. adminId: {}, isAdmin: {}", adminId, admin.isAdmin());
                throw new RuntimeException("관리자 권한이 필요합니다.");
            }

            String action = actionDTO.getAction();
            String message = actionDTO.getMessage();

            if ("EXPENSE".equals(approvalRequest.getRequestType())) {
                Expense expense = expenseRepository.findByIdWithWriter(approvalRequest.getRefId())
                        .orElseThrow();

                if ("APPROVE".equals(action)) {
                    expense.approve();
                } else if ("REJECT".equals(action)) {
                    expense.reject(message);
                } else if ("REQUEST_MORE_INFO".equals(action)) {
                    expense.requestMoreInfo(message);
                } else {
                    log.error("지원하지 않는 액션입니다. action: {}", action);
                    throw new RuntimeException("지원하지 않는 액션입니다: " + action);
                }

                expenseRepository.save(expense);

                approvalRequest.syncStatusSnapshot(expense.getStatus());
                approvalRequestRepository.save(approvalRequest);

                ApprovalActionLog actionLog = ApprovalActionLog.builder()
                        .approvalRequest(approvalRequest)
                        .actor(admin)
                        .action(action)
                        .message(message)
                        .build();

                approvalActionLogRepository.save(actionLog);

            } else {
                log.error("지원하지 않는 요청 타입입니다. requestType: {}", approvalRequest.getRequestType());
                throw new RuntimeException("지원하지 않는 요청 타입입니다: " + approvalRequest.getRequestType());
            }

            return entityToDTO(approvalRequest);

        } catch (RuntimeException e) {
            log.error("결재 처리 실패 - id: {}, error: {}", id, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("결재 처리 중 예상치 못한 오류 발생 - id: {}", id, e);
            throw new RuntimeException("결재 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * ApprovalActionLog 엔티티를 ApprovalActionLogDTO로 변환
     *
     * @param entity 변환할 ApprovalActionLog 엔티티
     * @return ApprovalActionLogDTO (entity가 null이면 null 반환)
     */
    private ApprovalActionLogDTO logEntityToDTO(ApprovalActionLog entity) {
        if (entity == null) {
            return null;
        }

        ApprovalActionLogDTO dto = modelMapper.map(entity, ApprovalActionLogDTO.class);

        if (entity.getApprovalRequest() != null) {
            dto.setApprovalRequestId(entity.getApprovalRequest().getId());
        }

        if (entity.getActor() != null) {
            dto.setActorId(entity.getActor().getId());
            dto.setActorName(entity.getActor().getName());
        }

        return dto;
    }
}

