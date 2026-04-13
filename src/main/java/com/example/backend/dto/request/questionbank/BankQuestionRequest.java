package com.example.backend.dto.request.questionbank;

import com.example.backend.constant.DifficultyLevel;
import com.example.backend.constant.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BankQuestionRequest {
    @NotBlank
    private String content;
    private String explanation;
    private String fileUrl;
    private String embedUrl;
    private String cloudinaryId;
    @NotNull
    private QuestionType type;
    private DifficultyLevel difficultyLevel;
    private Integer defaultPoints;
    private Integer parentQuestionId;
    private List<Integer> tagIds;
    private List<BankQuestionOptionRequest> options;
}
