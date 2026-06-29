package com.blog.blogsystem.service.impl;

import com.blog.blogsystem.dto.response.NotificationResponse;
import com.blog.blogsystem.dto.response.PageResponse;
import com.blog.blogsystem.entity.Notification;
import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.entity.enums.NotificationType;
import com.blog.blogsystem.mapper.UserMapper;
import com.blog.blogsystem.repository.NotificationRepository;
import com.blog.blogsystem.repository.UserRepository;
import com.blog.blogsystem.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
// Force VSCode to recompile this file
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public void createNotification(User recipient, User actor, NotificationType type, String content, String targetUrl) {
        // Tránh tự thông báo cho chính mình (ví dụ tự thả tim bài của mình)
        if (actor != null && actor.getId().equals(recipient.getId())) {
            return;
        }

        Notification notification = Notification.builder()
                .recipient(recipient)
                .actor(actor)
                .type(type)
                .content(content)
                .targetUrl(targetUrl)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getMyNotifications(String username, int pageNo, int pageSize) {
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        Page<Notification> notifications = notificationRepository.findByRecipient_UsernameOrderByCreatedAtDesc(username, pageable);

        List<NotificationResponse> content = notifications.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<NotificationResponse>builder()
                .content(content)
                .pageNo(notifications.getNumber())
                .pageSize(notifications.getSize())
                .totalElements(notifications.getTotalElements())
                .totalPages(notifications.getTotalPages())
                .last(notifications.isLast())
                .build();
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId, String username) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));

        if (!notification.getRecipient().getUsername().equals(username)) {
            throw new RuntimeException("Bạn không có quyền đánh dấu thông báo này");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(String username) {
        List<Notification> unreadList = notificationRepository.findByRecipient_UsernameOrderByCreatedAtDesc(username, Pageable.unpaged())
                .getContent().stream()
                .filter(n -> !n.isRead())
                .collect(Collectors.toList());

        unreadList.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unreadList);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String username) {
        return notificationRepository.countByRecipient_UsernameAndIsReadFalse(username);
    }

    @Override
    @Transactional(readOnly = true)
    public Object getAllNotificationsForDebug() {
        return notificationRepository.findAll();
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType().name())
                .content(notification.getContent())
                .targetUrl(notification.getTargetUrl())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .actor(notification.getActor() != null ? userMapper.toResponse(notification.getActor()) : null)
                .build();
    }
}
