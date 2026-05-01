package com.example.backend.dto.response.quiz;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizAttemptAnswerItemResponse {
    private Integer id;
    private Integer itemId;
    private Integer selectedItemId;
    private String answerText;
    private Integer submittedOrderIndex;
    private Integer blankIndex;
    @JsonProperty("isCorrect")
    private Boolean isCorrect;
}
