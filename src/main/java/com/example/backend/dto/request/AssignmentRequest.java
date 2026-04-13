package com.example.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentRequest {
    @NotBlank(message = "Assignment title is required")
    private String title;

    private String description;
    private String instruction;

    @NotNull(message = "maxScore is required")
    @Min(value = 1, message = "maxScore must be greater than 0")
    private Integer maxScore;

    private LocalDateTime dueAt;

    private Boolean allowLateSubmission;

    @NotNull(message = "classSectionId is required")
    private Integer classSectionId;
}
