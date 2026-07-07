package com.example.backend.dto.response.reporting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizReportResponse {
    private long totalQuizzes;
    private long totalAttempts;
    private long totalParticipants;
    private double averageScore;
    private double topScore;
    private long totalPassed;
    private long totalNotPassed;
    private long totalWaitingReview;
    private long passedStudents;
    private long participantStudents;

    private List<HistogramBin> histogram;
    private List<QuizSummaryRow> rows;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizSummaryRow {
        private Integer quizId;
        private Integer classContentItemId;
        private String quizTitle;
        private long totalAttempts;
        private long uniqueStudents;
        private long passedCount;
        private long passedStudents;
        private long notPassedCount;
        private long waitingReviewCount;
        private double averageScore;
        private double topScore;
        private Integer minPassScore;
        private List<HistogramBin> histogram;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistogramBin {
        private String range;
        private int count;
    }
}
