package com.example.backend.controller;

import com.example.backend.dto.request.CommentRequest;
import com.example.backend.dto.response.CommentResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/lms")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @Operation(summary = "Thêm bình luận vào bài học")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/lessons/{lessonId}/comments")
    public ResponseEntity<CommentResponse> addComment(@PathVariable Integer lessonId, @RequestBody CommentRequest request) {
        CommentResponse response = commentService.addComment(lessonId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Cập nhật bình luận")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/comments/{id}")
    public ResponseEntity<CommentResponse> updateComment(@PathVariable Integer id, @RequestBody CommentRequest request) {
        CommentResponse response = commentService.updateComment(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Xóa bình luận")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Integer id) {
        commentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Lấy thông tin bình luận theo ID")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/comments/{id}")
    public ResponseEntity<CommentResponse> getCommentById(@PathVariable Integer id) {
        CommentResponse response = commentService.getComment(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Lấy danh sách bình luận theo bài học")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/lessons/{lessonId}/comments")
    public ResponseEntity<PageResponse<CommentResponse>> getCommentsByLesson(
            @PathVariable Integer lessonId,
            @RequestParam(value = "pageNumber", defaultValue = "1") Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize
    ) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        PageResponse<CommentResponse> response =
                commentService.getCommentsByLesson(lessonId, pageable);
        return ResponseEntity.ok(response);
    }
}