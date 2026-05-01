package com.example.backend.dto.response.benchmark;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BenchmarkLearningSeedResponse {
    private String seedTag;
    private Integer firstTeacherUserId;
    private Integer lastTeacherUserId;
    private Integer firstStudentUserId;
    private Integer lastStudentUserId;
    private Integer firstSubjectId;
    private Integer lastSubjectId;
    private Integer firstClassSectionId;
    private Integer lastClassSectionId;
    private Integer firstEnrollmentId;
    private Integer lastEnrollmentId;
    private Integer firstQuizId;
    private Integer lastQuizId;
    private Integer firstQuizAttemptId;
    private Integer lastQuizAttemptId;
    private Integer teachersInserted;
    private Integer studentsInserted;
    private Integer subjectsInserted;
    private Integer classSectionsInserted;
    private Integer enrollmentsInserted;
    private Integer quizzesInserted;
    private Integer quizAttemptsInserted;
    private Long durationMs;
}
