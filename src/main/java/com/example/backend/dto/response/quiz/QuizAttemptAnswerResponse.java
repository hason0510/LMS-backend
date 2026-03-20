package com.example.backend.dto.response.quiz;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

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
    private QuizQuestionResponse quizQuestion;
    private List<QuizAnswerResponse> selectedAnswers;
    private String textAnswer;

}
