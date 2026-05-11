package com.example.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionGradeRequest {
    @NotNull(message = "grade is required")
    @Min(value = 0, message = "grade must be greater than or equal to 0")
    private Integer grade;

    private String feedback;
}
