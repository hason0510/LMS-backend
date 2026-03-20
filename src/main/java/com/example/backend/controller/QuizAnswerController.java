package com.example.backend.controller;
import com.example.backend.dto.request.quiz.QuizAnswerRequest;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.quiz.QuizAnswerResponse;
import com.example.backend.service.QuizAnswerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;  // ← ADD THIS IMPORT
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/lms/quiz-answers")
@Tag(name = "Quiz Answer Management", description = "APIs for managing quiz answers and answer options")  // ← ADD THIS LINE
public class QuizAnswerController {

    private final QuizAnswerService quizAnswerService;
    public QuizAnswerController(QuizAnswerService quizAnswerService) {
        this.quizAnswerService = quizAnswerService;
    }

    @Operation(summary = "Tạo câu trả lời cho câu hỏi quiz", description = "Create a new answer option for a quiz question")  // ← ENHANCED
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/question/{questionId}")
    public ResponseEntity<QuizAnswerResponse> createQuizAnswer(
            @PathVariable Integer questionId,
            @RequestBody QuizAnswerRequest request) {
        return ResponseEntity.ok(quizAnswerService.createQuizAnswer(questionId, request));
    }

    @Operation(summary = "Lấy thông tin câu trả lời quiz theo ID", description = "Retrieve specific quiz answer details by ID")  // ← ENHANCED
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'TEACHER')")
    @GetMapping("/{id}")
    public ResponseEntity<QuizAnswerResponse> getQuizAnswerById(@PathVariable Integer id) {
        return ResponseEntity.ok(quizAnswerService.getQuizAnswerById(id));
    }

    @Operation(summary = "Lấy danh sách câu trả lời quiz có phân trang", description = "Retrieve paginated list of all quiz answers with filtering options")  // ← ENHANCED
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'TEACHER')")
    @GetMapping
    public ResponseEntity<PageResponse<QuizAnswerResponse>> getAllQuizAnswers(
            @RequestParam(value = "pageNumber", defaultValue = "1", required = false) Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "10", required = false) Integer pageSize
    ) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        return ResponseEntity.ok(quizAnswerService.getQuizAnswerPage(pageable));
    }

    @Operation(summary = "Cập nhật câu trả lời quiz", description = "Update an existing quiz answer option")  // ← ENHANCED
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PutMapping("/{id}")
    public ResponseEntity<QuizAnswerResponse> updateQuizAnswer(
            @PathVariable Integer id,
            @RequestBody QuizAnswerRequest request
    ) {
        return ResponseEntity.ok(quizAnswerService.updateQuizAnswer(id, request));
    }

    @Operation(summary = "Xóa câu trả lời quiz", description = "Delete a quiz answer option by ID")  // ← ENHANCED
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuizAnswer(@PathVariable Integer id) {
        quizAnswerService.deleteQuizAnswer(id);
        return ResponseEntity.noContent().build();
    }
}