package com.example.backend.service.impl;

import com.example.backend.dto.request.quiz.QuizQuestionRequest;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.quiz.QuizAnswerResponse;
import com.example.backend.dto.response.quiz.QuizQuestionResponse;
import com.example.backend.entity.Quiz;
import com.example.backend.entity.QuizQuestion;
import com.example.backend.repository.QuizQuestionRepository;
import com.example.backend.repository.QuizRepository;
import com.example.backend.service.QuizQuestionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
public class QuizQuestionServiceImpl implements QuizQuestionService{
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizRepository quizRepository;

    public QuizQuestionServiceImpl(QuizQuestionRepository quizQuestionRepository, QuizRepository quizRepository) {
        this.quizQuestionRepository = quizQuestionRepository;
        this.quizRepository = quizRepository;
    }

    @Override
    public QuizQuestionResponse convertQuizQuestionToDTO(QuizQuestion question) {
        QuizQuestionResponse response = new QuizQuestionResponse();
        response.setId(question.getId());
        response.setContent(question.getContent());
        response.setType(question.getType());
        response.setPoints(question.getPoints());
        if (question.getAnswers() != null) {
            response.setAnswers(question.getAnswers().stream().map(answer -> {
                QuizAnswerResponse aDto = new QuizAnswerResponse();
                aDto.setId(answer.getId());
                aDto.setIsCorrect(answer.getIsCorrect());
                aDto.setContent(answer.getContent());
                return aDto;
            }).collect(Collectors.toList()));
        } else {
            response.setAnswers(new ArrayList<>());
        }
        
        return response;
    }

    @Override
    public PageResponse<QuizQuestionResponse> getQuizQuestionPage(Pageable pageable) {
        Page<QuizQuestion> questionPage = quizQuestionRepository.findAll(pageable);
        Page<QuizQuestionResponse> responsePage = questionPage.map(this::convertQuizQuestionToDTO);

        return new PageResponse<>(
                responsePage.getNumber() + 1,
                responsePage.getTotalPages(),
                (int) responsePage.getTotalElements(),
                responsePage.getContent()
        );
    }

    @Override
    public QuizQuestionResponse getQuizQuestionById(Integer id) {
        QuizQuestion question = quizQuestionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found with id: " + id));
        return convertQuizQuestionToDTO(question);
    }

    @Override
    @Transactional
    public QuizQuestionResponse createQuizQuestion(Integer quizId, QuizQuestionRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found with id: " + quizId));

        QuizQuestion question = new QuizQuestion();
        question.setContent(request.getContent());
        question.setType(request.getType());
        question.setPoints(request.getPoints());
        question.setQuiz(quiz);

        return convertQuizQuestionToDTO(quizQuestionRepository.save(question));
    }

    @Override
    @Transactional
    public QuizQuestionResponse updateQuizQuestion(Integer id, QuizQuestionRequest request) {
        QuizQuestion question = quizQuestionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        if(request.getContent() != null){
            question.setContent(request.getContent());
        }
        if(request.getType() != null){
            question.setType(request.getType());
        }
        if(request.getPoints() != null){
            question.setPoints(request.getPoints());
        }
        return convertQuizQuestionToDTO(quizQuestionRepository.save(question));
    }

    @Override
    @Transactional
    public void deleteQuizQuestion(Integer id) {
        QuizQuestion question = quizQuestionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        quizQuestionRepository.delete(question);
    }
}
