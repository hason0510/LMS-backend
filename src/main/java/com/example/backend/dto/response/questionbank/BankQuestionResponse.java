package com.example.backend.dto.response.questionbank;

import com.example.backend.constant.DifficultyLevel;
import com.example.backend.constant.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BankQuestionResponse {
    private Integer id;
    private Integer questionBankId;
    private Integer parentQuestionId;
    private String content;
    private String explanation;
    private String fileUrl;
    private String embedUrl;
    private String cloudinaryId;
    private QuestionType type;
    private DifficultyLevel difficultyLevel;
    private Integer defaultPoints;
    private List<QuestionTagResponse> tags;
    private List<BankQuestionOptionResponse> options;
}
