package com.example.backend.service;

import com.example.backend.dto.response.NotificationResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {
    void createNotification(User recipient, String title, String message);

    void createNotification(User recipient, String title, String message, String type, String description, String actionUrl);

    int countUnread();

    List<NotificationResponse> getMyNotifications();

    PageResponse<NotificationResponse> getMyNotificationsPage(Pageable pageable);

    void markAsRead(Integer notificationId);

    void deleteNotification(Integer notificationId);
}
