package com.example.backend.dto.response.curriculum;

import com.example.backend.dto.response.quiz.QuizBankSourceResponse;
import com.example.backend.dto.response.quiz.QuizQuestionResponse;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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
    private Boolean generateQuestionsPerAttempt;
    private Boolean shuffleQuestions;
    private Boolean shuffleAnswers;
    private String displayMode;
    private Boolean showCorrectAnswer;
    private Integer questionCount;
    private List<QuizBankSourceResponse> bankSources;
    private List<QuizQuestionResponse> questions;
}
