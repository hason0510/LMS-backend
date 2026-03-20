package com.example.backend.controller;

import com.example.backend.dto.response.NotificationResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lms/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/unread/count")
    public ResponseEntity<Integer> countUnread() {
        return ResponseEntity.ok(notificationService.countUnread());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications() {
        return ResponseEntity.ok(notificationService.getMyNotifications());
    }

    @Operation(summary = "Đánh dấu thông báo là đã đọc")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Integer id) {
        notificationService.markAsRead(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Lấy danh sách thông báo của người dùng hiện tại (có phân trang)")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/page")
    public ResponseEntity<PageResponse<NotificationResponse>> getMyNotificationsPage(
            @RequestParam(value = "pageNumber", defaultValue = "1", required = false) Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "10", required = false) Integer pageSize
    ) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        PageResponse<NotificationResponse> response =
                notificationService.getMyNotificationsPage(pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Xóa thông báo của người dùng hiện tại")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable Integer id
    ) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }
}
