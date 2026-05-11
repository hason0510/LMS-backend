package com.example.backend.dto.response.assignment;

import com.example.backend.constant.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentAssignmentFeedResponse {
    private Integer assignmentId;
    private String assignmentTitle;
    private Integer classSectionId;
    private String classSectionTitle;
    private LocalDateTime dueAt;
    private LocalDateTime closeAt;
    private Integer maxScore;
    private Boolean allowLateSubmission;
    private SubmissionStatus submissionStatus;
    private LocalDateTime submissionTime;
    private Integer grade;
    private boolean completed;
    private boolean pastDue;
    private boolean upcoming;
}

