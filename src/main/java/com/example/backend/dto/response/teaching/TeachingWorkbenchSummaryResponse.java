package com.example.backend.dto.response.teaching;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeachingWorkbenchSummaryResponse {
    private long totalClasses;
    private long totalStudents;
    private long pendingSubmissions;
    private long pendingQuizReviews;
    private long atRiskStudents;
    private long upcomingAssignments;
}
