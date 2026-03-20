package com.example.backend.controller;

import com.example.backend.dto.request.quiz.QuizQuestionRequest;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.quiz.QuizQuestionResponse;
import com.example.backend.service.QuizQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;  // ← ADD THIS IMPORT
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/lms/quiz-questions")
@Tag(name = "Quiz Question Management", description = "APIs for managing quiz questions and question configurations")  // ← ADD THIS LINE
public class QuizQuestionController {
    private final QuizQuestionService quizQuestionService;

    public QuizQuestionController(QuizQuestionService quizQuestionService) {
        this.quizQuestionService = quizQuestionService;
    }

    @Operation(summary = "Tạo câu hỏi quiz mới", description = "Create a new question for a quiz with answer options")  // ← ENHANCED
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/quiz/{quizId}")
    public ResponseEntity<QuizQuestionResponse> createQuizQuestion(
            @PathVariable Integer quizId,
            @RequestBody QuizQuestionRequest request) {
        return ResponseEntity.ok(quizQuestionService.createQuizQuestion(quizId, request));
    }

    @Operation(summary = "Lấy thông tin câu hỏi quiz theo ID", description = "Retrieve detailed information about a specific quiz question by ID")  // ← ENHANCED
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'TEACHER')")
    @GetMapping("/{id}")
    public ResponseEntity<QuizQuestionResponse> getQuizQuestionById(@PathVariable Integer id) {
        return ResponseEntity.ok(quizQuestionService.getQuizQuestionById(id));
    }

    @Operation(summary = "Lấy danh sách câu hỏi quiz có phân trang", description = "Retrieve paginated list of all quiz questions with filtering options")  // ← ENHANCED
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'TEACHER')")
    @GetMapping
    public ResponseEntity<PageResponse<QuizQuestionResponse>> getAllQuizQuestions(
            @RequestParam(value = "pageNumber", defaultValue = "1", required = false) Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "10", required = false) Integer pageSize
    ) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        return ResponseEntity.ok(quizQuestionService.getQuizQuestionPage(pageable));
    }

    @Operation(summary = "Cập nhật câu hỏi quiz", description = "Update an existing quiz question and its answer options")  // ← ENHANCED
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PutMapping("/{id}")
    public ResponseEntity<QuizQuestionResponse> updateQuizQuestion(
            @PathVariable Integer id,
            @RequestBody QuizQuestionRequest request
    ) {
        return ResponseEntity.ok(quizQuestionService.updateQuizQuestion(id, request));
    }

    @Operation(summary = "Xóa câu hỏi quiz", description = "Delete a quiz question and all associated answer options")  // ← ENHANCED
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuizQuestion(@PathVariable Integer id) {
        quizQuestionService.deleteQuizQuestion(id);
        return ResponseEntity.noContent().build();
    }
}