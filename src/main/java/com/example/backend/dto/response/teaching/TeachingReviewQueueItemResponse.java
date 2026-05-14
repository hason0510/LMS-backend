package com.example.backend.dto.response.teaching;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeachingReviewQueueItemResponse {
    private String type;
    private Integer submissionId;
    private Integer attemptId;
    private Integer assignmentId;
    private Integer quizId;
    private Integer classSectionId;
    private String classSectionTitle;
    private String title;
    private Integer studentId;
    private String studentName;
    private LocalDateTime submittedAt;
    private LocalDateTime dueAt;
    private String status;
    private boolean late;
    private Number score;
    private Number maxScore;
    private boolean selfOwned;
}
