package com.Team1_Back.service;

import com.Team1_Back.domain.Notification;
import com.Team1_Back.dto.NotificationDTO;
import com.Team1_Back.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // 1. 알림 보내기 (결재 승인 시 사용)
    public void send(String receiver, String message) {
        Notification notification = Notification.builder()
                .receiver(receiver)
                .message(message)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
    }

    // 2. 내 알림 가져오기 (컨트롤러 에러 해결! ✨)
    public List<NotificationDTO> getMyNotifications(String mid) {
        List<Notification> list = notificationRepository.findByReceiverAndIsReadFalseOrderByNnoDesc(mid);
        
        return list.stream().map(n -> NotificationDTO.builder()
                .nno(n.getNno())
                .message(n.getMessage())
                .isRead(n.isRead())
                .regDate(n.getRegDate())
                .build()).collect(Collectors.toList());
    }

    // 3. 읽음 처리 (컨트롤러 에러 해결! ✨)
    public void read(Long nno) {
        notificationRepository.findById(nno).ifPresent(notification -> {
            notification.changeRead(true);
            notificationRepository.save(notification);
        });
    }
}