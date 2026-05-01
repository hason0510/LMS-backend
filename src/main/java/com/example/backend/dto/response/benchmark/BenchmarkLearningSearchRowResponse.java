package com.example.backend.dto.response.benchmark;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BenchmarkLearningSearchRowResponse {
    private Integer userId;
    private String userName;
    private String fullName;
    private Integer subjectId;
    private String subjectTitle;
    private Long enrolledClassCount;
    private Long quizCount;
    private Long attemptCount;
    private Double averageGrade;
    private String lastAttemptTime;
}
