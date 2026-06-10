package com.example.backend.dto.response.questionbank;

import com.example.backend.constant.DifficultyLevel;
import com.example.backend.constant.QuestionType;
import com.example.backend.dto.response.ResourceResponse;
import com.example.backend.dto.response.quiz.QuestionInteractionItemResponse;
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
public class BankQuestionResponse {
    private Integer id;
    private Integer questionBankId;
    private String content;
    private String explanation;
    private ResourceResponse resource;
    private QuestionType type;
    private DifficultyLevel difficultyLevel;
    private BigDecimal defaultPoints;
    private List<QuestionTagResponse> tags;
    private List<BankQuestionOptionResponse> options;
    private List<QuestionInteractionItemResponse> items;
}
