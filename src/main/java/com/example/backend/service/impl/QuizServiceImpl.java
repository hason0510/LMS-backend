package com.example.backend.service.impl;

import com.example.backend.constant.ContentItemType;
import com.example.backend.constant.ClassContentAvailabilityStatus;
import com.example.backend.constant.ClassSectionStatus;
import com.example.backend.constant.EnrollmentStatus;
import com.example.backend.constant.QuestionInteractionItemRole;
import com.example.backend.constant.QuestionType;
import com.example.backend.constant.QuizSourceSelectionMode;
import com.example.backend.constant.QuizTagMatchMode;
import com.example.backend.constant.ResourceScopeType;
import com.example.backend.constant.RoleType;
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
import com.example.backend.entity.Enrollment;
import com.example.backend.entity.quiz.QuestionBank;
import com.example.backend.entity.quiz.QuestionInteractionItem;
import com.example.backend.entity.quiz.QuestionTag;
import com.example.backend.entity.quiz.Quiz;
import com.example.backend.entity.quiz.QuizAnswer;
import com.example.backend.entity.quiz.QuizBankSource;
import com.example.backend.entity.quiz.QuizQuestion;
import com.example.backend.entity.Resource;
import com.example.backend.entity.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.repository.BankQuestionRepository;
import com.example.backend.repository.ClassContentItemRepository;
import com.example.backend.repository.ClassSectionRepository;
import com.example.backend.repository.EnrollmentRepository;
import com.example.backend.repository.QuestionBankRepository;
import com.example.backend.repository.QuestionTagRepository;
import com.example.backend.repository.QuizAttemptRepository;
import com.example.backend.repository.QuizBankSourceRepository;
import com.example.backend.repository.QuizQuestionRepository;
import com.example.backend.repository.QuizRepository;
import com.example.backend.repository.ResourceRepository;
import com.example.backend.service.QuizService;
import com.example.backend.service.ClassMemberAuthorizationService;
import com.example.backend.service.ClassContentAccessResult;
import com.example.backend.service.ClassContentAccessService;
import com.example.backend.service.ClassNotificationService;
import com.example.backend.service.ResourceAuthorizationService;
import com.example.backend.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class QuizServiceImpl implements QuizService {
    private static final Pattern CLOZE_TOKEN_PATTERN = Pattern.compile("\\[\\[([^\\]]+)\\]\\]");

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizBankSourceRepository quizBankSourceRepository;
    private final QuestionBankRepository questionBankRepository;
    private final BankQuestionRepository bankQuestionRepository;
    private final QuestionTagRepository questionTagRepository;
    private final ClassSectionRepository classSectionRepository;
    private final ClassContentItemRepository classContentItemRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ResourceRepository resourceRepository;
    private final ClassNotificationService classNotificationService;
    private final UserService userService;
    private final ClassMemberAuthorizationService classMemberAuthorizationService;
    private final ClassContentAccessService classContentAccessService;
    private final ResourceAuthorizationService resourceAuthorizationService;

    public QuizServiceImpl(
            QuizRepository quizRepository,
            QuizQuestionRepository quizQuestionRepository,
            QuizAttemptRepository quizAttemptRepository,
            QuizBankSourceRepository quizBankSourceRepository,
            QuestionBankRepository questionBankRepository,
            BankQuestionRepository bankQuestionRepository,
            QuestionTagRepository questionTagRepository,
            ClassSectionRepository classSectionRepository,
            ClassContentItemRepository classContentItemRepository,
            EnrollmentRepository enrollmentRepository,
            ResourceRepository resourceRepository,
            ClassNotificationService classNotificationService,
            UserService userService,
            ClassMemberAuthorizationService classMemberAuthorizationService,
            ClassContentAccessService classContentAccessService,
            ResourceAuthorizationService resourceAuthorizationService
    ) {
        this.quizRepository = quizRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.quizBankSourceRepository = quizBankSourceRepository;
        this.questionBankRepository = questionBankRepository;
        this.bankQuestionRepository = bankQuestionRepository;
        this.questionTagRepository = questionTagRepository;
        this.classSectionRepository = classSectionRepository;
        this.classContentItemRepository = classContentItemRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.resourceRepository = resourceRepository;
        this.classNotificationService = classNotificationService;
        this.userService = userService;
        this.classMemberAuthorizationService = classMemberAuthorizationService;
        this.classContentAccessService = classContentAccessService;
        this.resourceAuthorizationService = resourceAuthorizationService;
    }

    @Override
    public QuizResponse convertQuizToDTO(Quiz quiz) {
        List<QuizQuestion> questions = quizQuestionRepository.findByQuiz_IdOrderByIdAsc(quiz.getId());
        List<QuizBankSource> bankSources = quizBankSourceRepository.findByQuiz_IdOrderByOrderIndexAsc(quiz.getId());
        ClassContentItem classContentItem = classContentItemRepository.findByQuiz_Id(quiz.getId()).orElse(null);
        ClassSection classSection = classContentItem != null && classContentItem.getClassChapter() != null
                ? classContentItem.getClassChapter().getClassSection()
                : resolveClassSectionForQuiz(quiz);
        boolean showCorrectAnswer = shouldShowCorrectAnswersForCurrentUser();

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
        response.setClassSectionId(classSection != null ? classSection.getId() : null);
        response.setClassContentItemId(classContentItem != null ? classContentItem.getId() : null);
        response.setBankSources(bankSources.stream().map(this::convertBankSourceToDTO).toList());
        response.setQuestions(questions.stream()
                .map(question -> convertQuestionToDTO(question, showCorrectAnswer))
                .collect(Collectors.toList()));
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
        return getQuizById(id, null);
    }

    @Override
    public QuizResponse getQuizById(Integer id, Integer classContentItemId) {
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + id));
        if (classContentItemId != null) {
            validateQuizAccessByClassContentItem(quiz, classContentItemId);
        } else {
            requireViewPermission(resolveClassSectionForQuiz(quiz));
        }
        return convertQuizToDTO(quiz);
    }

    @Override
    public QuizResponse getQuizPreviewSample(Integer id, Long seed) {
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + id));
        requireEditContentPermission(resolveClassSectionForQuiz(quiz));

        QuizResponse response = convertQuizToDTO(quiz);
        long effectiveSeed = seed != null ? seed : (10_000L + id);
        Random random = new Random(effectiveSeed);
        List<QuizQuestionResponse> previewQuestions = buildPreviewQuestions(quiz, random);

        response.setQuestions(previewQuestions);
        response.setQuestionCount(previewQuestions.size());
        return response;
    }

    @Override
    @Transactional
    public QuizResponse createQuiz(QuizRequest request) {
        validateQuizStructureRequest(request);
        ClassSection targetClassSection = resolveTargetClassSection(request);
        requireEditContentPermission(targetClassSection);

        Quiz quiz = new Quiz();
        applyQuizMetadata(quiz, request, true);
        Quiz savedQuiz = quizRepository.save(quiz);
        syncQuizPlacement(savedQuiz, request);
        syncQuizContent(savedQuiz, request);
        notifyStudentsAboutNewQuiz(savedQuiz, targetClassSection);
        return convertQuizToDTO(savedQuiz);
    }

    @Override
    @Transactional
    public QuizResponse updateQuiz(Integer id, QuizRequest request) {
        validateQuizStructureRequest(request);

        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));
        ClassSection currentClassSection = resolveClassSectionForQuiz(quiz);
        ClassSection requestedClassSection = resolveTargetClassSection(request);
        requireEditContentPermission(requestedClassSection != null ? requestedClassSection : currentClassSection);
        ensureQuizHasNoAttempts(quiz);
        String previousTitle = quiz.getTitle();
        applyQuizMetadata(quiz, request, false);
        Quiz savedQuiz = quizRepository.save(quiz);
        syncQuizPlacement(savedQuiz, request);
        syncLinkedClassContentItemTitle(savedQuiz, previousTitle);
        syncQuizContent(savedQuiz, request);
        return convertQuizToDTO(savedQuiz);
    }

    @Transactional
    @Override
    public void deleteQuiz(Integer id) {
        Quiz quiz = quizRepository.findById(id).orElseThrow();
        requireEditContentPermission(resolveClassSectionForQuiz(quiz));
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
    }

    private void notifyStudentsAboutNewQuiz(Quiz quiz, ClassSection targetClassSection) {
        ClassSection classSection = targetClassSection != null ? targetClassSection : resolveClassSectionForQuiz(quiz);
        if (classSection == null) {
            return;
        }
        classNotificationService.notifyApprovedStudents(
                classSection,
                "Quiz mới: " + quiz.getTitle(),
                "Lớp " + classSection.getTitle() + " vừa có quiz mới.",
                "QUIZ_CREATED",
                quiz.getDescription(),
                "/class-sections/" + classSection.getId() + "/quizzes/" + quiz.getId() + "/detail",
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
        ClassSection currentClassSection = resolveClassSectionForQuiz(quiz);
        if (currentClassSection != null && !Objects.equals(currentClassSection.getId(), classSection.getId())) {
            throw new BusinessException("Quiz classSection does not match the target class content item");
        }

        classContentItemRepository.findByQuiz_Id(quiz.getId())
                .filter(existing -> !existing.getId().equals(classContentItem.getId()))
                .ifPresent(existing -> {
                    throw new BusinessException("Quiz is already linked to another class content item");
                });
        if (classContentItem.getQuiz() != null && !classContentItem.getQuiz().getId().equals(quiz.getId())) {
            throw new BusinessException("Class content item is already linked to another quiz");
        }

        classContentItem.setQuiz(quiz);
        if (!StringUtils.hasText(classContentItem.getTitle())) {
            classContentItem.setTitle(quiz.getTitle());
        }
        classContentItemRepository.save(classContentItem);
    }

    private void validateQuizAccessByClassContentItem(Quiz quiz, Integer classContentItemId) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Unauthorized");
        }

        ClassContentItem classContentItem = classContentItemRepository.findById(classContentItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Class content item not found"));
        if (classContentItem.getItemType() != ContentItemType.QUIZ
                || classContentItem.getQuiz() == null
                || !classContentItem.getQuiz().getId().equals(quiz.getId())) {
            throw new UnauthorizedException("Noi dung quiz khong hop le");
        }

        ClassContentAccessResult accessResult = classContentAccessService.evaluateForUser(classContentItem, currentUser);
        if (accessResult.accessible()) {
            return;
        }
        if (accessResult.availabilityStatus() == ClassContentAvailabilityStatus.NO_ENROLLMENT) {
            throw new UnauthorizedException(accessResult.message());
        }
        throw new BusinessException(accessResult.message() != null
                ? accessResult.message()
                : "Ban khong co quyen truy cap noi dung nay");
    }

    private void syncLinkedClassContentItemTitle(Quiz quiz, String previousQuizTitle) {
        classContentItemRepository.findByQuiz_Id(quiz.getId()).ifPresent(classContentItem -> {
            String itemTitle = classContentItem.getTitle();
            if (!StringUtils.hasText(itemTitle) || Objects.equals(itemTitle, previousQuizTitle)) {
                classContentItem.setTitle(quiz.getTitle());
                classContentItemRepository.save(classContentItem);
            }
        });
    }

    private void syncQuizContent(Quiz quiz, QuizRequest request) {
        boolean hasBankSourcesField = request.getBankSources() != null;
        boolean hasQuestionsField = request.getQuestions() != null;
        if (!hasBankSourcesField && !hasQuestionsField) {
            return;
        }

        clearQuizSourcesAndQuestions(quiz);

        if (request.getBankSources() != null && !request.getBankSources().isEmpty()) {
            if (!quiz.isGenerateQuestionsPerAttempt()) {
                quiz.setGenerateQuestionsPerAttempt(true);
                quizRepository.save(quiz);
            }
            saveBankSources(quiz, request.getBankSources());
            return;
        }

        if (request.getQuestions() != null && !request.getQuestions().isEmpty()) {
            createManualQuestions(quiz, request.getQuestions());
        }
    }

    private void ensureQuizHasNoAttempts(Quiz quiz) {
        if (quiz != null && quiz.getId() != null && quizAttemptRepository.existsByQuiz_Id(quiz.getId())) {
            throw new BusinessException("Đã có người dùng làm bài, bạn không thể sửa Quiz.");
        }
    }

    private void clearQuizSourcesAndQuestions(Quiz quiz) {
        quizBankSourceRepository.deleteAll(quizBankSourceRepository.findByQuiz_IdOrderByOrderIndexAsc(quiz.getId()));

        List<QuizQuestion> existingQuestions = quizQuestionRepository.findByQuiz_IdOrderByIdAsc(quiz.getId());
        // Soft delete the old quiz questions so the rows stay in DB for history,
        // but disappear from normal queries via @SQLRestriction.
        quizQuestionRepository.deleteAll(existingQuestions);
    }

    private List<QuizBankSource> saveBankSources(Quiz quiz, List<QuizBankSourceRequest> sourceRequests) {
        List<QuizBankSource> savedSources = new ArrayList<>();
        for (int i = 0; i < sourceRequests.size(); i++) {
            QuizBankSourceRequest sourceRequest = sourceRequests.get(i);
            if (sourceRequest.getQuestionBankId() == null) {
                throw new BusinessException("questionBankId is required for each question bank source");
            }
            QuestionBank questionBank = questionBankRepository.findById(sourceRequest.getQuestionBankId())
                    .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));

            List<Integer> tagIds = normalizeTagIds(sourceRequest.getTagIds());
            List<QuestionTag> tags = resolveTags(tagIds);

            validateQuestionBankSource(quiz, questionBank, tags, sourceRequest);

            QuizBankSource source = new QuizBankSource();
            source.setQuiz(quiz);
            source.setQuestionBank(questionBank);
            source.setOrderIndex(i + 1);
            source.setSelectionMode(sourceRequest.getSelectionMode());
            source.setQuestionCount(sourceRequest.getQuestionCount());
            source.setDifficultyLevel(sourceRequest.getDifficultyLevel());
            source.setTags(new ArrayList<>(tags));
            source.setTagMatchMode(sourceRequest.getTagMatchMode() != null ? sourceRequest.getTagMatchMode() : QuizTagMatchMode.ANY);
            savedSources.add(quizBankSourceRepository.save(source));
        }
        return savedSources;
    }

    private void validateQuestionBankSource(
            Quiz quiz,
            QuestionBank questionBank,
            List<QuestionTag> tags,
            QuizBankSourceRequest sourceRequest
    ) {
        if (questionBank.getSubject() == null) {
            throw new BusinessException("Question bank must belong to a subject");
        }

        if (sourceRequest.getSelectionMode() == null) {
            throw new BusinessException("selectionMode is required for question bank sources");
        }
        if (sourceRequest.getSelectionMode() == QuizSourceSelectionMode.RANDOM
                && (sourceRequest.getQuestionCount() == null || sourceRequest.getQuestionCount() <= 0)) {
            throw new BusinessException("questionCount is required for RANDOM question bank sources");
        }

        for (QuestionTag tag : tags) {
            if (tag.getQuestionBank() != null
                    && !Objects.equals(tag.getQuestionBank().getId(), questionBank.getId())) {
                throw new BusinessException("Question tag must belong to the same question bank");
            }
        }

        ClassSection classSection = resolveClassSectionForQuiz(quiz);
        if (classSection != null
                && classSection.getSubject() != null
                && !Objects.equals(classSection.getSubject().getId(), questionBank.getSubject().getId())) {
            throw new BusinessException("Question bank subject does not match the quiz class section subject");
        }
    }

    private List<BankQuestion> resolveMatchedQuestions(QuizBankSource source) {
        List<BankQuestion> candidates = bankQuestionRepository.findSelectableQuestions(
                source.getQuestionBank().getId(),
                source.getDifficultyLevel(),
                null
        );
        List<Integer> tagIds = toTagIds(source.getTags());
        if (tagIds.isEmpty()) {
            return candidates;
        }

        final Set<Integer> selectedTagIds = new HashSet<>(tagIds);
        final Predicate<Set<Integer>> matcher = source.getTagMatchMode() == QuizTagMatchMode.ALL
                ? questionTagIds -> questionTagIds.containsAll(selectedTagIds)
                : questionTagIds -> questionTagIds.stream().anyMatch(selectedTagIds::contains);

        return candidates.stream()
                .filter(question -> {
                    Set<Integer> questionTagIds = question.getTagMappings() == null ? Set.of()
                            : question.getTagMappings().stream()
                            .filter(mapping -> mapping != null && mapping.getTag() != null && mapping.getTag().getId() != null)
                            .map(mapping -> mapping.getTag().getId())
                            .collect(Collectors.toSet());
                    return matcher.test(questionTagIds);
                })
                .toList();
    }

    private List<QuizQuestionResponse> buildPreviewQuestions(Quiz quiz, Random random) {
        List<QuizBankSource> bankSources = quizBankSourceRepository.findByQuiz_IdOrderByOrderIndexAsc(quiz.getId());
        if (!bankSources.isEmpty()) {
            return buildPreviewQuestionsFromBankSources(quiz, bankSources, random);
        }
        return buildPreviewQuestionsFromManual(quiz, random);
    }

    private List<QuizQuestionResponse> buildPreviewQuestionsFromManual(Quiz quiz, Random random) {
        List<QuizQuestion> questions = quizQuestionRepository.findByQuiz_IdOrderByIdAsc(quiz.getId());
        List<QuizQuestionResponse> responses = questions.stream()
                .map(question -> convertQuestionToDTO(question, true))
                .collect(Collectors.toCollection(ArrayList::new));

        if (quiz.isShuffleAnswers()) {
            for (QuizQuestionResponse question : responses) {
                if (question.getAnswers() != null && question.getAnswers().size() > 1) {
                    Collections.shuffle(question.getAnswers(), random);
                }
            }
        }

        if (quiz.isShuffleQuestions()) {
            Collections.shuffle(responses, random);
        }
        return responses;
    }

    private List<QuizQuestionResponse> buildPreviewQuestionsFromBankSources(
            Quiz quiz,
            List<QuizBankSource> bankSources,
            Random random
    ) {
        LinkedHashMap<Integer, BankQuestion> selectedQuestions = new LinkedHashMap<>();
        HashSet<Integer> excludedIds = new HashSet<>();

        for (QuizBankSource source : bankSources) {
            List<BankQuestion> selectedForSource = selectPreviewQuestionsForSource(source, random, excludedIds);
            for (BankQuestion question : selectedForSource) {
                if (selectedQuestions.putIfAbsent(question.getId(), question) == null) {
                    excludedIds.add(question.getId());
                }
            }
        }

        List<QuizQuestionResponse> responses = selectedQuestions.values().stream()
                .map(question -> convertBankQuestionToPreviewDTO(question, true))
                .collect(Collectors.toCollection(ArrayList::new));

        if (quiz.isShuffleAnswers()) {
            for (QuizQuestionResponse question : responses) {
                if (question.getAnswers() != null && question.getAnswers().size() > 1) {
                    Collections.shuffle(question.getAnswers(), random);
                }
            }
        }
        if (quiz.isShuffleQuestions()) {
            Collections.shuffle(responses, random);
        }
        return responses;
    }

    private List<BankQuestion> selectPreviewQuestionsForSource(
            QuizBankSource source,
            Random random,
            Set<Integer> excludedQuestionIds
    ) {
        List<BankQuestion> matchedQuestions = resolveMatchedQuestions(source);
        if (source.getSelectionMode() == QuizSourceSelectionMode.ALL_MATCHED) {
            if (excludedQuestionIds == null || excludedQuestionIds.isEmpty()) {
                return matchedQuestions;
            }
            return matchedQuestions.stream()
                    .filter(q -> !excludedQuestionIds.contains(q.getId()))
                    .toList();
        }

        if (source.getSelectionMode() == QuizSourceSelectionMode.RANDOM) {
            if (source.getQuestionCount() == null || source.getQuestionCount() <= 0) {
                throw new BusinessException("questionCount is required for RANDOM question bank sources");
            }
            List<BankQuestion> candidates = matchedQuestions.stream()
                    .filter(q -> excludedQuestionIds == null || !excludedQuestionIds.contains(q.getId()))
                    .collect(Collectors.toCollection(ArrayList::new));
            if (candidates.size() < source.getQuestionCount()) {
                throw new BusinessException("Not enough questions in question bank source to satisfy questionCount");
            }
            Collections.shuffle(candidates, random);
            return candidates.subList(0, source.getQuestionCount());
        }

        return List.of();
    }

    private QuizQuestionResponse convertBankQuestionToPreviewDTO(BankQuestion question, boolean showCorrectAnswer) {
        QuizQuestionResponse response = new QuizQuestionResponse();
        response.setId(question.getId());
        response.setSourceBankQuestionId(question.getId());
        response.setContent(sanitizeQuestionContent(question.getType(), question.getContent(), showCorrectAnswer));
        response.setType(question.getType());
        response.setResource(convertResourceToDTO(question.getResource()));
        response.setPoints(question.getDefaultPoints() != null ? question.getDefaultPoints() : BigDecimal.ONE);
        response.setAnswers(question.getOptions() != null
                ? question.getOptions().stream()
                .map(option -> convertBankAnswerToDTO(option, showCorrectAnswer))
                .collect(Collectors.toCollection(ArrayList::new))
                : new ArrayList<>());
        response.setItems(question.getInteractionItems() != null
                ? question.getInteractionItems().stream()
                .map(item -> convertInteractionItemToDTO(item, showCorrectAnswer))
                .toList()
                : List.of());
        return response;
    }

    private QuizAnswerResponse convertBankAnswerToDTO(BankQuestionOption answer, boolean showCorrectAnswer) {
        QuizAnswerResponse response = new QuizAnswerResponse();
        response.setId(answer.getId());
        response.setContent(answer.getContent());
        response.setIsCorrect(showCorrectAnswer ? answer.getIsCorrect() : null);
        response.setExplanation(showCorrectAnswer ? answer.getExplanation() : null);
        response.setResourceId(answer.getResource() != null ? answer.getResource().getId() : null);
        response.setResource(convertResourceToDTO(answer.getResource()));
        return response;
    }

    private void createManualQuestions(Quiz quiz, List<QuizQuestionRequest> questionRequests) {
        for (QuizQuestionRequest questionRequest : questionRequests) {
            validateInteractionItems(questionRequest.getType(), questionRequest.getItems());

            QuizQuestion question = new QuizQuestion();
            if (questionRequest.getSourceBankQuestionId() != null) {
                BankQuestion sourceQuestion = bankQuestionRepository.findById(questionRequest.getSourceBankQuestionId())
                        .orElseThrow(() -> new ResourceNotFoundException("Source bank question not found"));
                question.setSourceBankQuestion(sourceQuestion);
            }
            question.setContent(questionRequest.getContent());
            question.setType(questionRequest.getType());
            question.setPoints(questionRequest.getPoints() != null ? questionRequest.getPoints() : BigDecimal.ONE);
            if (questionRequest.getResourceId() != null) {
                question.setResource(resolveUsableQuizResource(quiz, questionRequest.getResourceId()));
            }
            question.setQuiz(quiz);

            List<QuizAnswer> answers = new ArrayList<>();
            if (!isInteractionQuestionType(questionRequest.getType()) && questionRequest.getAnswers() != null) {
                for (QuizAnswerRequest answerRequest : questionRequest.getAnswers()) {
                    QuizAnswer answer = new QuizAnswer();
                    answer.setContent(answerRequest.getContent());
                    answer.setIsCorrect(answerRequest.getIsCorrect() != null ? answerRequest.getIsCorrect() : false);
                    answer.setExplanation(answerRequest.getExplanation());
                    if (answerRequest.getResourceId() != null) {
                        answer.setResource(resolveUsableQuizResource(quiz, answerRequest.getResourceId()));
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
                item.setBlankType(StringUtils.hasText(itemRequest.getBlankType()) ? itemRequest.getBlankType().trim() : "TEXT_INPUT");
                item.setBlankOptions(StringUtils.hasText(itemRequest.getBlankOptions()) ? itemRequest.getBlankOptions().trim() : null);
                if (itemRequest.getResourceId() != null) {
                    item.setResource(resolveUsableQuizResource(question.getQuiz(), itemRequest.getResourceId()));
                }
                item.setOrderIndex(itemRequest.getOrderIndex() != null ? itemRequest.getOrderIndex() : orderIndex);
                items.add(item);
                orderIndex++;
            }
        }
        return items;
    }

    private Resource resolveUsableQuizResource(Quiz quiz, Integer resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        ClassSection classSection = resolveClassSectionForQuiz(quiz);
        Integer classSectionId = classSection != null ? classSection.getId() : null;
        ResourceScopeType expectedScope = classSectionId != null ? ResourceScopeType.CLASS_SECTION : null;
        resourceAuthorizationService.assertCanUse(resource, expectedScope, classSectionId);
        resource.setLastUsedAt(LocalDateTime.now());
        resourceRepository.save(resource);
        return resource;
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
        if (Boolean.TRUE.equals(request.getGenerateQuestionsPerAttempt()) && !hasBankSources) {
            throw new BusinessException("bankSources is required when generateQuestionsPerAttempt is enabled");
        }
    }

    private ClassSection resolveTargetClassSection(QuizRequest request) {
        if (request == null) {
            return null;
        }
        if (request.getClassContentItemId() != null) {
            ClassContentItem classContentItem = classContentItemRepository.findById(request.getClassContentItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Class content item not found"));
            return classContentItem.getClassChapter() != null ? classContentItem.getClassChapter().getClassSection() : null;
        }
        if (request.getClassSectionId() != null) {
            return classSectionRepository.findById(request.getClassSectionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        }
        return null;
    }

    private ClassSection resolveClassSectionForQuiz(Quiz quiz) {
        if (quiz == null || quiz.getId() == null) {
            return null;
        }
        return classContentItemRepository.findByQuiz_Id(quiz.getId())
                .map(ClassContentItem::getClassChapter)
                .map(classChapter -> classChapter.getClassSection())
                .orElse(null);
    }

    private void requireEditContentPermission(ClassSection classSection) {
        if (classSection == null) {
            return;
        }
        ensureClassSectionInteractive(classSection);
        User currentUser = requireCurrentUser();
        if (classMemberAuthorizationService.hasCapability(
                classSection,
                currentUser,
                ClassMemberAuthorizationService.CAP_EDIT_CONTENT
        )) {
            return;
        }
        throw new UnauthorizedException("You do not have permission to edit content in this class section");
    }

    private void requireViewPermission(ClassSection classSection) {
        if (classSection == null) {
            if (requireCurrentUser().getRole().getRoleName() == RoleType.STUDENT) {
                throw new UnauthorizedException("You do not have permission to access this quiz");
            }
            return;
        }
        User currentUser = requireCurrentUser();
        if (canViewClassSection(classSection, currentUser)) {
            return;
        }
        throw new UnauthorizedException("You do not have permission to access this class section");
    }

    private boolean canViewClassSection(ClassSection classSection, User currentUser) {
        if (classSection == null || currentUser == null) {
            return false;
        }
        if (classMemberAuthorizationService.isTeacherOrTa(classSection, currentUser)) {
            return true;
        }
        if (currentUser.getRole().getRoleName() != RoleType.STUDENT) {
            return false;
        }
        return enrollmentRepository.existsByStudent_IdAndClassSection_IdAndApprovalStatus(
                currentUser.getId(),
                classSection.getId(),
                EnrollmentStatus.APPROVED
        );
    }

    private void ensureClassSectionInteractive(ClassSection classSection) {
        if (classSection != null && classSection.getStatus() == ClassSectionStatus.ARCHIVED) {
            throw new BusinessException("Class section is archived and only supports read-only access");
        }
    }

    private User requireCurrentUser() {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        return currentUser;
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

            if (source.getSelectionMode() == QuizSourceSelectionMode.ALL_MATCHED) {
                List<BankQuestion> matched = resolveMatchedQuestions(source);
                for (BankQuestion question : matched) {
                    if (fixedQuestionIds.add(question.getId())) {
                        total++;
                    }
                }
            }
        }

        return total;
    }

    private QuizQuestionResponse convertQuestionToDTO(QuizQuestion question, boolean showCorrectAnswer) {
        QuizQuestionResponse response = new QuizQuestionResponse();
        response.setId(question.getId());
        response.setSourceBankQuestionId(question.getSourceBankQuestion() != null ? question.getSourceBankQuestion().getId() : null);
        response.setContent(sanitizeQuestionContent(question.getType(), question.getContent(), showCorrectAnswer));
        response.setType(question.getType());
        response.setResource(convertResourceToDTO(question.getResource()));
        response.setPoints(question.getPoints());
        response.setAnswers(question.getAnswers() != null
                ? question.getAnswers().stream()
                .map(answer -> convertAnswerToDTO(answer, showCorrectAnswer))
                .collect(Collectors.toList())
                : List.of());
        response.setItems(question.getInteractionItems() != null
                ? question.getInteractionItems().stream()
                .map(item -> convertInteractionItemToDTO(item, showCorrectAnswer))
                .toList()
                : List.of());
        return response;
    }

    private QuestionInteractionItemResponse convertInteractionItemToDTO(
            QuestionInteractionItem item,
            boolean showCorrectAnswer
    ) {
        return new QuestionInteractionItemResponse(
                item.getId(),
                resolveInteractionItemContent(item.getRole(), item.getContent(), showCorrectAnswer),
                item.getItemKey(),
                item.getRole(),
                showCorrectAnswer ? item.getCorrectMatchKey() : null,
                showCorrectAnswer ? item.getCorrectOrderIndex() : null,
                item.getBlankIndex(),
                showCorrectAnswer ? parseAcceptedAnswers(item.getAcceptedAnswers()) : null,
                item.getBlankType(),
                item.getBlankOptions(),
                item.getResource() != null ? item.getResource().getId() : null,
                convertResourceToDTO(item.getResource()),
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

    private QuizAnswerResponse convertAnswerToDTO(QuizAnswer answer, boolean showCorrectAnswer) {
        QuizAnswerResponse response = new QuizAnswerResponse();
        response.setId(answer.getId());
        response.setContent(answer.getContent());
        response.setExplanation(showCorrectAnswer ? answer.getExplanation() : null);
        response.setIsCorrect(showCorrectAnswer ? answer.getIsCorrect() : null);
        response.setResourceId(answer.getResource() != null ? answer.getResource().getId() : null);
        response.setResource(convertResourceToDTO(answer.getResource()));
        return response;
    }

    private boolean shouldShowCorrectAnswersForCurrentUser() {
        try {
            return userService.getCurrentUser().getRole().getRoleName() != RoleType.STUDENT;
        } catch (Exception ex) {
            return true;
        }
    }

    private String resolveInteractionItemContent(
            QuestionInteractionItemRole role,
            String content,
            boolean showCorrectAnswer
    ) {
        if (role == QuestionInteractionItemRole.BLANK && !showCorrectAnswer) {
            return null;
        }
        return content;
    }

    private String sanitizeQuestionContent(QuestionType type, String content, boolean showCorrectAnswer) {
        if (type != QuestionType.CLOZE || showCorrectAnswer || !StringUtils.hasText(content)) {
            return content;
        }
        var matcher = CLOZE_TOKEN_PATTERN.matcher(content);
        StringBuffer sanitized = new StringBuffer();
        int blankIndex = 1;
        while (matcher.find()) {
            matcher.appendReplacement(sanitized, Matcher.quoteReplacement("[[blank_" + blankIndex + "]]"));
            blankIndex += 1;
        }
        matcher.appendTail(sanitized);
        return sanitized.toString();
    }

    private QuizBankSourceResponse convertBankSourceToDTO(QuizBankSource source) {
        QuizBankSourceResponse response = new QuizBankSourceResponse();
        response.setId(source.getId());
        response.setQuestionBankId(source.getQuestionBank() != null ? source.getQuestionBank().getId() : null);
        response.setQuestionBankName(source.getQuestionBank() != null ? source.getQuestionBank().getName() : null);
        response.setTagIds(toTagIds(source.getTags()));
        response.setTagMatchMode(source.getTagMatchMode());
        response.setOrderIndex(source.getOrderIndex());
        response.setSelectionMode(source.getSelectionMode());
        response.setQuestionCount(source.getQuestionCount());
        response.setDifficultyLevel(source.getDifficultyLevel());
        return response;
    }

    private List<Integer> normalizeTagIds(List<Integer> tagIds) {
        LinkedHashMap<Integer, Integer> dedup = new LinkedHashMap<>();
        if (tagIds != null) {
            for (Integer id : tagIds) {
                if (id != null) dedup.put(id, id);
            }
        }
        return new ArrayList<>(dedup.values());
    }

    private List<QuestionTag> resolveTags(List<Integer> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        List<QuestionTag> tags = questionTagRepository.findAllById(tagIds);
        if (tags.size() != tagIds.size()) {
            throw new ResourceNotFoundException("One or more question tags not found");
        }
        return tags;
    }

    private List<Integer> toTagIds(List<QuestionTag> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        return tags.stream()
                .filter(Objects::nonNull)
                .map(QuestionTag::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
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
