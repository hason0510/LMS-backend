package com.example.backend.service;

import com.example.backend.dto.request.quiz.QuizRequest;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.quiz.QuizResponse;
import com.example.backend.dto.response.quiz.StudentQuizFeedResponse;
import com.example.backend.entity.quiz.Quiz;
import com.example.backend.entity.quiz.QuizQuestion;
import org.springframework.data.domain.Pageable;

public interface QuizService {
    QuizResponse createQuiz(QuizRequest request);
    QuizResponse getQuizById(Integer id);
    QuizResponse getQuizById(Integer id, Integer classContentItemId);
    QuizResponse getQuizPreviewSample(Integer id, Long seed);
    QuizResponse updateQuiz(Integer id, QuizRequest request);
    void deleteQuiz(Integer id);
    
    PageResponse<QuizResponse> getQuizPage(Pageable pageable);
    QuizResponse convertQuizToDTO(Quiz quiz);
    void createQuestionForQuiz(Integer quizId, QuizQuestion question);

    PageResponse<StudentQuizFeedResponse> getStudentQuizFeed(String tab, String keyword, Integer classSectionId);
}
