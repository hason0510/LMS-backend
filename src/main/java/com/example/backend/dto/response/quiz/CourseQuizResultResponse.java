package com.example.backend.dto.response.quiz;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseQuizResultResponse {
    // Info Sinh viên
    private Integer studentId;
    private String studentName;
    private String studentNumber;

    // Info Bài Quiz
    private Integer quizId;
    private String quizTitle;
    private Integer chapterItemId;

    // Kết quả cao nhất
    private Integer maxGrade;
}
