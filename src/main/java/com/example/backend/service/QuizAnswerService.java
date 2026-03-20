package com.example.backend.service;

import com.example.backend.dto.request.quiz.QuizAnswerRequest;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.quiz.QuizAnswerResponse;
import com.example.backend.entity.QuizAnswer;
import org.springframework.data.domain.Pageable;

public interface QuizAnswerService {
    QuizAnswerResponse createQuizAnswer(Integer questionId, QuizAnswerRequest request);
    QuizAnswerResponse getQuizAnswerById(Integer id);
    QuizAnswerResponse updateQuizAnswer(Integer id, QuizAnswerRequest request);
    void deleteQuizAnswer(Integer id);
    
    PageResponse<QuizAnswerResponse> getQuizAnswerPage(Pageable pageable);
    QuizAnswerResponse convertQuizAnswerToDTO(QuizAnswer quizAnswer);
}
