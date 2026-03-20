package com.example.backend.service.impl;

import com.example.backend.dto.request.quiz.QuizRequest;
import com.example.backend.dto.request.quiz.QuizQuestionRequest;
import com.example.backend.dto.request.quiz.QuizAnswerRequest;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.quiz.QuizResponse;
import com.example.backend.dto.response.quiz.QuizQuestionResponse;
import com.example.backend.dto.response.quiz.QuizAnswerResponse;
import com.example.backend.entity.Quiz;
import com.example.backend.entity.QuizQuestion;
import com.example.backend.entity.QuizAnswer;
import com.example.backend.repository.QuizRepository;
import com.example.backend.repository.QuizQuestionRepository;
import com.example.backend.repository.QuizAnswerRepository;
import com.example.backend.service.QuizService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuizServiceImpl implements QuizService {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizAnswerRepository quizAnswerRepository;

    public QuizServiceImpl(QuizRepository quizRepository, QuizQuestionRepository quizQuestionRepository, QuizAnswerRepository quizAnswerRepository){
        this.quizRepository = quizRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.quizAnswerRepository = quizAnswerRepository;
    }

    @Override
    public QuizResponse convertQuizToDTO(Quiz quiz) {
        QuizResponse response = new QuizResponse();
        response.setId(quiz.getId());
        response.setTitle(quiz.getTitle());
        response.setDescription(quiz.getDescription());
        response.setMinPassScore(quiz.getMinPassScore());
        response.setTimeLimitMinutes(quiz.getTimeLimitMinutes());
        response.setMaxAttempts(quiz.getMaxAttempts());
        response.setAvailableFrom(quiz.getAvailableFrom());
        response.setAvailableUntil(quiz.getAvailableUntil());
        if (quiz.getQuestions() != null) {
            response.setQuestions(quiz.getQuestions().stream()
                .map(question -> {
                    QuizQuestionResponse qDto = new QuizQuestionResponse();
                    qDto.setId(question.getId());
                    qDto.setContent(question.getContent());
                    qDto.setType(question.getType());
                    qDto.setPoints(question.getPoints());
                    if (question.getAnswers() != null) {
                        qDto.setAnswers(question.getAnswers().stream()
                            .map(answer -> {
                                var aDto = new QuizAnswerResponse();
                                aDto.setId(answer.getId());
                                aDto.setContent(answer.getContent());
                                aDto.setIsCorrect(answer.getIsCorrect());
                                return aDto;
                            })
                            .collect(Collectors.toList()));
                    }
                    return qDto;
                })
                .collect(Collectors.toList()));
        }
        return response;
    }
    
    @Transactional
    public void createQuestionForQuiz(Integer quizId, QuizQuestion question) {
        quizQuestionRepository.save(question);
    }

    @Override
    public PageResponse<QuizResponse> getQuizPage(Pageable pageable) {
        Page<Quiz> quizPage = quizRepository.findAll(pageable);
        Page<QuizResponse> quizResponsePage = quizPage.map(this::convertQuizToDTO);

        return new PageResponse<>(
                quizResponsePage.getNumber() + 1,
                quizResponsePage.getTotalPages(),
                (int) quizResponsePage.getTotalElements(),
                quizResponsePage.getContent()
        );
    }

    @Override
    public QuizResponse getQuizById(Integer id) {
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quiz not found with id: " + id));
        return convertQuizToDTO(quiz);
    }

    @Override
    @Transactional
    public QuizResponse createQuiz(QuizRequest request) {
        Quiz quiz = new Quiz();
        quiz.setTitle(request.getTitle());
        quiz.setDescription(request.getDescription());
        quiz.setMinPassScore(request.getMinPassScore());
        quiz.setTimeLimitMinutes(request.getTimeLimitMinutes());
        quiz.setMaxAttempts(request.getMaxAttempts());
        quiz.setAvailableFrom(request.getAvailableFrom());
        quiz.setAvailableUntil(request.getAvailableUntil());
        
        Quiz savedQuiz = quizRepository.save(quiz);
        
        // Handle questions if provided
        if (request.getQuestions() != null && !request.getQuestions().isEmpty()) {
            for (QuizQuestionRequest questionRequest : request.getQuestions()) {
                QuizQuestion question = new QuizQuestion();
                question.setContent(questionRequest.getContent());
                question.setType(questionRequest.getType());
                question.setPoints(questionRequest.getPoints() != null ? questionRequest.getPoints() : 1);
                question.setFileUrl(questionRequest.getFileUrl());
                question.setEmbedUrl(questionRequest.getEmbedUrl());
                question.setCloudinaryId(questionRequest.getCloudinaryId());
                question.setQuiz(savedQuiz);
                
                QuizQuestion savedQuestion = quizQuestionRepository.save(question);
                
                // Handle answers if provided
                if (questionRequest.getAnswers() != null && !questionRequest.getAnswers().isEmpty()) {
                    for (QuizAnswerRequest answerRequest : questionRequest.getAnswers()) {
                        QuizAnswer answer = new QuizAnswer();
                        answer.setContent(answerRequest.getContent());
                        answer.setIsCorrect(answerRequest.getIsCorrect() != null ? answerRequest.getIsCorrect() : false);
                        answer.setQuizQuestion(savedQuestion);
                        quizAnswerRepository.save(answer);
                    }
                }
            }
        }
        
        return convertQuizToDTO(savedQuiz);
    }

    @Override
    @Transactional
    public QuizResponse updateQuiz(Integer id, QuizRequest request) {
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        if(request.getTitle() != null){
            quiz.setTitle(request.getTitle());
        }
        if(request.getDescription() != null){
            quiz.setDescription(request.getDescription());
        }
        if(request.getMinPassScore() != null){
            quiz.setMinPassScore(request.getMinPassScore());
        }
        if(request.getTimeLimitMinutes() != null){
            quiz.setTimeLimitMinutes(request.getTimeLimitMinutes());
        }
        if(request.getMaxAttempts() != null){
            quiz.setMaxAttempts(request.getMaxAttempts());
        }
        if(request.getAvailableFrom() != null){
            quiz.setAvailableFrom(request.getAvailableFrom());
        }
        if(request.getAvailableUntil() != null){
            quiz.setAvailableUntil(request.getAvailableUntil());
        }
        
        Quiz savedQuiz = quizRepository.save(quiz);
        
        // Handle questions update if provided
        if (request.getQuestions() != null && !request.getQuestions().isEmpty()) {
            for (QuizQuestionRequest questionRequest : request.getQuestions()) {
                if (questionRequest.getId() == null) {
                    // Create new question
                    QuizQuestion question = new QuizQuestion();
                    question.setContent(questionRequest.getContent());
                    question.setType(questionRequest.getType());
                    question.setPoints(questionRequest.getPoints() != null ? questionRequest.getPoints() : 1);
                    question.setFileUrl(questionRequest.getFileUrl());
                    question.setEmbedUrl(questionRequest.getEmbedUrl());
                    question.setCloudinaryId(questionRequest.getCloudinaryId());
                    question.setQuiz(savedQuiz);
                    
                    QuizQuestion savedQuestion = quizQuestionRepository.save(question);
                    
                    // Handle answers for new question
                    if (questionRequest.getAnswers() != null && !questionRequest.getAnswers().isEmpty()) {
                        for (QuizAnswerRequest answerRequest : questionRequest.getAnswers()) {
                            QuizAnswer answer = new QuizAnswer();
                            answer.setContent(answerRequest.getContent());
                            answer.setIsCorrect(answerRequest.getIsCorrect() != null ? answerRequest.getIsCorrect() : false);
                            answer.setQuizQuestion(savedQuestion);
                            quizAnswerRepository.save(answer);
                        }
                    }
                } else {
                    // Update existing question
                    QuizQuestion question = quizQuestionRepository.findById(questionRequest.getId())
                            .orElseThrow(() -> new RuntimeException("Question not found"));
                    question.setContent(questionRequest.getContent());
                    question.setType(questionRequest.getType());
                    if (questionRequest.getPoints() != null) {
                        question.setPoints(questionRequest.getPoints());
                    }
                    question.setFileUrl(questionRequest.getFileUrl());
                    question.setEmbedUrl(questionRequest.getEmbedUrl());
                    question.setCloudinaryId(questionRequest.getCloudinaryId());
                    
                    QuizQuestion savedQuestion = quizQuestionRepository.save(question);
                    
                    // Handle answers update
                    if (questionRequest.getAnswers() != null && !questionRequest.getAnswers().isEmpty()) {
                        for (QuizAnswerRequest answerRequest : questionRequest.getAnswers()) {
                            if (answerRequest.getId() == null) {
                                // Create new answer
                                QuizAnswer answer = new QuizAnswer();
                                answer.setContent(answerRequest.getContent());
                                answer.setIsCorrect(answerRequest.getIsCorrect() != null ? answerRequest.getIsCorrect() : false);
                                answer.setQuizQuestion(savedQuestion);
                                quizAnswerRepository.save(answer);
                            } else {
                                // Update existing answer
                                QuizAnswer answer = quizAnswerRepository.findById(answerRequest.getId())
                                        .orElseThrow(() -> new RuntimeException("Answer not found"));
                                answer.setContent(answerRequest.getContent());
                                answer.setIsCorrect(answerRequest.getIsCorrect() != null ? answerRequest.getIsCorrect() : false);
                                quizAnswerRepository.save(answer);
                            }
                        }
                    }
                }
            }
        }
        
        return convertQuizToDTO(savedQuiz);
    }

    @Transactional
    @Override
    public void deleteQuiz(Integer id) {
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow();
        quiz.getQuestions().forEach(q -> {
            q.getAnswers().forEach(a -> a.set_deleted(true));
            q.set_deleted(true);
        });
        quiz.getAttempts().forEach(a -> a.set_deleted(true));
        quiz.set_deleted(true);
    }

}