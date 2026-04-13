package com.example.backend.dto.request.quiz;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizRequest {
    private String title;
    private String description;
    private Integer minPassScore;
    private Integer timeLimitMinutes;
    private Integer maxAttempts;
    private LocalDateTime availableFrom;
    private LocalDateTime availableUntil;
    private Integer classSectionId;
    private Integer classContentItemId;
    private List<QuizBankSourceRequest> bankSources;
    private List<QuizQuestionRequest> questions;
}
