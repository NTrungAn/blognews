package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.response.NotificationResponse;
import com.blog.blogsystem.dto.response.PageResponse;
import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.entity.enums.NotificationType;

import java.util.UUID;

public interface NotificationService {
    void createNotification(User recipient, User actor, NotificationType type, String content, String targetUrl);
    PageResponse<NotificationResponse> getMyNotifications(String username, int pageNo, int pageSize);
    void markAsRead(UUID notificationId, String username);
    void markAllAsRead(String username);
    long getUnreadCount(String username);
    Object getAllNotificationsForDebug();
}
