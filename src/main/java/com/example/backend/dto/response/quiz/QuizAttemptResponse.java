package com.example.backend.dto.response.quiz;

import lombok.*;

import com.example.backend.constant.GradingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizAttemptResponse {
    private Integer id;
    private LocalDateTime startTime;
    private LocalDateTime completedTime;
    private Integer grade;
    private Boolean isPassed;
    private Integer quizId;
    private Integer studentId;
    private Integer totalQuestions;
    private Integer correctAnswers;
    private Integer incorrectAnswers;
    private Integer unansweredQuestions;
    private BigDecimal earnedPoints;
    private BigDecimal totalPoints;
    private GradingStatus gradingStatus;
    private String instructorFeedback;
    private Integer classContentItemId;
    private Integer classSectionId;
    private String quizTitle;
    private String studentName;
    private String studentEmail;
    private String classSectionTitle;
    private Long remainingTimeSeconds;
}
