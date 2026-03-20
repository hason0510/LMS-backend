package com.example.backend.service;

import com.example.backend.dto.request.quiz.QuizRequest;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.quiz.QuizResponse;
import com.example.backend.entity.Quiz;
import org.springframework.data.domain.Pageable;

public interface QuizService {
    QuizResponse createQuiz(QuizRequest request);
    QuizResponse getQuizById(Integer id);
    QuizResponse updateQuiz(Integer id, QuizRequest request);
    void deleteQuiz(Integer id);
    
    PageResponse<QuizResponse> getQuizPage(Pageable pageable);
    QuizResponse convertQuizToDTO(Quiz quiz);
    void createQuestionForQuiz(Integer quizId, com.example.backend.entity.QuizQuestion question);
}
