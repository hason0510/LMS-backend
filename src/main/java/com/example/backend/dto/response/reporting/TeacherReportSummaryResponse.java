package com.example.backend.dto.response.reporting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherReportSummaryResponse {
    private long totalClasses;
    private long totalStudents;
    private long pendingSubmissions;
    private long pendingQuizReviews;
    private long atRiskStudents;
    private long pendingRequests;

    private List<TaughtClassItem> taughtClasses;
    private List<AssistedClassItem> assistedClasses;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaughtClassItem {
        private Integer classSectionId;
        private String title;
        private String classCode;
        private String status;
        private String subjectTitle;
        private long totalEnrollments;
        private long taCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssistedClassItem {
        private Integer classSectionId;
        private String title;
        private String classCode;
        private String status;
        private String subjectTitle;
        private String primaryTeacherName;
        private long totalEnrollments;
    }
}
