package com.example.backend.dto.request.curriculum;

import com.example.backend.dto.request.quiz.QuizBankSourceRequest;
import com.example.backend.dto.request.quiz.QuizQuestionRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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

    private Boolean generateQuestionsPerAttempt;

    private Boolean shuffleQuestions;

    private Boolean shuffleAnswers;

    private String displayMode;

    private Boolean showCorrectAnswer;

    private List<QuizBankSourceRequest> bankSources;

    private List<QuizQuestionRequest> questions;
}
