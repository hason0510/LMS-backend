package com.example.backend.dto.response.quiz;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClassSectionStudentQuizResultResponse {
    private Integer quizId;
    private String quizTitle;
    private Integer classContentItemId;
    private Integer maxGrade;

    @JsonProperty("isPassed")
    private Boolean isPassed;
}
