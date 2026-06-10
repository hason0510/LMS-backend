package com.example.backend.dto.request.quiz;

import com.example.backend.constant.QuestionType;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestionRequest {
    private Integer id;
    private Integer sourceBankQuestionId;
    private String content;
    private QuestionType type;
    private BigDecimal points;
    private Integer resourceId;
    private List<QuizAnswerRequest> answers;
    private List<QuestionInteractionItemRequest> items;
}
