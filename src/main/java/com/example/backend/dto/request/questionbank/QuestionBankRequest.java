package com.example.backend.dto.request.questionbank;

import com.example.backend.constant.QuestionBankScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBankRequest {
    @NotBlank
    private String name;
    private String description;
    @NotNull
    private QuestionBankScope scopeType;
    private Integer subjectId;
    private Integer curriculumVersionId;
    private Integer classSectionId;
}
