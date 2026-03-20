package com.example.backend.dto.response.quiz;

import lombok.*;

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
    private Integer chapterItemId;
    private Long remainingTimeSeconds;
}
