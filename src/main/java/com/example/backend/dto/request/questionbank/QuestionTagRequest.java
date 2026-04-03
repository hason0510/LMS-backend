package com.example.backend.dto.request.questionbank;

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
public class QuestionTagRequest {
    @NotBlank
    private String name;
    @NotNull
    private Integer subjectId;
}
