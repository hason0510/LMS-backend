package com.example.backend.dto.request.quiz;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizAttemptAnswerItemRequest {
    private Integer itemId;
    private Integer selectedItemId;
    private String answerText;
    private Integer submittedOrderIndex;
    private Integer blankIndex;
}
