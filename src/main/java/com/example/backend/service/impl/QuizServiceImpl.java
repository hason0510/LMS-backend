package com.example.backend.service.impl;

import com.example.backend.constant.ContentItemType;
import com.example.backend.constant.QuestionInteractionItemRole;
import com.example.backend.constant.QuestionType;
import com.example.backend.constant.QuizSourceSelectionMode;
import com.example.backend.dto.request.quiz.QuestionInteractionItemRequest;
import com.example.backend.dto.request.quiz.QuizAnswerRequest;
import com.example.backend.dto.request.quiz.QuizBankSourceRequest;
import com.example.backend.dto.request.quiz.QuizQuestionRequest;
import com.example.backend.dto.request.quiz.QuizRequest;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.quiz.QuizAnswerResponse;
import com.example.backend.dto.response.quiz.QuizBankSourceResponse;
import com.example.backend.dto.response.quiz.QuestionInteractionItemResponse;
import com.example.backend.dto.response.quiz.QuizQuestionResponse;
import com.example.backend.dto.response.quiz.QuizResponse;
import com.example.backend.dto.response.ResourceResponse;
import com.example.backend.entity.quiz.BankQuestion;
import com.example.backend.entity.quiz.BankQuestionOption;
import com.example.backend.entity.ClassContentItem;
import com.example.backend.entity.ClassSection;
import com.example.backend.entity.quiz.QuestionBank;
import com.example.backend.entity.quiz.QuestionInteractionItem;
import com.example.backend.entity.quiz.QuestionTag;
import com.example.backend.entity.quiz.Quiz;
import com.example.backend.entity.quiz.QuizAnswer;
import com.example.backend.entity.quiz.QuizBankSource;
import com.example.backend.entity.quiz.QuizQuestion;
import com.example.backend.entity.Resource;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.BankQuestionRepository;
import com.example.backend.repository.ClassContentItemRepository;
import com.example.backend.repository.ClassSectionRepository;
import com.example.backend.repository.QuestionBankRepository;
import com.example.backend.repository.QuestionTagRepository;
import com.example.backend.repository.QuizBankSourceRepository;
import com.example.backend.repository.QuizQuestionRepository;
import com.example.backend.repository.QuizRepository;
import com.example.backend.repository.ResourceRepository;
import com.example.backend.service.QuizService;
import com.example.backend.service.ClassNotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class QuizServiceImpl implements QuizService {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizBankSourceRepository quizBankSourceRepository;
    private final QuestionBankRepository questionBankRepository;
    private final BankQuestionRepository bankQuestionRepository;
    private final QuestionTagRepository questionTagRepository;
    private final ClassSectionRepository classSectionRepository;
    private final ClassContentItemRepository classContentItemRepository;
    private final ResourceRepository resourceRepository;
    private final ClassNotificationService classNotificationService;

    public QuizServiceImpl(
            QuizRepository quizRepository,
            QuizQuestionRepository quizQuestionRepository,
            QuizBankSourceRepository quizBankSourceRepository,
            QuestionBankRepository questionBankRepository,
            BankQuestionRepository bankQuestionRepository,
            QuestionTagRepository questionTagRepository,
            ClassSectionRepository classSectionRepository,
            ClassContentItemRepository classContentItemRepository,
            ResourceRepository resourceRepository,
            ClassNotificationService classNotificationService
    ) {
        this.quizRepository = quizRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.quizBankSourceRepository = quizBankSourceRepository;
        this.questionBankRepository = questionBankRepository;
        this.bankQuestionRepository = bankQuestionRepository;
        this.questionTagRepository = questionTagRepository;
        this.classSectionRepository = classSectionRepository;
        this.classContentItemRepository = classContentItemRepository;
        this.resourceRepository = resourceRepository;
        this.classNotificationService = classNotificationService;
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
        response.setGenerateQuestionsPerAttempt(quiz.isGenerateQuestionsPerAttempt());
        response.setShuffleQuestions(quiz.isShuffleQuestions());
        response.setShuffleAnswers(quiz.isShuffleAnswers());
        response.setDisplayMode(quiz.getDisplayMode());
        response.setShowCorrectAnswer(quiz.isShowCorrectAnswer());
        response.setQuestionCount(resolveQuizQuestionCount(quiz, questions, bankSources));
        response.setClassSectionId(quiz.getClassSection() != null ? quiz.getClassSection().getId() : null);
        response.setClassContentItemId(
                classContentItemRepository.findByQuiz_Id(quiz.getId())
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
        notifyStudentsAboutNewQuiz(savedQuiz);
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
        if (isCreate || request.getGenerateQuestionsPerAttempt() != null) {
            quiz.setGenerateQuestionsPerAttempt(Boolean.TRUE.equals(request.getGenerateQuestionsPerAttempt()));
        }
        if (isCreate || request.getShuffleQuestions() != null) {
            quiz.setShuffleQuestions(Boolean.TRUE.equals(request.getShuffleQuestions()));
        }
        if (isCreate || request.getShuffleAnswers() != null) {
            quiz.setShuffleAnswers(Boolean.TRUE.equals(request.getShuffleAnswers()));
        }
        if (isCreate || request.getDisplayMode() != null) {
            quiz.setDisplayMode(StringUtils.hasText(request.getDisplayMode()) ? request.getDisplayMode() : "PAGINATION");
        }
        if (isCreate || request.getShowCorrectAnswer() != null) {
            quiz.setShowCorrectAnswer(Boolean.TRUE.equals(request.getShowCorrectAnswer()));
        }
        if (request.getClassSectionId() != null) {
            ClassSection classSection = classSectionRepository.findById(request.getClassSectionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
            quiz.setClassSection(classSection);
        }
    }

    private void notifyStudentsAboutNewQuiz(Quiz quiz) {
        if (quiz.getClassSection() == null) {
            return;
        }
        classNotificationService.notifyApprovedStudents(
                quiz.getClassSection(),
                "Quiz má»›i: " + quiz.getTitle(),
                "Lá»›p " + quiz.getClassSection().getTitle() + " vá»«a cÃ³ quiz má»›i.",
                "QUIZ_CREATED",
                quiz.getDescription(),
                "/class-sections/" + quiz.getClassSection().getId() + "/quizzes/" + quiz.getId() + "/detail",
                "QUIZ",
                quiz.getId(),
                "quiz-created"
        );
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

        classContentItemRepository.findByQuiz_Id(quiz.getId())
                .filter(existing -> !existing.getId().equals(classContentItem.getId()))
                .ifPresent(existing -> {
                    existing.setQuiz(null);
                    classContentItemRepository.save(existing);
                });

        classContentItem.setQuiz(quiz);
        if (!StringUtils.hasText(classContentItem.getTitle())) {
            classContentItem.setTitle(quiz.getTitle());
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
            if (quiz.isGenerateQuestionsPerAttempt()) {
                return;
            }
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
            // Keep historical attempts stable: detach questions instead of deleting them.
            question.setQuiz(null);
        }
        quizQuestionRepository.saveAll(existingQuestions);
    }

    private List<QuizBankSource> saveBankSources(Quiz quiz, List<QuizBankSourceRequest> sourceRequests) {
        List<QuizBankSource> savedSources = new ArrayList<>();
        for (int i = 0; i < sourceRequests.size(); i++) {
            QuizBankSourceRequest sourceRequest = sourceRequests.get(i);
            QuestionBank questionBank = questionBankRepository.findById(sourceRequest.getQuestionBankId())
                    .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));

            QuestionTag tag = null;
            if (sourceRequest.getTagId() != null) {
                tag = questionTagRepository.findById(sourceRequest.getTagId())
                        .orElseThrow(() -> new ResourceNotFoundException("Question tag not found"));
            }

            validateQuestionBankSource(quiz, questionBank, tag);

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

    private void validateQuestionBankSource(Quiz quiz, QuestionBank questionBank, QuestionTag tag) {
        if (questionBank.getSubject() == null) {
            throw new BusinessException("Question bank must belong to a subject");
        }

        if (tag != null
                && tag.getQuestionBank() != null
                && !Objects.equals(tag.getQuestionBank().getId(), questionBank.getId())) {
            throw new BusinessException("Question tag must belong to the same question bank");
        }

        if (quiz.getClassSection() != null
                && quiz.getClassSection().getSubject() != null
                && !Objects.equals(quiz.getClassSection().getSubject().getId(), questionBank.getSubject().getId())) {
            throw new BusinessException("Question bank subject does not match the quiz class section subject");
        }
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
            question.setResource(sourceQuestion.getResource());
            question.setPoints(sourceQuestion.getDefaultPoints() != null ? sourceQuestion.getDefaultPoints() : BigDecimal.ONE);

            List<QuizAnswer> answers = new ArrayList<>();
            if (sourceQuestion.getOptions() != null) {
                for (BankQuestionOption sourceOption : sourceQuestion.getOptions()) {
                    QuizAnswer answer = new QuizAnswer();
                    answer.setQuizQuestion(question);
                    answer.setContent(sourceOption.getContent());
                    answer.setIsCorrect(sourceOption.getIsCorrect());
                    answer.setResource(sourceOption.getResource());
                    answers.add(answer);
                }
            }
            question.setAnswers(answers);
            question.setInteractionItems(copyBankInteractionItems(sourceQuestion, question));
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
            validateInteractionItems(questionRequest.getType(), questionRequest.getItems());

            QuizQuestion question = new QuizQuestion();
            question.setContent(questionRequest.getContent());
            question.setType(questionRequest.getType());
            question.setPoints(questionRequest.getPoints() != null ? questionRequest.getPoints() : BigDecimal.ONE);
            if (questionRequest.getResourceId() != null) {
                question.setResource(resourceRepository.findById(questionRequest.getResourceId()).orElse(null));
            }
            question.setQuiz(quiz);

            List<QuizAnswer> answers = new ArrayList<>();
            if (!isInteractionQuestionType(questionRequest.getType()) && questionRequest.getAnswers() != null) {
                for (QuizAnswerRequest answerRequest : questionRequest.getAnswers()) {
                    QuizAnswer answer = new QuizAnswer();
                    answer.setContent(answerRequest.getContent());
                    answer.setIsCorrect(answerRequest.getIsCorrect() != null ? answerRequest.getIsCorrect() : false);
                    if (answerRequest.getResourceId() != null) {
                        answer.setResource(resourceRepository.findById(answerRequest.getResourceId()).orElse(null));
                    }
                    answer.setQuizQuestion(question);
                    answers.add(answer);
                }
            }
            question.setAnswers(answers);
            question.setInteractionItems(buildQuizInteractionItems(question, questionRequest.getItems()));
            quizQuestionRepository.save(question);
        }
    }

    private List<QuestionInteractionItem> copyBankInteractionItems(BankQuestion sourceQuestion, QuizQuestion question) {
        if (sourceQuestion.getInteractionItems() == null) {
            return List.of();
        }
        List<QuestionInteractionItem> items = new ArrayList<>();
        for (QuestionInteractionItem sourceItem : sourceQuestion.getInteractionItems()) {
            QuestionInteractionItem item = new QuestionInteractionItem();
            item.setQuizQuestion(question);
            copyInteractionItemFields(sourceItem, item);
            items.add(item);
        }
        return items;
    }

    private List<QuestionInteractionItem> buildQuizInteractionItems(
            QuizQuestion question,
            List<QuestionInteractionItemRequest> requests
    ) {
        List<QuestionInteractionItem> items = new ArrayList<>();
        int orderIndex = 1;
        if (requests != null) {
            for (QuestionInteractionItemRequest itemRequest : requests) {
                QuestionInteractionItem item = new QuestionInteractionItem();
                item.setQuizQuestion(question);
                item.setContent(resolveItemContent(itemRequest, orderIndex));
                item.setItemKey(resolveItemKey(itemRequest, orderIndex));
                item.setRole(itemRequest.getRole());
                item.setCorrectMatchKey(StringUtils.hasText(itemRequest.getCorrectMatchKey())
                        ? itemRequest.getCorrectMatchKey().trim()
                        : null);
                item.setCorrectOrderIndex(itemRequest.getCorrectOrderIndex());
                item.setBlankIndex(itemRequest.getBlankIndex());
                item.setAcceptedAnswers(joinAcceptedAnswers(itemRequest.getAcceptedAnswers()));
                if (itemRequest.getResourceId() != null) {
                    item.setResource(resourceRepository.findById(itemRequest.getResourceId()).orElse(null));
                }
                item.setOrderIndex(itemRequest.getOrderIndex() != null ? itemRequest.getOrderIndex() : orderIndex);
                items.add(item);
                orderIndex++;
            }
        }
        return items;
    }

    private void copyInteractionItemFields(QuestionInteractionItem sourceItem, QuestionInteractionItem targetItem) {
        targetItem.setContent(sourceItem.getContent());
        targetItem.setItemKey(sourceItem.getItemKey());
        targetItem.setRole(sourceItem.getRole());
        targetItem.setCorrectMatchKey(sourceItem.getCorrectMatchKey());
        targetItem.setCorrectOrderIndex(sourceItem.getCorrectOrderIndex());
        targetItem.setBlankIndex(sourceItem.getBlankIndex());
        targetItem.setAcceptedAnswers(sourceItem.getAcceptedAnswers());
        targetItem.setResource(sourceItem.getResource());
        targetItem.setOrderIndex(sourceItem.getOrderIndex());
    }

    private void validateInteractionItems(QuestionType type, List<QuestionInteractionItemRequest> items) {
        if (!isInteractionQuestionType(type)) {
            return;
        }

        List<QuestionInteractionItemRequest> safeItems = items != null ? items : List.of();
        if (isMatchingQuestionType(type)) {
            List<QuestionInteractionItemRequest> prompts = filterItemsByRole(safeItems, QuestionInteractionItemRole.PROMPT);
            List<QuestionInteractionItemRequest> matches = filterItemsByRole(safeItems, QuestionInteractionItemRole.MATCH);
            if (prompts.isEmpty() || matches.isEmpty()) {
                throw new BusinessException("MATCHING requires prompt and match items");
            }

            Set<String> matchKeys = new HashSet<>();
            for (QuestionInteractionItemRequest match : matches) {
                if (!hasContentOrResource(match) || !StringUtils.hasText(match.getItemKey())) {
                    throw new BusinessException("MATCHING match items require content and itemKey");
                }
                matchKeys.add(match.getItemKey().trim());
            }
            for (QuestionInteractionItemRequest prompt : prompts) {
                if (!hasContentOrResource(prompt) || !StringUtils.hasText(prompt.getCorrectMatchKey())) {
                    throw new BusinessException("MATCHING prompt items require content and correctMatchKey");
                }
                if (!matchKeys.contains(prompt.getCorrectMatchKey().trim())) {
                    throw new BusinessException("MATCHING correctMatchKey must reference a match itemKey");
                }
            }
            return;
        }

        if (type == QuestionType.DRAG_ORDER) {
            List<QuestionInteractionItemRequest> orderItems = filterItemsByRole(safeItems, QuestionInteractionItemRole.ORDER_ITEM);
            if (orderItems.size() < 2) {
                throw new BusinessException("DRAG_ORDER requires at least two order items");
            }

            Set<Integer> orderIndexes = new HashSet<>();
            for (QuestionInteractionItemRequest item : orderItems) {
                if (!StringUtils.hasText(item.getContent()) || item.getCorrectOrderIndex() == null) {
                    throw new BusinessException("DRAG_ORDER items require content and correctOrderIndex");
                }
                if (!orderIndexes.add(item.getCorrectOrderIndex())) {
                    throw new BusinessException("DRAG_ORDER correctOrderIndex values must be unique");
                }
            }
            return;
        }

        if (type == QuestionType.CLOZE) {
            List<QuestionInteractionItemRequest> blanks = filterItemsByRole(safeItems, QuestionInteractionItemRole.BLANK);
            if (blanks.isEmpty()) {
                throw new BusinessException("CLOZE requires at least one blank item");
            }

            Set<Integer> blankIndexes = new HashSet<>();
            for (QuestionInteractionItemRequest blank : blanks) {
                if (blank.getBlankIndex() == null) {
                    throw new BusinessException("CLOZE blank items require blankIndex");
                }
                if (!blankIndexes.add(blank.getBlankIndex())) {
                    throw new BusinessException("CLOZE blankIndex values must be unique");
                }
                if (normalizeAcceptedAnswers(blank.getAcceptedAnswers()).isEmpty()) {
                    throw new BusinessException("CLOZE blank items require acceptedAnswers");
                }
            }
        }
    }

    private List<QuestionInteractionItemRequest> filterItemsByRole(
            List<QuestionInteractionItemRequest> items,
            QuestionInteractionItemRole role
    ) {
        return items.stream()
                .filter(item -> item != null && item.getRole() == role)
                .toList();
    }

    private boolean isInteractionQuestionType(QuestionType type) {
        return isMatchingQuestionType(type)
                || type == QuestionType.DRAG_ORDER
                || type == QuestionType.CLOZE;
    }

    private boolean isMatchingQuestionType(QuestionType type) {
        return type == QuestionType.MATCHING
                || type == QuestionType.IMAGE_MATCHING;
    }

    private boolean hasContentOrResource(QuestionInteractionItemRequest item) {
        return item != null
                && (StringUtils.hasText(item.getContent()) || item.getResourceId() != null);
    }

    private String resolveItemContent(QuestionInteractionItemRequest itemRequest, int orderIndex) {
        if (itemRequest != null && StringUtils.hasText(itemRequest.getContent())) {
            return itemRequest.getContent().trim();
        }
        return itemRequest != null && itemRequest.getRole() == QuestionInteractionItemRole.BLANK
                ? "Blank " + orderIndex
                : "Item " + orderIndex;
    }

    private String resolveItemKey(QuestionInteractionItemRequest itemRequest, int orderIndex) {
        if (itemRequest != null && StringUtils.hasText(itemRequest.getItemKey())) {
            return itemRequest.getItemKey().trim();
        }
        String prefix = itemRequest != null && itemRequest.getRole() != null
                ? itemRequest.getRole().name().toLowerCase(Locale.ROOT)
                : "item";
        return prefix + "-" + orderIndex;
    }

    private String joinAcceptedAnswers(List<String> acceptedAnswers) {
        List<String> normalized = normalizeAcceptedAnswers(acceptedAnswers);
        return normalized.isEmpty() ? null : String.join("\n", normalized);
    }

    private List<String> normalizeAcceptedAnswers(List<String> acceptedAnswers) {
        if (acceptedAnswers == null) {
            return List.of();
        }
        return acceptedAnswers.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private void validateQuizStructureRequest(QuizRequest request) {
        boolean hasBankSources = request.getBankSources() != null && !request.getBankSources().isEmpty();
        boolean hasManualQuestions = request.getQuestions() != null && !request.getQuestions().isEmpty();
        if (hasBankSources && hasManualQuestions) {
            throw new BusinessException("Quiz can be configured either by manual questions or by question bank sources, not both");
        }
        if (Boolean.TRUE.equals(request.getGenerateQuestionsPerAttempt())
                && request.getBankSources() != null
                && request.getBankSources().isEmpty()) {
            throw new BusinessException("bankSources is required when generateQuestionsPerAttempt is enabled");
        }
    }

    private Integer resolveQuizQuestionCount(Quiz quiz, List<QuizQuestion> questions, List<QuizBankSource> bankSources) {
        if (!quiz.isGenerateQuestionsPerAttempt()) {
            return questions != null ? questions.size() : 0;
        }

        if (bankSources == null || bankSources.isEmpty()) {
            return questions != null ? questions.size() : 0;
        }

        int total = 0;
        java.util.HashSet<Integer> fixedQuestionIds = new java.util.HashSet<>();

        for (QuizBankSource source : bankSources) {
            if (source.getSelectionMode() == QuizSourceSelectionMode.RANDOM) {
                total += source.getQuestionCount() != null ? source.getQuestionCount() : 0;
                continue;
            }

            if (source.getSelectionMode() == QuizSourceSelectionMode.MANUAL) {
                for (Integer id : parseIds(source.getManualQuestionIds())) {
                    if (fixedQuestionIds.add(id)) {
                        total++;
                    }
                }
                continue;
            }

            if (source.getSelectionMode() == QuizSourceSelectionMode.ALL_MATCHED) {
                List<BankQuestion> matched = bankQuestionRepository.findSelectableQuestions(
                        source.getQuestionBank().getId(),
                        source.getDifficultyLevel(),
                        source.getTag() != null ? source.getTag().getId() : null
                );
                for (BankQuestion question : matched) {
                    if (fixedQuestionIds.add(question.getId())) {
                        total++;
                    }
                }
            }
        }

        return total;
    }

    private QuizQuestionResponse convertQuestionToDTO(QuizQuestion question) {
        QuizQuestionResponse response = new QuizQuestionResponse();
        response.setId(question.getId());
        response.setSourceBankQuestionId(question.getSourceBankQuestion() != null ? question.getSourceBankQuestion().getId() : null);
        response.setContent(question.getContent());
        response.setType(question.getType());
        response.setResource(convertResourceToDTO(question.getResource()));
        response.setPoints(question.getPoints());
        response.setAnswers(question.getAnswers() != null
                ? question.getAnswers().stream().map(this::convertAnswerToDTO).collect(Collectors.toList())
                : List.of());
        response.setItems(question.getInteractionItems() != null
                ? question.getInteractionItems().stream().map(item -> convertInteractionItemToDTO(item, true)).toList()
                : List.of());
        return response;
    }

    private QuestionInteractionItemResponse convertInteractionItemToDTO(
            QuestionInteractionItem item,
            boolean showCorrectAnswer
    ) {
        return new QuestionInteractionItemResponse(
                item.getId(),
                item.getContent(),
                item.getItemKey(),
                item.getRole(),
                showCorrectAnswer ? item.getCorrectMatchKey() : null,
                showCorrectAnswer ? item.getCorrectOrderIndex() : null,
                item.getBlankIndex(),
                showCorrectAnswer ? parseAcceptedAnswers(item.getAcceptedAnswers()) : null,
                item.getBlankType(),
                item.getBlankOptions(),
                item.getResource() != null ? item.getResource().getId() : null,
                item.getOrderIndex()
        );
    }

    private List<String> parseAcceptedAnswers(String acceptedAnswers) {
        if (!StringUtils.hasText(acceptedAnswers)) {
            return List.of();
        }
        return Pattern.compile("\\R")
                .splitAsStream(acceptedAnswers)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private QuizAnswerResponse convertAnswerToDTO(QuizAnswer answer) {
        QuizAnswerResponse response = new QuizAnswerResponse();
        response.setId(answer.getId());
        response.setContent(answer.getContent());
        response.setIsCorrect(answer.getIsCorrect());
        response.setResourceId(answer.getResource() != null ? answer.getResource().getId() : null);
        response.setResource(convertResourceToDTO(answer.getResource()));
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

    private ResourceResponse convertResourceToDTO(Resource resource) {
        if (resource == null) return null;
        ResourceResponse r = new ResourceResponse();
        r.setId(resource.getId());
        r.setTitle(resource.getTitle());
        r.setFileUrl(resource.getFileUrl());
        r.setEmbedUrl(resource.getEmbedUrl());
        r.setCloudinaryId(resource.getCloudinaryId());
        r.setHlsUrl(resource.getHlsUrl());
        r.setDescription(resource.getDescription());
        r.setMimeType(resource.getMimeType());
        r.setFileSize(resource.getFileSize());
        r.setType(resource.getType());
        r.setSource(resource.getSource());
        return r;
    }



}
