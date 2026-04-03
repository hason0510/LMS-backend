package com.example.backend.dto.response.questionbank;

import com.example.backend.constant.QuestionBankScope;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBankResponse {
    private Integer id;
    private String name;
    private String description;
    private QuestionBankScope scopeType;
    private Integer subjectId;
    private Integer curriculumVersionId;
    private Integer classSectionId;
    private List<BankQuestionResponse> questions;
}
