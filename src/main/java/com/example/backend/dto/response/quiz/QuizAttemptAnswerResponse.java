package com.example.backend.dto.response.quiz;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.backend.constant.GradingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizAttemptAnswerResponse {
    private Integer id;
    @JsonProperty("isCorrect")
    private Boolean isCorrect;
    private BigDecimal maxPoints;
    private BigDecimal earnedPoints;
    private GradingStatus gradingStatus;
    private String teacherFeedback;
    private LocalDateTime reviewedAt;
    private QuizQuestionResponse quizQuestion;
    private List<QuizAnswerResponse> selectedAnswers;
    private String textAnswer;
    private List<QuizAttemptAnswerItemResponse> answerItems;
}
