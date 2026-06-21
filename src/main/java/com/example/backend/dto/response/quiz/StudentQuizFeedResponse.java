package com.example.backend.dto.response.quiz;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Một mục quiz trong "feed" của học viên (dùng cho trang tổng quan / việc cần làm).
 * Tương đương StudentAssignmentFeedResponse nhưng cho quiz.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentQuizFeedResponse {
    private Integer quizId;
    private String quizTitle;
    private Integer classContentItemId;
    private Integer classSectionId;
    private String classSectionTitle;
    private LocalDateTime availableFrom;
    private LocalDateTime availableUntil; // hạn làm bài (tương đương dueAt)
    private Integer maxAttempts;          // null = không giới hạn
    private Integer attemptsUsed;
    private Integer minPassScore;
    private Integer bestGrade;            // null nếu chưa làm
    private Boolean passed;
    private boolean needsReview;          // có lần làm chờ giáo viên chấm tay
    private boolean inProgress;           // có lần làm đang dở
    private boolean completed;            // không còn việc phải làm (đạt, hoặc hết lượt)
    private boolean pastDue;              // quá hạn mà chưa hoàn thành
    private boolean upcoming;             // còn làm được
}
