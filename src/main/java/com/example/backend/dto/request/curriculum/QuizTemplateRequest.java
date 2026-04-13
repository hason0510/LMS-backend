package com.example.backend.dto.request.curriculum;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QuizTemplateRequest {
    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    private String description;

    private Integer minPassScore;

    private Integer timeLimitMinutes;

    private Integer maxAttempts;

    private LocalDateTime availableFrom;

    private LocalDateTime availableTo;
}
