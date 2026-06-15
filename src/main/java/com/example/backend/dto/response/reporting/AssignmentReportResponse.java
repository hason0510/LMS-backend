package com.example.backend.dto.response.reporting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentReportResponse {
    private long totalAssignments;
    private long totalStudents;
    private long totalGraded;
    private long totalPending;
    private long totalNotSubmitted;
    private long totalLateSubmitted;
    private long totalReturned;

    private List<AssignmentStatusRow> rows;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignmentStatusRow {
        private Integer assignmentId;
        private String assignmentTitle;
        private long totalStudents;
        private long submittedCount;
        private long gradedCount;
        private long pendingReviewCount;
        private long lateSubmittedCount;
        private long returnedCount;
        private long notSubmitted;
        private Integer maxScore;
        private String dueAt;
        private String closeAt;
    }
}
