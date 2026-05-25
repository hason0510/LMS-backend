package com.example.backend.dto.response;

import com.example.backend.constant.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionResponse {
    private Integer id;
    private Integer assignmentId;
    private String assignmentTitle;
    private Integer classSectionId;
    private Integer studentId;
    private String studentName;
    private String studentNumber;
    private String studentAvatar;
    private String description;
    private String fileUrl;
    private String embedUrl;
    private String cloudinaryId;
    private LocalDateTime submissionTime;
    private SubmissionStatus status;
    private Integer submissionCount;
    private Integer grade;
    private String feedback;
    private LocalDateTime gradedAt;
    private LocalDateTime dueAt;
    private LocalDateTime closeAt;
    private boolean late;
    private boolean canResubmit;
    private List<ResourceResponse> resources;
}
