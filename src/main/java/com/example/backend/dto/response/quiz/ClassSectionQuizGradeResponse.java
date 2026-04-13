package com.example.backend.dto.response.quiz;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClassSectionQuizGradeResponse {
    private Integer studentId;
    private String studentName;
    private String studentNumber;
    private Integer quizId;
    private String quizTitle;
    private Integer classContentItemId;
    private Integer maxGrade;
}
