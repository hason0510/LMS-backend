package com.example.backend.dto.response.assignment;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeacherAssignmentOverviewResponse {
    private Integer assignmentId;
    private String assignmentTitle;
    private Integer classSectionId;
    private String classSectionTitle;
    private LocalDateTime dueAt;
    private LocalDateTime closeAt;
    private Integer maxScore;
    private long totalStudents;
    private long turnedInCount;
    private long gradedCount;
    private long pendingReviewCount;
    private boolean pastDue;
    private boolean upcoming;
    private boolean completed;
}
