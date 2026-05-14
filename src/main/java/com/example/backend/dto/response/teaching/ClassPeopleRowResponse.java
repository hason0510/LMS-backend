package com.example.backend.dto.response.teaching;

import com.example.backend.constant.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassPeopleRowResponse {
    private Integer enrollmentId;
    private Integer studentId;
    private String studentName;
    private String studentNumber;
    private String email;
    private String avatarUrl;
    private EnrollmentStatus enrollmentStatus;
    private Integer progress;
    private long missingAssignments;
    private long pendingReviews;
    private Number latestScore;
    private String lastActivity;
    private boolean self;
}
