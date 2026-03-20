package com.example.backend.service;

import com.example.backend.dto.request.CommentRequest;
import com.example.backend.dto.response.CommentResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.entity.Comment;
import com.example.backend.exception.UnauthorizedException;
import org.springframework.data.domain.Pageable;

public interface CommentService {

    CommentResponse addComment(Integer lessonId, CommentRequest request);

    CommentResponse updateComment(Integer id, CommentRequest request);

    PageResponse<CommentResponse> getCommentsByLesson(Integer lessonId, Pageable pageable);

    CommentResponse getComment(Integer id);

    void deleteComment(Integer id);

    CommentResponse convertEntityToDTO(Comment comment);
}
