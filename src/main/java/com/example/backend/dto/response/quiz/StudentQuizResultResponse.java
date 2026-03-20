package com.example.backend.dto.response.quiz;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentQuizResultResponse {
    private Integer quizId;
    private String quizTitle;
    private Integer chapterItemId;
    private Integer maxGrade;// Điểm cao nhất đạt được
    @JsonProperty("isPassed")
    private Boolean isPassed; // Đã từng qua môn chưa
}