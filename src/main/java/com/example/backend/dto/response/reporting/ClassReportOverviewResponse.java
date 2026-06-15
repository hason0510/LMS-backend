package com.example.backend.dto.response.reporting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassReportOverviewResponse {
    private Integer classSectionId;
    private String classTitle;
    private String classCode;
    private String subjectTitle;
    private String status;
    private String startDate;
    private String endDate;
    private String primaryTeacherName;
    private Integer primaryTeacherId;

    private long totalStudents;
    private double averageProgress;
    private long pendingRequests;
    private long taCount;
    private long trackedQuizzes;
    private long trackedAssignments;
    private long atRiskStudents;
    private long engagedStudents;
    private double topScore;
    private double averageQuizScore;

    private ProgressBuckets progressBuckets;
    private List<TeachingMember> teachingAssistants;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressBuckets {
        private long low;
        private long medium;
        private long high;
        private int lowThreshold;
        private int highThreshold;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeachingMember {
        private Integer userId;
        private String fullName;
        private String email;
        private String avatarUrl;
        private String role;
    }
}
