package com.example.backend.dto.request.quiz;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizAttemptReviewRequest {
    private String instructorFeedback;
    private List<AnswerReview> answers;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerReview {
        private Integer answerId;
        private BigDecimal score;
        private String feedback;
    }
}
