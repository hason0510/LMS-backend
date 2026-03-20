package com.example.backend.service.impl;

import com.example.backend.dto.response.NotificationResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.entity.Notification;
import com.example.backend.entity.User;
import com.example.backend.repository.NotificationRepository;
import com.example.backend.service.NotificationService;
import com.example.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserService userService;

    @Override
    public void createNotification(User recipient, String title, String message) {
        createNotification(recipient, title, message, "SYSTEM", null, null);
    }

    @Override
    public void createNotification(User recipient, String title, String message, String type, String description, String actionUrl) {
        Notification notification = Notification.builder()
                .recipient(recipient)
                .title(title)
                .message(message)
                .description(description)
                .type(type)
                .actionUrl(actionUrl)
                .readStatus(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
    }

    @Override
    public int countUnread() {
        User currentUser = userService.getCurrentUser();
        return notificationRepository.countByRecipient_IdAndReadStatusFalse(currentUser.getId());
    }

    @Override
    public List<NotificationResponse> getMyNotifications() {
        User currentUser = userService.getCurrentUser();
        return notificationRepository.findByRecipient_IdOrderByCreatedAtDesc(currentUser.getId())
                .stream().map(this::convertEntityToDTO).collect(Collectors.toList());
    }

    @Override
    public void markAsRead(Integer notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification != null) {
            notification.setReadStatus(true);
            notificationRepository.save(notification);
        }
    }

    @Override
    public PageResponse<NotificationResponse> getMyNotificationsPage(Pageable pageable) {
        User currentUser = userService.getCurrentUser();
        Page<Notification> notificationPage =
                notificationRepository.findByRecipient_IdOrderByCreatedAtDesc(
                        currentUser.getId(),
                        pageable
                );
        Page<NotificationResponse> responsePage =
                notificationPage.map(this::convertEntityToDTO);
        return new PageResponse<>(
                responsePage.getNumber() + 1,
                responsePage.getTotalPages(),
                responsePage.getNumberOfElements(),
                responsePage.getContent()
        );
    }

    @Override
    public void deleteNotification(Integer id) {
        User currentUser = userService.getCurrentUser();

        Notification notification = notificationRepository
                .findByIdAndRecipient_Id(id, currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Không có quyền xóa thông báo này"));

        notificationRepository.delete(notification);
    }

    private NotificationResponse convertEntityToDTO(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setTitle(notification.getTitle());
        response.setMessage(notification.getMessage());
        response.setDescription(notification.getDescription());
        response.setType(notification.getType());
        response.setActionUrl(notification.getActionUrl());
        response.setReadStatus(notification.isReadStatus());
        response.setCreatedAt(notification.getCreatedAt());
        return response;
    }

}
