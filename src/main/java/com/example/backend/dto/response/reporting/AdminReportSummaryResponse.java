package com.example.backend.dto.response.reporting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminReportSummaryResponse {
    private long totalUsers;
    private long totalClassSections;
    private long totalSubjects;
    private long totalTeachers;
    private long totalAssistants;
    private long totalStudents;
    private long pendingEnrollments;
    private long pendingSubmissions;
    private long pendingQuizReviews;

    private List<StatusBreakdownItem> classStatusBreakdown;
    private List<TeacherLoadItem> teacherLoad;
    private List<SubjectLoadItem> subjectLoad;
    private List<TopClassItem> topClasses;
    private List<AssistantClassesItem> assistantsActive;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusBreakdownItem {
        private String status;
        private long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherLoadItem {
        private Integer teacherId;
        private String teacherName;
        private long classCount;
        private long studentCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectLoadItem {
        private Integer subjectId;
        private String subjectTitle;
        private long classCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopClassItem {
        private Integer classSectionId;
        private String title;
        private String classCode;
        private String status;
        private String subjectTitle;
        private Integer teacherId;
        private String teacherName;
        private long totalEnrollments;
        private long taCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssistantClassesItem {
        private Integer userId;
        private String fullName;
        private String email;
        private String avatarUrl;
        private List<AssistantClassRef> classes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssistantClassRef {
        private Integer classSectionId;
        private String title;
        private String classCode;
    }
}
