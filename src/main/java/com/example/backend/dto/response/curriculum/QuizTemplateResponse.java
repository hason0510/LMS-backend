package com.example.backend.dto.response.curriculum;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QuizTemplateResponse {
    private Integer id;
    private String title;
    private String description;
    private Integer minPassScore;
    private Integer timeLimitMinutes;
    private Integer maxAttempts;
    private LocalDateTime availableFrom;
    private LocalDateTime availableTo;
}
