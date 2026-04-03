package com.example.backend.service.impl;

import com.example.backend.constant.ContentItemType;
import com.example.backend.constant.QuizSourceSelectionMode;
import com.example.backend.dto.request.quiz.QuizAnswerRequest;
import com.example.backend.dto.request.quiz.QuizBankSourceRequest;
import com.example.backend.dto.request.quiz.QuizQuestionRequest;
import com.example.backend.dto.request.quiz.QuizRequest;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.quiz.QuizAnswerResponse;
import com.example.backend.dto.response.quiz.QuizBankSourceResponse;
import com.example.backend.dto.response.quiz.QuizQuestionResponse;
import com.example.backend.dto.response.quiz.QuizResponse;
import com.example.backend.entity.BankQuestion;
import com.example.backend.entity.BankQuestionOption;
import com.example.backend.entity.ClassContentItem;
import com.example.backend.entity.ClassSection;
import com.example.backend.entity.QuestionBank;
import com.example.backend.entity.QuestionTag;
import com.example.backend.entity.Quiz;
import com.example.backend.entity.QuizAnswer;
import com.example.backend.entity.QuizBankSource;
import com.example.backend.entity.QuizQuestion;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.BankQuestionRepository;
import com.example.backend.repository.ClassContentItemRepository;
import com.example.backend.repository.ClassSectionRepository;
import com.example.backend.repository.QuestionBankRepository;
import com.example.backend.repository.QuestionTagRepository;
import com.example.backend.repository.QuizAnswerRepository;
import com.example.backend.repository.QuizBankSourceRepository;
import com.example.backend.repository.QuizQuestionRepository;
import com.example.backend.repository.QuizRepository;
import com.example.backend.service.QuizService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class QuizServiceImpl implements QuizService {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final QuizBankSourceRepository quizBankSourceRepository;
    private final QuestionBankRepository questionBankRepository;
    private final BankQuestionRepository bankQuestionRepository;
    private final QuestionTagRepository questionTagRepository;
    private final ClassSectionRepository classSectionRepository;
    private final ClassContentItemRepository classContentItemRepository;

    public QuizServiceImpl(
            QuizRepository quizRepository,
            QuizQuestionRepository quizQuestionRepository,
            QuizAnswerRepository quizAnswerRepository,
            QuizBankSourceRepository quizBankSourceRepository,
            QuestionBankRepository questionBankRepository,
            BankQuestionRepository bankQuestionRepository,
            QuestionTagRepository questionTagRepository,
            ClassSectionRepository classSectionRepository,
            ClassContentItemRepository classContentItemRepository
    ) {
        this.quizRepository = quizRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.quizAnswerRepository = quizAnswerRepository;
        this.quizBankSourceRepository = quizBankSourceRepository;
        this.questionBankRepository = questionBankRepository;
        this.bankQuestionRepository = bankQuestionRepository;
        this.questionTagRepository = questionTagRepository;
        this.classSectionRepository = classSectionRepository;
        this.classContentItemRepository = classContentItemRepository;
    }

    @Override
    public QuizResponse convertQuizToDTO(Quiz quiz) {
        List<QuizQuestion> questions = quizQuestionRepository.findByQuiz_IdOrderByIdAsc(quiz.getId());
        List<QuizBankSource> bankSources = quizBankSourceRepository.findByQuiz_IdOrderByOrderIndexAsc(quiz.getId());

        QuizResponse response = new QuizResponse();
        response.setId(quiz.getId());
        response.setTitle(quiz.getTitle());
        response.setDescription(quiz.getDescription());
        response.setMinPassScore(quiz.getMinPassScore());
        response.setTimeLimitMinutes(quiz.getTimeLimitMinutes());
        response.setMaxAttempts(quiz.getMaxAttempts());
        response.setAvailableFrom(quiz.getAvailableFrom());
        response.setAvailableUntil(quiz.getAvailableUntil());
        response.setClassSectionId(quiz.getClassSection() != null ? quiz.getClassSection().getId() : null);
        response.setClassContentItemId(
                classContentItemRepository.findByOverrideQuiz_Id(quiz.getId())
                        .map(ClassContentItem::getId)
                        .orElse(null)
        );
        response.setBankSources(bankSources.stream().map(this::convertBankSourceToDTO).toList());
        response.setQuestions(questions.stream().map(this::convertQuestionToDTO).collect(Collectors.toList()));
        return response;
    }

    @Transactional
    public void createQuestionForQuiz(Integer quizId, QuizQuestion question) {
        if (question.getQuiz() == null) {
            question.setQuiz(quizRepository.findById(quizId)
                    .orElseThrow(() -> new ResourceNotFoundException("Quiz not found")));
        }
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
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + id));
        return convertQuizToDTO(quiz);
    }

    @Override
    @Transactional
    public QuizResponse createQuiz(QuizRequest request) {
        validateQuizStructureRequest(request);

        Quiz quiz = new Quiz();
        applyQuizMetadata(quiz, request, true);
        Quiz savedQuiz = quizRepository.save(quiz);
        syncQuizPlacement(savedQuiz, request);
        syncQuizContent(savedQuiz, request);
        return convertQuizToDTO(savedQuiz);
    }

    @Override
    @Transactional
    public QuizResponse updateQuiz(Integer id, QuizRequest request) {
        validateQuizStructureRequest(request);

        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));
        applyQuizMetadata(quiz, request, false);
        Quiz savedQuiz = quizRepository.save(quiz);
        syncQuizPlacement(savedQuiz, request);
        syncQuizContent(savedQuiz, request);
        return convertQuizToDTO(savedQuiz);
    }

    @Transactional
    @Override
    public void deleteQuiz(Integer id) {
        Quiz quiz = quizRepository.findById(id).orElseThrow();
        quizBankSourceRepository.findByQuiz_IdOrderByOrderIndexAsc(id).forEach(quizBankSourceRepository::delete);
        quizQuestionRepository.findByQuiz_IdOrderByIdAsc(id).forEach(question -> {
            if (question.getAnswers() != null) {
                question.getAnswers().forEach(answer -> answer.set_deleted(true));
            }
            question.set_deleted(true);
        });
        quiz.getAttempts().forEach(attempt -> attempt.set_deleted(true));
        quiz.set_deleted(true);
    }

    private void applyQuizMetadata(Quiz quiz, QuizRequest request, boolean isCreate) {
        if (isCreate || request.getTitle() != null) {
            quiz.setTitle(request.getTitle());
        }
        if (isCreate || request.getDescription() != null) {
            quiz.setDescription(request.getDescription());
        }
        if (isCreate || request.getMinPassScore() != null) {
            quiz.setMinPassScore(request.getMinPassScore());
        }
        if (isCreate || request.getTimeLimitMinutes() != null) {
            quiz.setTimeLimitMinutes(request.getTimeLimitMinutes());
        }
        if (isCreate || request.getMaxAttempts() != null) {
            quiz.setMaxAttempts(request.getMaxAttempts());
        }
        if (isCreate || request.getAvailableFrom() != null) {
            quiz.setAvailableFrom(request.getAvailableFrom());
        }
        if (isCreate || request.getAvailableUntil() != null) {
            quiz.setAvailableUntil(request.getAvailableUntil());
        }
        if (request.getClassSectionId() != null) {
            ClassSection classSection = classSectionRepository.findById(request.getClassSectionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
            quiz.setClassSection(classSection);
        }
    }

    private void syncQuizPlacement(Quiz quiz, QuizRequest request) {
        if (request.getClassContentItemId() == null) {
            return;
        }

        ClassContentItem classContentItem = classContentItemRepository.findById(request.getClassContentItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Class content item not found"));
        if (classContentItem.getItemType() != ContentItemType.QUIZ) {
            throw new BusinessException("Class content item is not a quiz");
        }

        ClassSection classSection = classContentItem.getClassChapter().getClassSection();
        if (quiz.getClassSection() == null) {
            quiz.setClassSection(classSection);
            quizRepository.save(quiz);
        }
        if (!Objects.equals(
                quiz.getClassSection() != null ? quiz.getClassSection().getId() : null,
                classSection.getId()
        )) {
            throw new BusinessException("Quiz classSection does not match the target class content item");
        }

        classContentItemRepository.findByOverrideQuiz_Id(quiz.getId())
                .filter(existing -> !existing.getId().equals(classContentItem.getId()))
                .ifPresent(existing -> {
                    existing.setOverrideQuiz(null);
                    classContentItemRepository.save(existing);
                });

        classContentItem.setOverrideQuiz(quiz);
        if (!StringUtils.hasText(classContentItem.getTitleOverride())) {
            classContentItem.setTitleOverride(quiz.getTitle());
        }
        classContentItemRepository.save(classContentItem);
    }

    private void syncQuizContent(Quiz quiz, QuizRequest request) {
        boolean hasBankSourcesField = request.getBankSources() != null;
        boolean hasQuestionsField = request.getQuestions() != null;
        if (!hasBankSourcesField && !hasQuestionsField) {
            return;
        }

        clearQuizSourcesAndQuestions(quiz);

        if (request.getBankSources() != null && !request.getBankSources().isEmpty()) {
            List<QuizBankSource> savedSources = saveBankSources(quiz, request.getBankSources());
            generateQuestionsFromBankSources(quiz, savedSources);
            return;
        }

        if (request.getQuestions() != null && !request.getQuestions().isEmpty()) {
            createManualQuestions(quiz, request.getQuestions());
        }
    }

    private void clearQuizSourcesAndQuestions(Quiz quiz) {
        quizBankSourceRepository.deleteAll(quizBankSourceRepository.findByQuiz_IdOrderByOrderIndexAsc(quiz.getId()));

        List<QuizQuestion> existingQuestions = quizQuestionRepository.findByQuiz_IdOrderByIdAsc(quiz.getId());
        for (QuizQuestion question : existingQuestions) {
            if (question.getAnswers() != null) {
                quizAnswerRepository.deleteAll(question.getAnswers());
            }
            quizQuestionRepository.delete(question);
        }
    }

    private List<QuizBankSource> saveBankSources(Quiz quiz, List<QuizBankSourceRequest> sourceRequests) {
        List<QuizBankSource> savedSources = new ArrayList<>();
        for (int i = 0; i < sourceRequests.size(); i++) {
            QuizBankSourceRequest sourceRequest = sourceRequests.get(i);
            QuestionBank questionBank = questionBankRepository.findById(sourceRequest.getQuestionBankId())
                    .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));

            if (questionBank.getClassSection() != null) {
                if (quiz.getClassSection() == null) {
                    quiz.setClassSection(questionBank.getClassSection());
                    quizRepository.save(quiz);
                } else if (!quiz.getClassSection().getId().equals(questionBank.getClassSection().getId())) {
                    throw new BusinessException("Class-scoped question bank does not belong to the quiz class section");
                }
            }

            QuestionTag tag = null;
            if (sourceRequest.getTagId() != null) {
                tag = questionTagRepository.findById(sourceRequest.getTagId())
                        .orElseThrow(() -> new ResourceNotFoundException("Question tag not found"));
            }

            QuizBankSource source = new QuizBankSource();
            source.setQuiz(quiz);
            source.setQuestionBank(questionBank);
            source.setTag(tag);
            source.setOrderIndex(i + 1);
            source.setSelectionMode(sourceRequest.getSelectionMode());
            source.setQuestionCount(sourceRequest.getQuestionCount());
            source.setDifficultyLevel(sourceRequest.getDifficultyLevel());
            source.setManualQuestionIds(joinIds(sourceRequest.getManualQuestionIds()));
            savedSources.add(quizBankSourceRepository.save(source));
        }
        return savedSources;
    }

    private void generateQuestionsFromBankSources(Quiz quiz, List<QuizBankSource> bankSources) {
        LinkedHashMap<Integer, BankQuestion> selectedQuestions = new LinkedHashMap<>();
        for (QuizBankSource source : bankSources) {
            for (BankQuestion question : selectQuestions(source)) {
                selectedQuestions.putIfAbsent(question.getId(), question);
            }
        }

        if (selectedQuestions.isEmpty()) {
            throw new BusinessException("No questions matched the configured question bank sources");
        }

        for (BankQuestion sourceQuestion : selectedQuestions.values()) {
            QuizQuestion question = new QuizQuestion();
            question.setQuiz(quiz);
            question.setSourceBankQuestion(sourceQuestion);
            question.setContent(sourceQuestion.getContent());
            question.setType(sourceQuestion.getType());
            question.setFileUrl(sourceQuestion.getFileUrl());
            question.setEmbedUrl(sourceQuestion.getEmbedUrl());
            question.setCloudinaryId(sourceQuestion.getCloudinaryId());
            question.setPoints(sourceQuestion.getDefaultPoints() != null ? sourceQuestion.getDefaultPoints() : 1);

            List<QuizAnswer> answers = new ArrayList<>();
            if (sourceQuestion.getOptions() != null) {
                for (BankQuestionOption sourceOption : sourceQuestion.getOptions()) {
                    QuizAnswer answer = new QuizAnswer();
                    answer.setQuizQuestion(question);
                    answer.setContent(sourceOption.getContent());
                    answer.setIsCorrect(sourceOption.getIsCorrect());
                    answer.setFileUrl(sourceOption.getFileUrl());
                    answer.setEmbedUrl(sourceOption.getEmbedUrl());
                    answer.setCloudinaryId(sourceOption.getCloudinaryId());
                    answers.add(answer);
                }
            }
            question.setAnswers(answers);
            quizQuestionRepository.save(question);
        }
    }

    private List<BankQuestion> selectQuestions(QuizBankSource source) {
        List<BankQuestion> matchedQuestions = bankQuestionRepository.findSelectableQuestions(
                source.getQuestionBank().getId(),
                source.getDifficultyLevel(),
                source.getTag() != null ? source.getTag().getId() : null
        );

        if (source.getSelectionMode() == QuizSourceSelectionMode.ALL_MATCHED) {
            return matchedQuestions;
        }

        if (source.getSelectionMode() == QuizSourceSelectionMode.RANDOM) {
            if (source.getQuestionCount() == null || source.getQuestionCount() <= 0) {
                throw new BusinessException("questionCount is required for RANDOM question bank sources");
            }
            if (matchedQuestions.size() < source.getQuestionCount()) {
                throw new BusinessException("Not enough questions in question bank source to satisfy questionCount");
            }
            List<BankQuestion> shuffled = new ArrayList<>(matchedQuestions);
            Collections.shuffle(shuffled);
            return shuffled.subList(0, source.getQuestionCount());
        }

        if (source.getSelectionMode() == QuizSourceSelectionMode.MANUAL) {
            List<Integer> selectedIds = parseIds(source.getManualQuestionIds());
            if (selectedIds.isEmpty()) {
                throw new BusinessException("manualQuestionIds is required for MANUAL question bank sources");
            }
            List<BankQuestion> selectedQuestions = bankQuestionRepository.findAllById(selectedIds);
            if (selectedQuestions.size() != selectedIds.size()) {
                throw new BusinessException("Some manually selected bank questions do not exist");
            }
            for (BankQuestion question : selectedQuestions) {
                if (!question.getQuestionBank().getId().equals(source.getQuestionBank().getId())) {
                    throw new BusinessException("Manual question selection must belong to the configured question bank");
                }
            }
            selectedQuestions.sort((left, right) ->
                    Integer.compare(selectedIds.indexOf(left.getId()), selectedIds.indexOf(right.getId())));
            return selectedQuestions;
        }

        throw new BusinessException("Unsupported question bank selection mode");
    }

    private void createManualQuestions(Quiz quiz, List<QuizQuestionRequest> questionRequests) {
        for (QuizQuestionRequest questionRequest : questionRequests) {
            QuizQuestion question = new QuizQuestion();
            question.setContent(questionRequest.getContent());
            question.setType(questionRequest.getType());
            question.setPoints(questionRequest.getPoints() != null ? questionRequest.getPoints() : 1);
            question.setFileUrl(questionRequest.getFileUrl());
            question.setEmbedUrl(questionRequest.getEmbedUrl());
            question.setCloudinaryId(questionRequest.getCloudinaryId());
            question.setQuiz(quiz);

            List<QuizAnswer> answers = new ArrayList<>();
            if (questionRequest.getAnswers() != null) {
                for (QuizAnswerRequest answerRequest : questionRequest.getAnswers()) {
                    QuizAnswer answer = new QuizAnswer();
                    answer.setContent(answerRequest.getContent());
                    answer.setIsCorrect(answerRequest.getIsCorrect() != null ? answerRequest.getIsCorrect() : false);
                    answer.setQuizQuestion(question);
                    answers.add(answer);
                }
            }
            question.setAnswers(answers);
            quizQuestionRepository.save(question);
        }
    }

    private void validateQuizStructureRequest(QuizRequest request) {
        boolean hasBankSources = request.getBankSources() != null && !request.getBankSources().isEmpty();
        boolean hasManualQuestions = request.getQuestions() != null && !request.getQuestions().isEmpty();
        if (hasBankSources && hasManualQuestions) {
            throw new BusinessException("Quiz can be configured either by manual questions or by question bank sources, not both");
        }
    }

    private QuizQuestionResponse convertQuestionToDTO(QuizQuestion question) {
        QuizQuestionResponse response = new QuizQuestionResponse();
        response.setId(question.getId());
        response.setSourceBankQuestionId(question.getSourceBankQuestion() != null ? question.getSourceBankQuestion().getId() : null);
        response.setContent(question.getContent());
        response.setType(question.getType());
        response.setFileUrl(question.getFileUrl());
        response.setEmbedUrl(question.getEmbedUrl());
        response.setCloudinaryId(question.getCloudinaryId());
        response.setPoints(question.getPoints());
        response.setAnswers(question.getAnswers() != null
                ? question.getAnswers().stream().map(this::convertAnswerToDTO).collect(Collectors.toList())
                : List.of());
        return response;
    }

    private QuizAnswerResponse convertAnswerToDTO(QuizAnswer answer) {
        QuizAnswerResponse response = new QuizAnswerResponse();
        response.setId(answer.getId());
        response.setContent(answer.getContent());
        response.setIsCorrect(answer.getIsCorrect());
        response.setFileUrl(answer.getFileUrl());
        response.setEmbedUrl(answer.getEmbedUrl());
        response.setCloudinaryId(answer.getCloudinaryId());
        return response;
    }

    private QuizBankSourceResponse convertBankSourceToDTO(QuizBankSource source) {
        QuizBankSourceResponse response = new QuizBankSourceResponse();
        response.setId(source.getId());
        response.setQuestionBankId(source.getQuestionBank() != null ? source.getQuestionBank().getId() : null);
        response.setQuestionBankName(source.getQuestionBank() != null ? source.getQuestionBank().getName() : null);
        response.setTagId(source.getTag() != null ? source.getTag().getId() : null);
        response.setTagName(source.getTag() != null ? source.getTag().getName() : null);
        response.setOrderIndex(source.getOrderIndex());
        response.setSelectionMode(source.getSelectionMode());
        response.setQuestionCount(source.getQuestionCount());
        response.setDifficultyLevel(source.getDifficultyLevel());
        response.setManualQuestionIds(parseIds(source.getManualQuestionIds()));
        return response;
    }

    private String joinIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private List<Integer> parseIds(String csv) {
        if (!StringUtils.hasText(csv)) {
            return List.of();
        }
        List<Integer> ids = new ArrayList<>();
        for (String value : csv.split(",")) {
            String trimmed = value.trim();
            if (StringUtils.hasText(trimmed)) {
                ids.add(Integer.valueOf(trimmed));
            }
        }
        return ids;
    }
}
