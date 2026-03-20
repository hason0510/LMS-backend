package com.example.backend.controller;

import com.example.backend.dto.request.quiz.QuizAttemptAnswerRequest;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.quiz.CourseQuizResultResponse;
import com.example.backend.dto.response.quiz.QuizAttemptDetailResponse;
import com.example.backend.dto.response.quiz.QuizAttemptResponse;
import com.example.backend.dto.response.quiz.StudentQuizResultResponse;
import com.example.backend.service.QuizAttemptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;  // ← ADD THIS IMPORT
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lms")
@RequiredArgsConstructor
@Tag(name = "Quiz Attempt Management", description = "APIs for managing quiz attempts, answers, and submissions")  // ← ADD THIS LINE
public class QuizAttemptController {

    private final QuizAttemptService quizAttemptService;

    @Operation(summary = "Bắt đầu làm quiz", description = "Allows students to start attempting a quiz. Creates a new quiz attempt record.")  // ← ENHANCED DESCRIPTION
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/chapterItem/{chapterItemId}/quiz/{quizId}/start")
    public ResponseEntity<QuizAttemptDetailResponse> startQuizAttempt(
            @PathVariable Integer quizId, @PathVariable Integer chapterItemId
    ) {
        return ResponseEntity.ok(quizAttemptService.startQuizAttempt(quizId, chapterItemId));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/quiz-attempts/{attemptId}")
    public ResponseEntity<QuizAttemptDetailResponse> getAttemptDetail(
            @PathVariable Integer attemptId
    ) {
        return ResponseEntity.ok(
                quizAttemptService.getAttemptDetail(attemptId)
        );
    }

    @Operation(
            summary = "Lấy bài quiz đang làm",
            description = "Return current IN_PROGRESS quiz attempt for the logged-in student"
    )
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/chapterItem/{chapterItemId}/quiz/current")
    public ResponseEntity<QuizAttemptDetailResponse> getCurrentAttempt(
            @PathVariable Integer chapterItemId
    ) {
        return ResponseEntity.ok(
                quizAttemptService.getCurrentAttempt(chapterItemId)
        );
    }

    @Operation(summary = "Trả lời một câu hỏi trong quiz", description = "Submit an answer for a specific question within an ongoing quiz attempt.")  // ← ENHANCED DESCRIPTION
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/quiz-attempts/{attemptId}/question/{questionId}/answer")
    public ResponseEntity<Void> answerQuestion(
            @PathVariable Integer attemptId,
            @PathVariable Integer questionId,
            @RequestBody QuizAttemptAnswerRequest request
    ) {
        quizAttemptService.answerQuestion(attemptId,questionId,request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Nộp bài quiz", description = "Finalize and submit a quiz attempt. Calculates final score and marks attempt as completed.")  // ← ENHANCED DESCRIPTION
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/quiz-attempts/{attemptId}/submit")
    public ResponseEntity<QuizAttemptResponse> submitQuiz(
            @PathVariable Integer attemptId
    ) {
        return ResponseEntity.ok(
                quizAttemptService.submitQuiz(attemptId)
        );
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/chapterItem/{chapterItemId}")
    public ResponseEntity<Integer> getStudentBestScoreOnQuizAttempt(
            @PathVariable Integer chapterItemId
    ) {
        return ResponseEntity.ok(
                quizAttemptService.getStudentBestScore(chapterItemId)
        );
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/chapterItem/{chapterItemId}/my-attempts")
    public ResponseEntity<List<QuizAttemptResponse>> getStudentAttemptsHistory(
            @PathVariable Integer chapterItemId
    ) {
        return ResponseEntity.ok(
                quizAttemptService.getStudentAttemptsHistory(chapterItemId)
        );
    }

    @Operation(
            summary = "Danh sách bài làm của học viên",
            description = "Teacher/Admin xem danh sách bài làm của quiz"
    )
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @GetMapping("/chapterItem/{chapterItemId}/attempts")
    public ResponseEntity<PageResponse<QuizAttemptResponse>> getAttemptsForTeacherOrAdmin(
            @PathVariable Integer chapterItemId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                quizAttemptService.getAttemptsForTeacherOrAdmin(chapterItemId, pageable)
        );
    }

    // =========================================================================
    // API BẢNG ĐIỂM (GRADE BOOK)
    // =========================================================================

    @Operation(summary = "Xem bảng điểm cá nhân trong 1 khóa học")
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/courses/{courseId}/my-grades") // Đổi endpoint để kèm courseId
    public ResponseEntity<List<StudentQuizResultResponse>> getMyGradeBook(
            @PathVariable Integer courseId
    ) {
        return ResponseEntity.ok(quizAttemptService.getMyGradeBook(courseId));
    }
    @Operation(
            summary = "Xem bảng điểm khóa học (Giáo viên/Admin)",
            description = "Xem tổng hợp kết quả làm bài Quiz của tất cả sinh viên trong một khóa học cụ thể."
    )
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @GetMapping("/courses/{courseId}/quiz-grades")
    public ResponseEntity<List<CourseQuizResultResponse>> getCourseGradeBook(
            @PathVariable Integer courseId
    ) {
        return ResponseEntity.ok(quizAttemptService.getCourseGradeBook(courseId));
    }
}