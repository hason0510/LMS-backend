package com.example.backend.service;

import com.example.backend.dto.request.quiz.QuizQuestionRequest;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.quiz.QuizQuestionResponse;
import com.example.backend.entity.QuizQuestion;
import org.springframework.data.domain.Pageable;

public interface QuizQuestionService {
    QuizQuestionResponse createQuizQuestion(Integer quizId, QuizQuestionRequest request);
    QuizQuestionResponse getQuizQuestionById(Integer id);
    QuizQuestionResponse updateQuizQuestion(Integer id, QuizQuestionRequest request);
    void deleteQuizQuestion(Integer id);
    
    PageResponse<QuizQuestionResponse> getQuizQuestionPage(Pageable pageable);
    QuizQuestionResponse convertQuizQuestionToDTO(QuizQuestion question);
}
