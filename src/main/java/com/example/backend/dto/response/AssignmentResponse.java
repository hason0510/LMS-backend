package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentResponse {
    private Integer id;
    private String title;
    private String description;
    private String instruction;
    private Integer maxScore;
    private LocalDateTime dueAt;
    private Boolean allowLateSubmission;
    private Integer classSectionId;
}
