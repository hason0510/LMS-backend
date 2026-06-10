package com.example.backend.dto.request.questionbank;

import com.example.backend.constant.DifficultyLevel;
import com.example.backend.constant.QuestionType;
import com.example.backend.dto.request.quiz.QuestionInteractionItemRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class BankQuestionRequest {
    @NotBlank
    private String content;
    private String explanation;
    private Integer resourceId;
    @NotNull
    private QuestionType type;
    private DifficultyLevel difficultyLevel;
    private BigDecimal defaultPoints;
    private List<String> tagNames;
    private List<BankQuestionOptionRequest> options;
    private List<QuestionInteractionItemRequest> items;
}
