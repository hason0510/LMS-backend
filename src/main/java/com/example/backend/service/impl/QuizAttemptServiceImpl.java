package com.example.backend.service.impl;

import com.example.backend.utils.ClassSectionGuard;

import com.example.backend.cache.CacheNames;
import com.example.backend.cache.RedisCacheInvalidationService;
import com.example.backend.constant.*;
import com.example.backend.dto.request.quiz.QuizAttemptAnswerItemRequest;
import com.example.backend.dto.request.quiz.QuizAttemptAnswerRequest;
import com.example.backend.dto.request.quiz.QuizAttemptReviewRequest;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.ResourceResponse;
import com.example.backend.dto.response.quiz.*;
import com.example.backend.entity.*;
import com.example.backend.entity.quiz.*;
import com.example.backend.entity.resource.Resource;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.repository.*;
import com.example.backend.service.ClassMemberAuthorizationService;
import com.example.backend.service.ClassContentAccessResult;
import com.example.backend.service.ClassContentAccessService;
import com.example.backend.service.EnrollmentService;
import com.example.backend.service.QuizAttemptService;
import com.example.backend.service.UserService;
import com.example.backend.specification.QuizAttemptSpecification;
import com.example.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizAttemptServiceImpl implements QuizAttemptService {
    private static final Pattern CLOZE_TOKEN_PATTERN = Pattern.compile("\\[\\[([^\\]]+)\\]\\]");

    private final QuizRepository quizRepository;
    private final UserService userService;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizAttemptAnswerRepository quizAttemptAnswerRepository;
    private final QuizAttemptQuestionRepository quizAttemptQuestionRepository;
    private final QuizAttemptQuestionOptionRepository quizAttemptQuestionOptionRepository;
    private final QuizAttemptQuestionItemRepository quizAttemptQuestionItemRepository;
    private final QuizAttemptAnswerItemRepository quizAttemptAnswerItemRepository;
    private final QuizBankSourceRepository quizBankSourceRepository;
    private final BankQuestionRepository bankQuestionRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassContentItemRepository classContentItemRepository;
    private final ClassSectionRepository classSectionRepository;
    private final ProgressRepository progressRepository;
    private final EnrollmentService enrollmentService;
    private final ClassMemberAuthorizationService classMemberAuthorizationService;
    private final ClassContentAccessService classContentAccessService;
    private final RedisCacheInvalidationService cacheInvalidationService;
    private final NotificationService notificationService;


    @Override
    @Transactional
    public QuizAttemptDetailResponse startQuizAttempt(Integer quizId, Integer chapterItemId) {
        throw new UnsupportedOperationException("Legacy chapter-item quiz flow has been removed");
    }

    @Override
    @Transactional
    public QuizAttemptDetailResponse startQuizAttemptForClassContentItem(Integer quizId, Integer classContentItemId) {
        User currentUser = userService.getCurrentUser();
        if (!userService.isCurrentUser(currentUser.getId())) {
            throw new UnauthorizedException("Chi hoc vien duoc dang ky moi duoc truy cap vao noi dung nay");
        }

        ClassContentItem classContentItem = classContentItemRepository.findById(classContentItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Class content item not found"));
        Quiz chosenQuiz = resolveQuizFromClassContentItem(classContentItem);
        if (!chosenQuiz.getId().equals(quizId)) {
            throw new ResourceNotFoundException("Quiz không tồn tại");
        }

        ensureClassSectionInteractive(classContentItem.getClassChapter().getClassSection());
        Integer classSectionId = classContentItem.getClassChapter().getClassSection().getId();
        boolean isEnrolled = enrollmentRepository.existsByStudent_IdAndClassSection_IdAndApprovalStatus(
                currentUser.getId(), classSectionId, EnrollmentStatus.APPROVED);
        if (!isEnrolled) {
            throw new UnauthorizedException("Bạn không có quyền truy cập vào tài nguyên này!");
        }

        ClassContentAccessResult accessResult = classContentAccessService.evaluateForUser(classContentItem, currentUser);
        if (!accessResult.accessible()) {
            throw new BusinessException(accessResult.message() != null
                    ? accessResult.message()
                    : "Bạn không có quyền truy cập vào tài nguyên này!");
        }

        checkQuizAvailability(chosenQuiz);

        Optional<QuizAttempt> inProgressAttempt = quizAttemptRepository
                .findTopByClassContentItem_IdAndStudent_IdAndStatusOrderByIdDesc(
                        classContentItemId,
                        currentUser.getId(),
                        AttemptStatus.IN_PROGRESS
                );
        if (inProgressAttempt.isPresent()) {
            return convertToDetailResponse(inProgressAttempt.get());
        }

        int currentAttempt = quizAttemptRepository.countByClassContentItem_IdAndStudent_Id(classContentItemId, currentUser.getId());
        if (chosenQuiz.getMaxAttempts() != null && currentAttempt >= chosenQuiz.getMaxAttempts()) {
            throw new BusinessException("Da vuot qua so lan lam bai!");
        }

        QuizAttempt attempt = QuizAttempt.builder()
                .quiz(chosenQuiz)
                .student(currentUser)
                .classContentItem(classContentItem)
                .attemptNumber(currentAttempt + 1)
                .grade(0)
                .isPassed(false)
                .totalQuestions(0)
                .unansweredQuestions(0)
                .incorrectAnswers(0)
                .correctAnswers(0)
                .earnedPoints(BigDecimal.ZERO)
                .totalPoints(BigDecimal.ZERO)
                .gradingStatus(GradingStatus.AUTO_GRADED)
                .startTime(LocalDateTime.now())
                .status(AttemptStatus.IN_PROGRESS)
                .build();
        quizAttemptRepository.save(attempt);

        initializeAttemptContent(attempt, chosenQuiz);

        return convertToDetailResponse(attempt);
    }

    private void initializeAttemptContent(QuizAttempt attempt, Quiz quiz) {
        boolean useAttemptSnapshot = quiz.isGenerateQuestionsPerAttempt()
                || quiz.isShuffleQuestions()
                || quiz.isShuffleAnswers()
                || hasInteractiveQuestions(quiz);

        if (!useAttemptSnapshot) {
            List<QuizQuestion> questions = quizQuestionRepository.findByQuiz_IdOrderByIdAsc(quiz.getId());
            attempt.setTotalQuestions(questions.size());
            attempt.setUnansweredQuestions(questions.size());
            quizAttemptRepository.save(attempt);

            for (QuizQuestion question : questions) {
                QuizAttemptAnswer attemptAnswer = new QuizAttemptAnswer();
                attemptAnswer.setAttempt(attempt);
                attemptAnswer.setQuestion(question);
                quizAttemptAnswerRepository.save(attemptAnswer);
            }
            return;
        }

        Random random = new Random(computeAttemptSeed(attempt));
        List<QuizAttemptQuestion> attemptQuestions = quiz.isGenerateQuestionsPerAttempt()
                ? buildAttemptQuestionsFromBankSources(attempt, quiz, random)
                : buildAttemptQuestionsFromQuizQuestions(attempt, quiz, random);

        if (quiz.isShuffleQuestions()) {
            Collections.shuffle(attemptQuestions, random);
        }
        applyQuestionOrdering(attemptQuestions);
        quizAttemptQuestionRepository.saveAll(attemptQuestions);

        attempt.setTotalQuestions(attemptQuestions.size());
        attempt.setUnansweredQuestions(attemptQuestions.size());
        quizAttemptRepository.save(attempt);

        for (QuizAttemptQuestion attemptQuestion : attemptQuestions) {
            QuizAttemptAnswer attemptAnswer = new QuizAttemptAnswer();
            attemptAnswer.setAttempt(attempt);
            attemptAnswer.setAttemptQuestion(attemptQuestion);
            quizAttemptAnswerRepository.save(attemptAnswer);
        }
    }

    private long computeAttemptSeed(QuizAttempt attempt) {
        Integer attemptId = attempt.getId();
        Integer quizId = attempt.getQuiz() != null ? attempt.getQuiz().getId() : null;
        Integer studentId = attempt.getStudent() != null ? attempt.getStudent().getId() : null;
        return Objects.hash(attemptId, quizId, studentId);
    }

    private void applyQuestionOrdering(List<QuizAttemptQuestion> questions) {
        int order = 1;
        for (QuizAttemptQuestion question : questions) {
            question.setOrderIndex(order++);
            if (question.getOptions() != null) {
                int optionOrder = 1;
                for (QuizAttemptQuestionOption option : question.getOptions()) {
                    option.setOrderIndex(optionOrder++);
                }
            }
            if (question.getItems() != null) {
                int itemOrder = 1;
                for (QuizAttemptQuestionItem item : question.getItems()) {
                    if (item.getOrderIndex() == null) {
                        item.setOrderIndex(itemOrder);
                    }
                    itemOrder++;
                }
            }
        }
    }

    private boolean hasInteractiveQuestions(Quiz quiz) {
        List<QuizQuestion> questions = quizQuestionRepository.findByQuiz_IdOrderByIdAsc(quiz.getId());
        return questions.stream().anyMatch(question -> isInteractionQuestionType(question.getType()));
    }

    private List<QuizAttemptQuestion> buildAttemptQuestionsFromQuizQuestions(QuizAttempt attempt, Quiz quiz, Random random) {
        List<QuizQuestion> quizQuestions = quizQuestionRepository.findByQuiz_IdOrderByIdAsc(quiz.getId());
        List<QuizAttemptQuestion> attemptQuestions = new ArrayList<>();

        for (QuizQuestion sourceQuestion : quizQuestions) {
            QuizAttemptQuestion attemptQuestion = new QuizAttemptQuestion();
            attemptQuestion.setAttempt(attempt);
            attemptQuestion.setSourceQuizQuestion(sourceQuestion);
            attemptQuestion.setSourceBankQuestion(sourceQuestion.getSourceBankQuestion());
            attemptQuestion.setContent(sourceQuestion.getContent());
            attemptQuestion.setType(sourceQuestion.getType());
            attemptQuestion.setResource(sourceQuestion.getResource());
            attemptQuestion.setPoints(sourceQuestion.getPoints() != null ? sourceQuestion.getPoints() : BigDecimal.ONE);

            List<QuizAttemptQuestionOption> options = new ArrayList<>();
            if (sourceQuestion.getAnswers() != null) {
                for (QuizAnswer sourceAnswer : sourceQuestion.getAnswers()) {
                    QuizAttemptQuestionOption option = new QuizAttemptQuestionOption();
                    option.setAttemptQuestion(attemptQuestion);
                    option.setSourceQuizAnswer(sourceAnswer);
                    option.setContent(sourceAnswer.getContent());
                    option.setIsCorrect(sourceAnswer.getIsCorrect());
                    option.setExplanation(sourceAnswer.getExplanation());
                    option.setResource(sourceAnswer.getResource());
                    options.add(option);
                }
            }
            if (quiz.isShuffleAnswers() && options.size() > 1) {
                Collections.shuffle(options, random);
            }
            attemptQuestion.setOptions(options);
            attemptQuestion.setItems(copyQuizQuestionItems(sourceQuestion, attemptQuestion));
            randomizeInteractionDisplayOrder(attemptQuestion, random);
            attemptQuestions.add(attemptQuestion);
        }

        return attemptQuestions;
    }

    private List<QuizAttemptQuestion> buildAttemptQuestionsFromBankSources(QuizAttempt attempt, Quiz quiz, Random random) {
        List<QuizBankSource> sources = quizBankSourceRepository.findByQuiz_IdOrderByOrderIndexAsc(quiz.getId());
        if (sources.isEmpty()) {
            throw new BusinessException("Quiz has no question bank sources configured");
        }

        LinkedHashMap<Integer, BankQuestion> selectedQuestions = new LinkedHashMap<>();
        HashSet<Integer> excludedIds = new HashSet<>();

        for (QuizBankSource source : sources) {
            List<BankQuestion> selectedForSource = selectBankQuestionsForSource(source, random, excludedIds);
            for (BankQuestion question : selectedForSource) {
                if (selectedQuestions.putIfAbsent(question.getId(), question) == null) {
                    excludedIds.add(question.getId());
                }
            }
        }

        if (selectedQuestions.isEmpty()) {
            throw new BusinessException("No questions matched the configured question bank sources");
        }

        List<QuizAttemptQuestion> attemptQuestions = new ArrayList<>();
        for (BankQuestion sourceQuestion : selectedQuestions.values()) {
            QuizAttemptQuestion attemptQuestion = new QuizAttemptQuestion();
            attemptQuestion.setAttempt(attempt);
            attemptQuestion.setSourceBankQuestion(sourceQuestion);
            attemptQuestion.setContent(sourceQuestion.getContent());
            attemptQuestion.setType(sourceQuestion.getType());
            attemptQuestion.setResource(sourceQuestion.getResource());
            attemptQuestion.setPoints(sourceQuestion.getDefaultPoints() != null ? sourceQuestion.getDefaultPoints() : BigDecimal.ONE);

            List<QuizAttemptQuestionOption> options = new ArrayList<>();
            if (sourceQuestion.getOptions() != null) {
                for (BankQuestionOption sourceOption : sourceQuestion.getOptions()) {
                    QuizAttemptQuestionOption option = new QuizAttemptQuestionOption();
                    option.setAttemptQuestion(attemptQuestion);
                    option.setSourceBankQuestionOption(sourceOption);
                    option.setContent(sourceOption.getContent());
                    option.setIsCorrect(sourceOption.getIsCorrect());
                    option.setExplanation(sourceOption.getExplanation());
                    option.setResource(sourceOption.getResource());
                    options.add(option);
                }
            }
            if (quiz.isShuffleAnswers() && options.size() > 1) {
                Collections.shuffle(options, random);
            }
            attemptQuestion.setOptions(options);
            attemptQuestion.setItems(copyBankQuestionItems(sourceQuestion, attemptQuestion));
            randomizeInteractionDisplayOrder(attemptQuestion, random);
            attemptQuestions.add(attemptQuestion);
        }

        return attemptQuestions;
    }

    private List<QuizAttemptQuestionItem> copyQuizQuestionItems(
            QuizQuestion sourceQuestion,
            QuizAttemptQuestion attemptQuestion
    ) {
        if (sourceQuestion.getInteractionItems() == null) {
            return List.of();
        }
        List<QuizAttemptQuestionItem> items = new ArrayList<>();
        for (QuestionInteractionItem sourceItem : sourceQuestion.getInteractionItems()) {
            QuizAttemptQuestionItem item = new QuizAttemptQuestionItem();
            item.setAttemptQuestion(attemptQuestion);
            item.setSourceItem(sourceItem);
            copyInteractionItemFields(sourceItem, item);
            items.add(item);
        }
        return items;
    }

    private List<QuizAttemptQuestionItem> copyBankQuestionItems(
            BankQuestion sourceQuestion,
            QuizAttemptQuestion attemptQuestion
    ) {
        if (sourceQuestion.getInteractionItems() == null) {
            return List.of();
        }
        List<QuizAttemptQuestionItem> items = new ArrayList<>();
        for (QuestionInteractionItem sourceItem : sourceQuestion.getInteractionItems()) {
            QuizAttemptQuestionItem item = new QuizAttemptQuestionItem();
            item.setAttemptQuestion(attemptQuestion);
            item.setSourceItem(sourceItem);
            copyInteractionItemFields(sourceItem, item);
            items.add(item);
        }
        return items;
    }

    private void copyInteractionItemFields(QuestionInteractionItem sourceItem, QuizAttemptQuestionItem targetItem) {
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

    private void randomizeInteractionDisplayOrder(QuizAttemptQuestion question, Random random) {
        if (question.getItems() == null || question.getItems().size() < 2) {
            return;
        }

        if (question.getType() == QuestionType.DRAG_ORDER) {
            List<QuizAttemptQuestionItem> orderItems = question.getItems().stream()
                    .filter(item -> item.getRole() == QuestionInteractionItemRole.ORDER_ITEM)
                    .toList();
            if (orderItems.size() > 1) {
                List<QuizAttemptQuestionItem> shuffled = new ArrayList<>(orderItems);
                Collections.shuffle(shuffled, random);
                for (int i = 0; i < shuffled.size(); i++) {
                    shuffled.get(i).setOrderIndex(i + 1);
                }
            }
            return;
        }

        if (isMatchingQuestionType(question.getType())) {
            List<QuizAttemptQuestionItem> matches = question.getItems().stream()
                    .filter(item -> item.getRole() == QuestionInteractionItemRole.MATCH)
                    .toList();
            if (matches.size() > 1) {
                List<QuizAttemptQuestionItem> shuffled = new ArrayList<>(matches);
                Collections.shuffle(shuffled, random);
                for (int i = 0; i < shuffled.size(); i++) {
                    shuffled.get(i).setOrderIndex(i + 1);
                }
            }
        }
    }

    private List<BankQuestion> selectBankQuestionsForSource(QuizBankSource source, Random random, Set<Integer> excludedQuestionIds) {
        List<BankQuestion> matchedQuestions = resolveMatchedBankQuestions(source);

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
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            if (candidates.size() < source.getQuestionCount()) {
                throw new BusinessException("Not enough questions in question bank source to satisfy questionCount");
            }
            Collections.shuffle(candidates, random);
            return candidates.subList(0, source.getQuestionCount());
        }
        throw new BusinessException("Unsupported question bank selection mode");
    }

    private List<BankQuestion> resolveMatchedBankQuestions(QuizBankSource source) {
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
                            .collect(java.util.stream.Collectors.toSet());
                    return matcher.test(questionTagIds);
                })
                .toList();
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

    @Override
    @Transactional
    public void answerQuestion(Integer attemptId, Integer questionId, QuizAttemptAnswerRequest request) {
        log.info("Answering question {} in attempt {}", questionId, attemptId);

        QuizAttempt attempt = quizAttemptRepository.findById((attemptId))
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found"));
        ensureClassSectionInteractive(resolveClassSection(attempt));

        User currentUser = userService.getCurrentUser();
        if (!attempt.getStudent().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("KhÃ´ng cÃ³ quyá»n lÃ m bÃ i nÃ y");
        }

        if (isAttemptExpired(attempt)) {
            expireAttempt(attempt);
            throw new BusinessException("ÄÃ£ háº¿t thá»i gian lÃ m bÃ i!");
        }

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new BusinessException("BÃ i lÃ m Ä‘Ã£ káº¿t thÃºc");
        }

        QuizAttemptAnswer attemptAnswer = quizAttemptAnswerRepository
                .findByAttempt_IdAndAttemptQuestion_Id(attemptId, questionId)
                .or(() -> quizAttemptAnswerRepository.findByAttempt_IdAndQuestion_Id(attemptId, questionId))
                .orElseThrow(() -> new ResourceNotFoundException("Answer attempt not found"));

        QuizAttemptQuestion attemptQuestion = attemptAnswer.getAttemptQuestion();
        QuizQuestion question = attemptAnswer.getQuestion();
        QuestionType questionType = attemptQuestion != null ? attemptQuestion.getType() : question.getType();

        boolean hasPrimaryAnswerPayload = hasPrimaryAnswerPayload(request);

        // Xá»­ lÃ½ Single Choice
        if (hasPrimaryAnswerPayload && isSingleSelectQuestionType(questionType)) {
            List<Integer> selectedIds = request.getSelectedAnswerIds() != null ? request.getSelectedAnswerIds() : List.of();
            if (attemptQuestion != null) {
                List<QuizAttemptQuestionOption> selectedOptions = quizAttemptQuestionOptionRepository.findAllById(selectedIds);
                if (selectedOptions.size() != selectedIds.size()) {
                    throw new BusinessException("Má»™t sá»‘ Ä‘Ã¡p Ã¡n khÃ´ng tá»“n táº¡i");
                }
                for (QuizAttemptQuestionOption opt : selectedOptions) {
                    if (opt.getAttemptQuestion() == null || !opt.getAttemptQuestion().getId().equals(attemptQuestion.getId())) {
                        throw new BusinessException("ÄÃ¡p Ã¡n khÃ´ng thuá»™c vá» cÃ¢u há»i nÃ y");
                    }
                }
                attemptAnswer.setSelectedOptions(selectedOptions);
                attemptAnswer.setSelectedAnswers(null);
                attemptAnswer.setTextAnswer(null);
            } else {
                List<QuizAnswer> selectedAnswers = quizAnswerRepository.findAllById(selectedIds);

                if (selectedAnswers.size() != selectedIds.size()) {
                    throw new BusinessException("Má»™t sá»‘ Ä‘Ã¡p Ã¡n khÃ´ng tá»“n táº¡i");
                }
                for (QuizAnswer ans : selectedAnswers) {
                    if (!ans.getQuizQuestion().getId().equals(questionId)) {
                        throw new BusinessException("ÄÃ¡p Ã¡n khÃ´ng thuá»™c vá» cÃ¢u há»i nÃ y");
                    }
                }
                attemptAnswer.setSelectedAnswers(selectedAnswers);
                attemptAnswer.setSelectedOptions(null);
                attemptAnswer.setTextAnswer(null);
            }
        }
        // Xá»­ lÃ½ Multiple Choice
        else if (hasPrimaryAnswerPayload && questionType == QuestionType.MULTIPLE_CHOICE) {
            List<Integer> selectedIds = request.getSelectedAnswerIds() != null ? request.getSelectedAnswerIds() : List.of();
            if (attemptQuestion != null) {
                List<QuizAttemptQuestionOption> selectedOptions = quizAttemptQuestionOptionRepository.findAllById(selectedIds);
                if (selectedOptions.size() != selectedIds.size()) {
                    throw new BusinessException("Má»™t sá»‘ Ä‘Ã¡p Ã¡n khÃ´ng tá»“n táº¡i");
                }
                for (QuizAttemptQuestionOption opt : selectedOptions) {
                    if (opt.getAttemptQuestion() == null || !opt.getAttemptQuestion().getId().equals(attemptQuestion.getId())) {
                        throw new BusinessException("ÄÃ¡p Ã¡n khÃ´ng thuá»™c vá» cÃ¢u há»i nÃ y");
                    }
                }
                attemptAnswer.setSelectedOptions(selectedOptions);
                attemptAnswer.setSelectedAnswers(null);
                attemptAnswer.setTextAnswer(null);
            } else {
                List<QuizAnswer> selectedAnswers = quizAnswerRepository.findAllById(selectedIds);

                if (selectedAnswers.size() != selectedIds.size()) {
                    throw new BusinessException("Má»™t sá»‘ Ä‘Ã¡p Ã¡n khÃ´ng tá»“n táº¡i");
                }
                for (QuizAnswer ans : selectedAnswers) {
                    if (!ans.getQuizQuestion().getId().equals(questionId)) {
                        throw new BusinessException("ÄÃ¡p Ã¡n khÃ´ng thuá»™c vá» cÃ¢u há»i nÃ y");
                    }
                }
                attemptAnswer.setSelectedAnswers(selectedAnswers);
                attemptAnswer.setSelectedOptions(null);
                attemptAnswer.setTextAnswer(null);
            }
        }
        // Xá»­ lÃ½ Short answer
        else if (hasPrimaryAnswerPayload && isTextAnswerQuestionType(questionType)) {
            String userAnswer = request.getTextAnswer() != null ? request.getTextAnswer().trim() : "";
            attemptAnswer.setTextAnswer(userAnswer);
            attemptAnswer.setSelectedAnswers(null);
            attemptAnswer.setSelectedOptions(null);
        }
        // Xá»­ lÃ½ cÃ¢u há»i tÆ°Æ¡ng tÃ¡c
        else if (hasPrimaryAnswerPayload && isInteractionQuestionType(questionType)) {
            saveInteractionAnswerItems(attemptAnswer, attemptQuestion, questionType, request);
        }

        attemptAnswer.setCompletedAt(LocalDateTime.now());
        quizAttemptAnswerRepository.save(attemptAnswer);
    }

    private boolean hasPrimaryAnswerPayload(QuizAttemptAnswerRequest request) {
        return request.getSelectedAnswerIds() != null
                || request.getTextAnswer() != null
                || request.getAnswerItems() != null;
    }

    private void saveInteractionAnswerItems(
            QuizAttemptAnswer attemptAnswer,
            QuizAttemptQuestion attemptQuestion,
            QuestionType questionType,
            QuizAttemptAnswerRequest request
    ) {
        if (attemptQuestion == null) {
            throw new BusinessException("Interactive questions require attempt snapshot data");
        }

        List<QuizAttemptQuestionItem> questionItems = quizAttemptQuestionItemRepository
                .findByAttemptQuestion_IdOrderByOrderIndexAsc(attemptQuestion.getId());
        Map<Integer, QuizAttemptQuestionItem> itemsById = new HashMap<>();
        Map<Integer, QuizAttemptQuestionItem> blanksByIndex = new HashMap<>();
        for (QuizAttemptQuestionItem item : questionItems) {
            itemsById.put(item.getId(), item);
            if (item.getBlankIndex() != null) {
                blanksByIndex.put(item.getBlankIndex(), item);
            }
        }

        List<QuizAttemptAnswerItem> incomingAnswerItems = new ArrayList<>();
        List<QuizAttemptAnswerItemRequest> requestItems = request.getAnswerItems() != null
                ? request.getAnswerItems()
                : List.of();

        for (QuizAttemptAnswerItemRequest itemRequest : requestItems) {
            QuizAttemptQuestionItem questionItem = itemRequest.getItemId() != null
                    ? itemsById.get(itemRequest.getItemId())
                    : blanksByIndex.get(itemRequest.getBlankIndex());
            if (questionItem == null) {
                throw new BusinessException("Answer item does not belong to this question");
            }

            QuizAttemptQuestionItem selectedItem = null;
            if (itemRequest.getSelectedItemId() != null) {
                selectedItem = itemsById.get(itemRequest.getSelectedItemId());
                if (selectedItem == null) {
                    throw new BusinessException("Selected item does not belong to this question");
                }
            }

            validateInteractionAnswerItem(questionType, questionItem, selectedItem, itemRequest);

            QuizAttemptAnswerItem answerItem = new QuizAttemptAnswerItem();
            answerItem.setAttemptAnswer(attemptAnswer);
            answerItem.setAttemptQuestionItem(questionItem);
            answerItem.setSelectedItem(selectedItem);
            answerItem.setAnswerText(itemRequest.getAnswerText() != null ? itemRequest.getAnswerText().trim() : null);
            answerItem.setSubmittedOrderIndex(itemRequest.getSubmittedOrderIndex());
            answerItem.setBlankIndex(itemRequest.getBlankIndex() != null
                    ? itemRequest.getBlankIndex()
                    : questionItem.getBlankIndex());
            answerItem.setIsCorrect(null);
            incomingAnswerItems.add(answerItem);
        }

        List<QuizAttemptAnswerItem> preservedContentBlockAnswers = getContentBlockAnswerItems(attemptAnswer);
        if (attemptAnswer.getAnswerItems() == null) {
            attemptAnswer.setAnswerItems(new ArrayList<>(preservedContentBlockAnswers));
        } else {
            attemptAnswer.getAnswerItems().removeIf(item -> !isContentBlockAnswerItem(item));
        }
        attemptAnswer.getAnswerItems().addAll(incomingAnswerItems);
        attemptAnswer.setSelectedAnswers(null);
        attemptAnswer.setSelectedOptions(null);
        attemptAnswer.setTextAnswer(null);
    }

    private void saveContentBlockAnswers(
            QuizAttemptAnswer attemptAnswer,
            List<QuizAttemptAnswerItemRequest> requestItems
    ) {
        if (requestItems == null) {
            return;
        }

        List<QuizAttemptAnswerItem> incomingContentBlockAnswers = new ArrayList<>();
        Set<String> validBlankKeys = resolveContentBlockBlankKeys(attemptAnswer);
        for (QuizAttemptAnswerItemRequest itemRequest : requestItems) {
            if (!StringUtils.hasText(itemRequest.getBlankKey())) {
                throw new BusinessException("Content block answer requires blankKey");
            }
            String blankKey = itemRequest.getBlankKey().trim();
            if (!validBlankKeys.contains(blankKey)) {
                throw new BusinessException("Content block answer blankKey does not belong to this question");
            }

            QuizAttemptAnswerItem answerItem = new QuizAttemptAnswerItem();
            answerItem.setAttemptAnswer(attemptAnswer);
            answerItem.setBlankKey(blankKey);
            answerItem.setAnswerText(itemRequest.getAnswerText() != null ? itemRequest.getAnswerText().trim() : "");
            answerItem.setAttemptQuestionItem(null);
            answerItem.setSelectedItem(null);
            answerItem.setSubmittedOrderIndex(null);
            answerItem.setBlankIndex(null);
            answerItem.setIsCorrect(null);
            incomingContentBlockAnswers.add(answerItem);
        }

        List<QuizAttemptAnswerItem> preservedInteractionAnswers = getInteractionAnswerItems(attemptAnswer);
        if (attemptAnswer.getAnswerItems() == null) {
            attemptAnswer.setAnswerItems(new ArrayList<>(preservedInteractionAnswers));
        } else {
            attemptAnswer.getAnswerItems().removeIf(this::isContentBlockAnswerItem);
        }
        attemptAnswer.getAnswerItems().addAll(incomingContentBlockAnswers);
    }

    private List<QuizAttemptAnswerItem> getContentBlockAnswerItems(QuizAttemptAnswer attemptAnswer) {
        List<QuizAttemptAnswerItem> answerItems = attemptAnswer.getAnswerItems() != null
                ? attemptAnswer.getAnswerItems()
                : quizAttemptAnswerItemRepository.findByAttemptAnswer_IdOrderByIdAsc(attemptAnswer.getId());
        return answerItems.stream()
                .filter(this::isContentBlockAnswerItem)
                .toList();
    }

    private List<QuizAttemptAnswerItem> getInteractionAnswerItems(QuizAttemptAnswer attemptAnswer) {
        List<QuizAttemptAnswerItem> answerItems = attemptAnswer.getAnswerItems() != null
                ? attemptAnswer.getAnswerItems()
                : quizAttemptAnswerItemRepository.findByAttemptAnswer_IdOrderByIdAsc(attemptAnswer.getId());
        return answerItems.stream()
                .filter(item -> !isContentBlockAnswerItem(item))
                .toList();
    }

    private boolean isContentBlockAnswerItem(QuizAttemptAnswerItem item) {
        return item != null
                && StringUtils.hasText(item.getBlankKey())
                && item.getAttemptQuestionItem() == null
                && item.getSelectedItem() == null
                && item.getSubmittedOrderIndex() == null
                && item.getBlankIndex() == null;
    }

    private Set<String> resolveContentBlockBlankKeys(QuizAttemptAnswer attemptAnswer) {
        return Set.of();
    }

    private void validateInteractionAnswerItem(
            QuestionType questionType,
            QuizAttemptQuestionItem questionItem,
            QuizAttemptQuestionItem selectedItem,
            QuizAttemptAnswerItemRequest itemRequest
    ) {
        if (isMatchingQuestionType(questionType)) {
            if (questionItem.getRole() != QuestionInteractionItemRole.PROMPT
                    || selectedItem == null
                    || selectedItem.getRole() != QuestionInteractionItemRole.MATCH) {
                throw new BusinessException("MATCHING answers must map a prompt item to a match item");
            }
            return;
        }

        if (questionType == QuestionType.DRAG_ORDER) {
            if (questionItem.getRole() != QuestionInteractionItemRole.ORDER_ITEM
                    || itemRequest.getSubmittedOrderIndex() == null) {
                throw new BusinessException("DRAG_ORDER answers require order item and submittedOrderIndex");
            }
            return;
        }

        if (questionType == QuestionType.CLOZE && questionItem.getRole() != QuestionInteractionItemRole.BLANK) {
            throw new BusinessException("CLOZE answers must reference blank items");
        }
    }

    @Override
    @Transactional
    public QuizAttemptResponse submitQuiz(Integer attemptId) {
        log.info("Submitting quiz attempt {}", attemptId);
        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found"));
        ensureClassSectionInteractive(resolveClassSection(attempt));

        User currentUser = userService.getCurrentUser();
        if (!attempt.getStudent().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("KhÃ´ng cÃ³ quyá»n ná»™p bÃ i nÃ y");
        }
        if (attempt.getCompletedTime() != null) {
            throw new BusinessException("Báº¡n Ä‘Ã£ ná»™p bÃ i rá»“i!");
        }
        if (isAttemptExpired(attempt)) {
            expireAttempt(attempt);
            return convertQuizAttemptToDTO(attempt);
        }

        updateAttemptStatistics(attempt);
        attempt.setCompletedTime(LocalDateTime.now());
        Integer passingScore = attempt.getQuiz().getMinPassScore() != null ? attempt.getQuiz().getMinPassScore() : 0;
        boolean isPassed = attempt.getGrade() >= passingScore; // Check pass
        attempt.setIsPassed(isPassed);
        attempt.setStatus(AttemptStatus.COMPLETED);
        quizAttemptRepository.save(attempt);

        if (isPassed) {
            markProgressIfPassed(attempt);
        }

        cacheInvalidationService.evictTeachingAndReportCaches();
        return convertQuizAttemptToDTO(attempt);
    }

    @Override
    public QuizAttemptDetailResponse getCurrentAttempt(Integer chapterItemId) {
        throw new UnsupportedOperationException("Legacy chapter-item quiz flow has been removed");
    }

    @Override
    public QuizAttemptDetailResponse getCurrentAttemptForClassContentItem(Integer classContentItemId) {
        User currentUser = userService.getCurrentUser();
        ClassContentItem classContentItem = classContentItemRepository.findById(classContentItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Class content item not found"));
        // Lớp lưu trữ vẫn cho người học XEM lại bài đang/đã làm; thao tác ghi
        // (answer/submit) đã bị chặn riêng ở từng API mutation.
        QuizAttempt attempt = quizAttemptRepository
                .findTopByClassContentItem_IdAndStudent_IdAndStatusOrderByIdDesc(
                        classContentItemId,
                        currentUser.getId(),
                        AttemptStatus.IN_PROGRESS
                )
                .orElseThrow(() -> new ResourceNotFoundException("Không có bài làm nào đang diễn ra"));

        attempt = expireAttemptIfTimedOut(attempt);
        cacheInvalidationService.evictTeachingAndReportCaches();
        return convertToDetailResponse(attempt);
    }

    // =========================================================================
    // THá»NG KÃŠ BÃ€I LÃ€M Cá»¦A CÃ NHÃ‚N SINH VIÃŠN VÃ€ SINH VIÃŠN NÃ“I CHUNG TRONG KHÃ“A Há»ŒC
    // =========================================================================

    @Override
    public QuizAttemptDetailResponse getAttemptDetail(Integer attemptId) {
        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found"));

        User currentUser = userService.getCurrentUser();
        boolean isStudent = attempt.getStudent().getId().equals(currentUser.getId());
        boolean isTeacher = canReviewAttempt(attempt, currentUser);
        boolean isAdmin = currentUser.getRole().getRoleName() == RoleType.ADMIN;

        // Check Access Control (Quyá»n truy cáº­p)
        if (!isStudent && !isTeacher && !isAdmin) {
            throw new UnauthorizedException("Báº¡n khÃ´ng cÃ³ quyá»n xem bÃ i lÃ m nÃ y");
        }

        attempt = expireAttemptIfTimedOut(attempt);
        // Gá»i hÃ m convert chung (nÃ³ sáº½ tá»± check quyá»n xem Ä‘Ã¡p Ã¡n bÃªn trong)
        return convertToDetailResponse(attempt);
    }

    @Override
    public PageResponse<QuizAttemptResponse> getManagedQuizAttempts(
            Integer classSectionId,
            Integer quizId,
            String result,
            String studentKeyword,
            String quizKeyword,
            String classKeyword,
            Pageable pageable
    ) {
        User currentUser = userService.getCurrentUser();
        if (classSectionId != null && currentUser.getRole().getRoleName() != RoleType.ADMIN) {
            ClassSection classSection = classSectionRepository.findById(classSectionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
            if (!canReviewClassSection(classSection, currentUser)) {
                throw new UnauthorizedException("Bạn không có quyền xem lịch sử bài làm");
            }
        }
        String resultFilter = StringUtils.hasText(result) ? result.trim().toUpperCase(Locale.ROOT) : null;
        if (resultFilter != null && !List.of("PASS", "FAIL", "PENDING").contains(resultFilter)) {
            resultFilter = null;
        }
        Specification<QuizAttempt> specification = buildManagedQuizAttemptSpecification(
                currentUser,
                classSectionId,
                quizId,
                resultFilter,
                StringUtils.hasText(studentKeyword) ? studentKeyword.trim() : null,
                StringUtils.hasText(quizKeyword) ? quizKeyword.trim() : null,
                StringUtils.hasText(classKeyword) ? classKeyword.trim() : null
        );

        Page<QuizAttemptResponse> dtoPage = quizAttemptRepository.findAll(specification, pageable)
                .map(this::convertQuizAttemptToDTO);

        return new PageResponse<>(
                dtoPage.getNumber() + 1,
                dtoPage.getTotalPages(),
                dtoPage.getTotalElements(),
                dtoPage.getContent()
        );
    }

    private Specification<QuizAttempt> buildManagedQuizAttemptSpecification(
            User currentUser,
            Integer classSectionId,
            Integer quizId,
            String resultFilter,
            String studentKeyword,
            String quizKeyword,
            String classKeyword
    ) {
        return Specification.allOf(QuizAttemptSpecification.hasStatuses(List.of(AttemptStatus.COMPLETED, AttemptStatus.EXPIRED)))
                .and(QuizAttemptSpecification.hasClassSectionId(classSectionId))
                .and(QuizAttemptSpecification.hasQuizId(quizId))
                .and(QuizAttemptSpecification.accessibleFor(
                        currentUser,
                        List.of(ClassMemberRole.TEACHER, ClassMemberRole.TA)
                ))
                .and(QuizAttemptSpecification.studentMatches(studentKeyword))
                .and(QuizAttemptSpecification.quizMatches(quizKeyword))
                .and(QuizAttemptSpecification.classMatches(classKeyword))
                .and(QuizAttemptSpecification.hasManagedResult(resultFilter, GradingStatus.NEEDS_REVIEW));
    }

    @Override
    @Transactional
    public QuizAttemptDetailResponse reviewAttempt(Integer attemptId, QuizAttemptReviewRequest request) {
        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found"));
        ensureClassSectionInteractive(resolveClassSection(attempt));
        User currentUser = userService.getCurrentUser();
        if (!canReviewAttempt(attempt, currentUser)) {
            throw new UnauthorizedException("Bạn không có quyền chấm bài này");
        }
        if (attempt.getStudent() != null && attempt.getStudent().getId().equals(currentUser.getId())) {
            throw new BusinessException("Bạn không thể tự chấm bài của chính mình");
        }
        if (attempt.getStatus() != AttemptStatus.COMPLETED && attempt.getStatus() != AttemptStatus.EXPIRED) {
            throw new BusinessException("Chi co the cham bai da nop hoac da het gio");
        }

        Map<Integer, QuizAttemptAnswer> answersById = quizAttemptAnswerRepository.findByAttempt_Id(attemptId)
                .stream()
                .filter(answer -> answer.getId() != null)
                .collect(java.util.stream.Collectors.toMap(QuizAttemptAnswer::getId, answer -> answer));

        if (request != null && request.getAnswers() != null) {
            for (QuizAttemptReviewRequest.AnswerReview answerReview : request.getAnswers()) {
                if (answerReview == null || answerReview.getAnswerId() == null) {
                    continue;
                }
                QuizAttemptAnswer answer = answersById.get(answerReview.getAnswerId());
                if (answer == null) {
                    throw new BusinessException("Reviewed answer does not belong to this attempt");
                }
                QuestionType questionType = resolveAnswerQuestionType(answer);
                if (questionType != QuestionType.ESSAY) {
                    continue;
                }
                BigDecimal maxPoints = answer.getMaxPoints() != null
                        ? answer.getMaxPoints()
                        : resolveAnswerPointValue(answer);
                BigDecimal score = clampScore(answerReview.getScore(), maxPoints);
                answer.setManualScore(score);
                answer.setEarnedPoints(score);
                answer.setIsCorrect(score.compareTo(maxPoints) >= 0);
                answer.setTeacherFeedback(StringUtils.hasText(answerReview.getFeedback())
                        ? answerReview.getFeedback().trim()
                        : null);
                answer.setReviewedBy(currentUser);
                answer.setReviewedAt(LocalDateTime.now());
                answer.setGradingStatus(GradingStatus.REVIEWED);
            }
        }

        if (request != null && request.getInstructorFeedback() != null) {
            attempt.setInstructorFeedback(StringUtils.hasText(request.getInstructorFeedback())
                    ? request.getInstructorFeedback().trim()
                    : null);
        }

        updateAttemptStatistics(attempt);
        Integer passingScore = attempt.getQuiz().getMinPassScore() != null ? attempt.getQuiz().getMinPassScore() : 0;
        attempt.setIsPassed(attempt.getGrade() >= passingScore);
        quizAttemptRepository.save(attempt);
        // Báo cho học sinh khi bài đã được chấm/đánh giá, kèm ref-link tới trang kết quả.
        if (attempt.getStudent() != null) {
            ClassSection reviewedClassSection = resolveClassSection(attempt);
            Integer reviewedCsId = reviewedClassSection != null ? reviewedClassSection.getId() : null;
            Integer reviewedQuizId = attempt.getQuiz() != null ? attempt.getQuiz().getId() : null;
            notificationService.createNotification(
                    attempt.getStudent(),
                    "Bài quiz đã được chấm",
                    "Giảng viên đã chấm/đánh giá bài làm quiz của bạn.",
                    "QUIZ_REVIEWED",
                    null,
                    (reviewedCsId != null && reviewedQuizId != null)
                            ? "/class-sections/" + reviewedCsId + "/quizzes/" + reviewedQuizId + "/result"
                            : null,
                    null,
                    reviewedCsId,
                    reviewedClassSection != null ? reviewedClassSection.getTitle() : null,
                    "QUIZ_ATTEMPT",
                    attempt.getId(),
                    null
            );
        }
        if (Boolean.TRUE.equals(attempt.getIsPassed())) {
            markProgressIfPassed(attempt);
        }

        cacheInvalidationService.evictTeachingAndReportCaches();
        return convertToDetailResponse(attempt);
    }

    // =========================================================================
    // HELPER METHODS & STATISTICS
    // =========================================================================

    /*private void updateAttemptStatistics(QuizAttempt attempt) {
        List<QuizAttemptAnswer> attemptAnswers = quizAttemptAnswerRepository.findByAttempt_Id(attempt.getId());
        int answered = 0;
        int correct = 0;
        int incorrect = 0;

        for (QuizAttemptAnswer submitAnswer : attemptAnswers) {
            QuizQuestion question = submitAnswer.getQuestion();

             if (question.getType() == QuestionType.SHORT_ANSWER) {
                 String userAnswer = submitAnswer.getTextAnswer();
                 if (userAnswer != null && !userAnswer.trim().isEmpty()) {
                     answered++;
                    QuizAnswer correctAnswer = question.getAnswers().get(0);
                    boolean isCorrect = correctAnswer.getContent().trim().equalsIgnoreCase(userAnswer.trim());
                    submitAnswer.setIsCorrect(isCorrect);
                    if (isCorrect) correct++; else incorrect++;
                }
            } else if (question.getType() == QuestionType.MULTIPLE_CHOICE) {
                List<QuizAnswer> selectedAnswers = submitAnswer.getSelectedAnswers();
                if (selectedAnswers != null && !selectedAnswers.isEmpty()) {
                    answered++;
                    var correctIds = question.getAnswers().stream()
                            .filter(a -> Boolean.TRUE.equals(a.getIsCorrect()))
                            .map(QuizAnswer::getId).collect(java.util.stream.Collectors.toSet());
                    var selectedIds = selectedAnswers.stream()
                            .map(QuizAnswer::getId).collect(java.util.stream.Collectors.toSet());
                    boolean isCorrect = correctIds.equals(selectedIds);
                    submitAnswer.setIsCorrect(isCorrect);
                    if (isCorrect) correct++; else incorrect++;
                }
            }
        }
        attempt.setCorrectAnswers(correct);
        attempt.setIncorrectAnswers(incorrect);
        attempt.setUnansweredQuestions(attempt.getTotalQuestions() - answered);
        attempt.setGrade(attempt.getTotalQuestions() > 0 ? correct * 100 / attempt.getTotalQuestions() : 0);

        quizAttemptRepository.save(attempt);
        quizAttemptAnswerRepository.saveAll(attemptAnswers);
    }*/

    private void updateAttemptStatistics(QuizAttempt attempt) {
        List<QuizAttemptAnswer> attemptAnswers = quizAttemptAnswerRepository.findByAttempt_Id(attempt.getId());

        int answeredCount = 0;
        int fullCorrectCount = 0;
        int incorrectCount = 0;
        boolean needsManualReview = false;
        boolean hasReviewedManualAnswer = false;

        BigDecimal totalEarnedScore = BigDecimal.ZERO;
        BigDecimal totalMaxScore = BigDecimal.ZERO;

        for (QuizAttemptAnswer submitAnswer : attemptAnswers) {
            QuizAttemptQuestion attemptQuestion = submitAnswer.getAttemptQuestion();
            QuizQuestion quizQuestion = submitAnswer.getQuestion();

            QuestionType questionType = attemptQuestion != null ? attemptQuestion.getType() : quizQuestion.getType();
            BigDecimal questionPoints = attemptQuestion != null
                    ? (attemptQuestion.getPoints() != null ? attemptQuestion.getPoints() : BigDecimal.ONE)
                    : (quizQuestion.getPoints() != null ? quizQuestion.getPoints() : BigDecimal.ONE);
            totalMaxScore = totalMaxScore.add(questionPoints);
            submitAnswer.setMaxPoints(questionPoints);

            boolean answeredThisQuestion = false;
            boolean isFullScore = false;
            boolean isPendingManualReview = false;
            BigDecimal earnedPoints = BigDecimal.ZERO;

            // ===== 1. SINGLE CHOICE =====
            if (isSingleSelectQuestionType(questionType)) {
                if (attemptQuestion != null) {
                    List<QuizAttemptQuestionOption> selectedOptions = submitAnswer.getSelectedOptions();
                    if (selectedOptions != null && !selectedOptions.isEmpty()) {
                        answeredThisQuestion = true;
                        QuizAttemptQuestionOption correctOption = attemptQuestion.getOptions() == null
                                ? null
                                : attemptQuestion.getOptions().stream()
                                .filter(a -> Boolean.TRUE.equals(a.getIsCorrect()))
                                .findFirst()
                                .orElse(null);
                        if (correctOption != null && selectedOptions.get(0).getId().equals(correctOption.getId())) {
                            earnedPoints = questionPoints;
                            isFullScore = true;
                        }
                    }
                } else {
                    List<QuizAnswer> selectedAnswers = submitAnswer.getSelectedAnswers();
                    if (selectedAnswers != null && !selectedAnswers.isEmpty()) {
                        answeredThisQuestion = true;
                        QuizAnswer correctAnswer = quizQuestion.getAnswers().stream()
                                .filter(a -> Boolean.TRUE.equals(a.getIsCorrect()))
                                .findFirst()
                                .orElse(null);
                        if (correctAnswer != null && selectedAnswers.get(0).getId().equals(correctAnswer.getId())) {
                            earnedPoints = questionPoints;
                            isFullScore = true;
                        }
                    }
                }
                submitAnswer.setGradingStatus(GradingStatus.AUTO_GRADED);
            }
            // ===== 2. SHORT_ANSWER =====
            else if (questionType == QuestionType.SHORT_ANSWER) {
                String userAnswer = submitAnswer.getTextAnswer();
                if (userAnswer != null && !userAnswer.trim().isEmpty()) {
                    answeredThisQuestion = true;
                    List<String> acceptedAnswers = attemptQuestion != null
                            ? (attemptQuestion.getOptions() == null
                                    ? List.of()
                                    : attemptQuestion.getOptions().stream()
                                    .filter(opt -> Boolean.TRUE.equals(opt.getIsCorrect()))
                                    .map(QuizAttemptQuestionOption::getContent)
                                    .filter(Objects::nonNull)
                                    .map(String::trim)
                                    .filter(v -> !v.isEmpty())
                                    .toList())
                            : (quizQuestion.getAnswers() == null
                                    ? List.of()
                                    : quizQuestion.getAnswers().stream()
                                    .filter(ans -> Boolean.TRUE.equals(ans.getIsCorrect()))
                                    .map(QuizAnswer::getContent)
                                    .filter(Objects::nonNull)
                                    .map(String::trim)
                                    .filter(v -> !v.isEmpty())
                                    .toList());

                    if (!acceptedAnswers.isEmpty()
                            && acceptedAnswers.stream().anyMatch(ans -> ans.equalsIgnoreCase(userAnswer.trim()))) {
                        earnedPoints = questionPoints;
                        isFullScore = true;
                    }
                }
                submitAnswer.setGradingStatus(GradingStatus.AUTO_GRADED);
            }
            // ===== 2b. ESSAY / OPEN ENDED =====
            else if (questionType == QuestionType.ESSAY) {
                String userAnswer = submitAnswer.getTextAnswer();
                answeredThisQuestion = StringUtils.hasText(userAnswer);
                if (answeredThisQuestion) {
                    if (submitAnswer.getGradingStatus() == GradingStatus.REVIEWED
                            && submitAnswer.getManualScore() != null) {
                        hasReviewedManualAnswer = true;
                        earnedPoints = clampScore(submitAnswer.getManualScore(), questionPoints);
                        isFullScore = earnedPoints.compareTo(questionPoints) >= 0;
                    } else {
                        isPendingManualReview = true;
                        needsManualReview = true;
                        submitAnswer.setGradingStatus(GradingStatus.NEEDS_REVIEW);
                    }
                } else {
                    submitAnswer.setGradingStatus(GradingStatus.AUTO_GRADED);
                }
            }
            // ===== 3. MULTIPLE CHOICE =====
            else if (questionType == QuestionType.MULTIPLE_CHOICE) {
                if (attemptQuestion != null) {
                    List<QuizAttemptQuestionOption> selectedOptions = submitAnswer.getSelectedOptions();
                    if (selectedOptions != null && !selectedOptions.isEmpty()) {
                        answeredThisQuestion = true;

                        var systemCorrectIds = attemptQuestion.getOptions().stream()
                                .filter(a -> Boolean.TRUE.equals(a.getIsCorrect()))
                                .map(QuizAttemptQuestionOption::getId)
                                .collect(java.util.stream.Collectors.toSet());
                        int totalCorrectOptions = systemCorrectIds.size();

                        var userSelectedIds = selectedOptions.stream()
                                .map(QuizAttemptQuestionOption::getId)
                                .collect(java.util.stream.Collectors.toSet());

                        if (totalCorrectOptions > 0) {
                            long userRightCount = userSelectedIds.stream()
                                    .filter(systemCorrectIds::contains)
                                    .count();
                            long userWrongCount = userSelectedIds.size() - userRightCount;
                            long netCorrectCount = userRightCount - userWrongCount;

                            if (netCorrectCount > 0) {
                                double ratio = (double) netCorrectCount / totalCorrectOptions;
                                if (ratio >= 1.0) {
                                    earnedPoints = questionPoints;
                                    isFullScore = true;
                                } else if (ratio >= 0.5) {
                                    earnedPoints = questionPoints.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
                                }
                            }
                        }
                    }
                } else {
                    List<QuizAnswer> selectedAnswers = submitAnswer.getSelectedAnswers();
                    if (selectedAnswers != null && !selectedAnswers.isEmpty()) {
                        answeredThisQuestion = true;

                        var systemCorrectIds = quizQuestion.getAnswers().stream()
                                .filter(a -> Boolean.TRUE.equals(a.getIsCorrect()))
                                .map(QuizAnswer::getId)
                                .collect(java.util.stream.Collectors.toSet());
                        int totalCorrectOptions = systemCorrectIds.size();

                        var userSelectedIds = selectedAnswers.stream()
                                .map(QuizAnswer::getId)
                                .collect(java.util.stream.Collectors.toSet());

                        if (totalCorrectOptions > 0) {
                            long userRightCount = userSelectedIds.stream()
                                    .filter(systemCorrectIds::contains)
                                    .count();
                            long userWrongCount = userSelectedIds.size() - userRightCount;
                            long netCorrectCount = userRightCount - userWrongCount;

                            if (netCorrectCount > 0) {
                                double ratio = (double) netCorrectCount / totalCorrectOptions;
                                if (ratio >= 1.0) {
                                    earnedPoints = questionPoints;
                                    isFullScore = true;
                                } else if (ratio >= 0.5) {
                                    earnedPoints = questionPoints.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
                                }
                            }
                        }
                    }
                }
                submitAnswer.setGradingStatus(GradingStatus.AUTO_GRADED);
            }
            // ===== 4. INTERACTION QUESTIONS =====
            else if (isInteractionQuestionType(questionType) && attemptQuestion != null) {
                InteractionGrade interactionGrade = gradeInteractionQuestion(submitAnswer, attemptQuestion, questionType, questionPoints);
                answeredThisQuestion = interactionGrade.answered();
                isFullScore = interactionGrade.fullScore();
                earnedPoints = interactionGrade.earnedPoints().setScale(2, RoundingMode.HALF_UP);
                submitAnswer.setGradingStatus(GradingStatus.AUTO_GRADED);
            }

            if (answeredThisQuestion) {
                answeredCount++;
            }

            submitAnswer.setEarnedPoints(earnedPoints);
            submitAnswer.setIsCorrect(isPendingManualReview ? null : isFullScore);
            totalEarnedScore = totalEarnedScore.add(earnedPoints);

            if (!isPendingManualReview && earnedPoints.compareTo(questionPoints) >= 0) {
                fullCorrectCount++;
            } else if (!isPendingManualReview && answeredThisQuestion && earnedPoints.compareTo(BigDecimal.ZERO) == 0) {
                incorrectCount++;
            }
        }

        attempt.setCorrectAnswers(fullCorrectCount);
        attempt.setIncorrectAnswers(incorrectCount);
        attempt.setUnansweredQuestions(attempt.getTotalQuestions() != null
                ? attempt.getTotalQuestions() - answeredCount
                : 0);

        attempt.setEarnedPoints(totalEarnedScore);
        attempt.setTotalPoints(totalMaxScore);
        if (needsManualReview) {
            attempt.setGradingStatus(GradingStatus.NEEDS_REVIEW);
        } else if (hasReviewedManualAnswer) {
            attempt.setGradingStatus(GradingStatus.REVIEWED);
        } else {
            attempt.setGradingStatus(GradingStatus.AUTO_GRADED);
        }

        if (totalMaxScore.compareTo(BigDecimal.ZERO) > 0) {
            int finalGrade = totalEarnedScore
                    .multiply(BigDecimal.valueOf(100))
                    .divide(totalMaxScore, 0, RoundingMode.HALF_UP)
                    .intValue();
            attempt.setGrade(finalGrade);
        } else {
            attempt.setGrade(0);
        }

        quizAttemptRepository.save(attempt);
        quizAttemptAnswerRepository.saveAll(attemptAnswers);
    }

    private InteractionGrade gradeInteractionQuestion(
            QuizAttemptAnswer submitAnswer,
            QuizAttemptQuestion attemptQuestion,
            QuestionType questionType,
            BigDecimal questionPoints
    ) {
        List<QuizAttemptAnswerItem> answerItems = submitAnswer.getAnswerItems() != null
                ? submitAnswer.getAnswerItems()
                : quizAttemptAnswerItemRepository.findByAttemptAnswer_IdOrderByIdAsc(submitAnswer.getId());
        answerItems = answerItems.stream()
                .filter(item -> !isContentBlockAnswerItem(item))
                .toList();
        boolean answered = answerItems.stream().anyMatch(item -> hasMeaningfulInteractionAnswer(item, questionType));
        if (!answered) {
            return new InteractionGrade(false, false, BigDecimal.ZERO);
        }

        double ratio = 0.0;
        if (isMatchingQuestionType(questionType)) {
            ratio = gradeMatchingQuestion(attemptQuestion, answerItems);
        } else if (questionType == QuestionType.DRAG_ORDER) {
            ratio = gradeDragOrderQuestion(attemptQuestion, answerItems);
        } else if (questionType == QuestionType.CLOZE) {
            ratio = gradeClozeQuestion(attemptQuestion, answerItems);
        }

        boolean fullScore = ratio >= 1.0;
        BigDecimal earnedPoints = fullScore
                ? questionPoints
                : questionPoints.multiply(BigDecimal.valueOf(ratio));
        return new InteractionGrade(true, fullScore, earnedPoints);
    }

    private double gradeMatchingQuestion(
            QuizAttemptQuestion attemptQuestion,
            List<QuizAttemptAnswerItem> answerItems
    ) {
        List<QuizAttemptQuestionItem> prompts = getAttemptQuestionItems(attemptQuestion).stream()
                .filter(item -> item.getRole() == QuestionInteractionItemRole.PROMPT)
                .toList();
        if (prompts.isEmpty()) {
            return 0.0;
        }

        Map<Integer, QuizAttemptAnswerItem> answersByPromptId = mapAnswersByQuestionItemId(answerItems);
        int correctCount = 0;
        for (QuizAttemptQuestionItem prompt : prompts) {
            QuizAttemptAnswerItem answerItem = answersByPromptId.get(prompt.getId());
            boolean correct = answerItem != null
                    && answerItem.getSelectedItem() != null
                    && Objects.equals(prompt.getCorrectMatchKey(), answerItem.getSelectedItem().getItemKey());
            if (answerItem != null) {
                answerItem.setIsCorrect(correct);
            }
            if (correct) {
                correctCount++;
            }
        }
        return (double) correctCount / prompts.size();
    }

    private double gradeDragOrderQuestion(
            QuizAttemptQuestion attemptQuestion,
            List<QuizAttemptAnswerItem> answerItems
    ) {
        List<QuizAttemptQuestionItem> orderItems = getAttemptQuestionItems(attemptQuestion).stream()
                .filter(item -> item.getRole() == QuestionInteractionItemRole.ORDER_ITEM)
                .toList();
        if (orderItems.isEmpty()) {
            return 0.0;
        }

        Map<Integer, QuizAttemptAnswerItem> answersByItemId = mapAnswersByQuestionItemId(answerItems);
        int correctCount = 0;
        for (QuizAttemptQuestionItem orderItem : orderItems) {
            QuizAttemptAnswerItem answerItem = answersByItemId.get(orderItem.getId());
            boolean correct = answerItem != null
                    && Objects.equals(orderItem.getCorrectOrderIndex(), answerItem.getSubmittedOrderIndex());
            if (answerItem != null) {
                answerItem.setIsCorrect(correct);
            }
            if (correct) {
                correctCount++;
            }
        }
        return (double) correctCount / orderItems.size();
    }

    private double gradeClozeQuestion(
            QuizAttemptQuestion attemptQuestion,
            List<QuizAttemptAnswerItem> answerItems
    ) {
        List<QuizAttemptQuestionItem> blanks = getAttemptQuestionItems(attemptQuestion).stream()
                .filter(item -> item.getRole() == QuestionInteractionItemRole.BLANK)
                .toList();
        if (blanks.isEmpty()) {
            return 0.0;
        }

        Map<Integer, QuizAttemptAnswerItem> answersByItemId = mapAnswersByQuestionItemId(answerItems);
        int correctCount = 0;
        for (QuizAttemptQuestionItem blank : blanks) {
            QuizAttemptAnswerItem answerItem = answersByItemId.get(blank.getId());
            List<String> acceptedAnswers = parseAcceptedAnswers(blank.getAcceptedAnswers());
            String submittedAnswer = answerItem != null ? normalizeAnswer(answerItem.getAnswerText()) : "";
            boolean correct = answerItem != null
                    && !submittedAnswer.isEmpty()
                    && acceptedAnswers.stream().map(this::normalizeAnswer).anyMatch(submittedAnswer::equals);
            if (answerItem != null) {
                answerItem.setIsCorrect(correct);
            }
            if (correct) {
                correctCount++;
            }
        }
        return (double) correctCount / blanks.size();
    }

    private List<QuizAttemptQuestionItem> getAttemptQuestionItems(QuizAttemptQuestion attemptQuestion) {
        if (attemptQuestion.getItems() != null && !attemptQuestion.getItems().isEmpty()) {
            return attemptQuestion.getItems();
        }
        return quizAttemptQuestionItemRepository.findByAttemptQuestion_IdOrderByOrderIndexAsc(attemptQuestion.getId());
    }

    private Map<Integer, QuizAttemptAnswerItem> mapAnswersByQuestionItemId(List<QuizAttemptAnswerItem> answerItems) {
        Map<Integer, QuizAttemptAnswerItem> answersByItemId = new HashMap<>();
        for (QuizAttemptAnswerItem answerItem : answerItems) {
            if (answerItem.getAttemptQuestionItem() != null && answerItem.getAttemptQuestionItem().getId() != null) {
                answersByItemId.put(answerItem.getAttemptQuestionItem().getId(), answerItem);
            }
        }
        return answersByItemId;
    }

    private boolean hasMeaningfulInteractionAnswer(QuizAttemptAnswerItem answerItem, QuestionType questionType) {
        if (isMatchingQuestionType(questionType)) {
            return answerItem.getSelectedItem() != null;
        }
        if (questionType == QuestionType.DRAG_ORDER) {
            return answerItem.getSubmittedOrderIndex() != null;
        }
        if (questionType == QuestionType.CLOZE) {
            return StringUtils.hasText(answerItem.getAnswerText());
        }
        return false;
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

    private boolean isSingleSelectQuestionType(QuestionType type) {
        return type == QuestionType.SINGLE_CHOICE
                || type == QuestionType.TRUE_FALSE
                || type == QuestionType.IMAGE_ANSWERING;
    }

    private boolean isTextAnswerQuestionType(QuestionType type) {
        return type == QuestionType.SHORT_ANSWER
                || type == QuestionType.ESSAY;
    }

    private BigDecimal clampScore(BigDecimal score, BigDecimal maxScore) {
        if (score == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal normalized = score.setScale(2, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (maxScore != null && normalized.compareTo(maxScore) > 0) {
            return maxScore.setScale(2, RoundingMode.HALF_UP);
        }
        return normalized;
    }

    private String normalizeAnswer(String answer) {
        return answer == null ? "" : answer.trim().toLowerCase(Locale.ROOT);
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

    private record InteractionGrade(boolean answered, boolean fullScore, BigDecimal earnedPoints) {
    }

    /**
     * Tự động hết hạn các bài làm quá giờ (status IN_PROGRESS, quiz CÓ timeLimit).
     * Chạy mỗi 5 phút, KHÔNG phụ thuộc việc HS có online / chạm vào bài hay không
     * → tránh attempt "treo" IN_PROGRESS và đảm bảo notif QUIZ_ATTEMPT_EXPIRED gửi đúng lúc.
     * expireAttempt() set completedTime = startTime + timeLimit nên thời lượng vẫn chính xác dù quét trễ.
     */
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void autoExpireTimedOutAttempts() {
        List<QuizAttempt> candidates =
                quizAttemptRepository.findByStatusAndQuiz_TimeLimitMinutesNotNull(AttemptStatus.IN_PROGRESS);
        for (QuizAttempt attempt : candidates) {
            if (!isAttemptExpired(attempt)) {
                continue;
            }
            // Cô lập từng bài: 1 bài lỗi chỉ bị bỏ qua + log, không kéo cả batch rollback/kẹt
            try {
                expireAttempt(attempt);
            } catch (Exception e) {
                log.error("Auto-expire thất bại cho quiz attempt {}", attempt.getId(), e);
            }
        }
    }

    private boolean isAttemptExpired(QuizAttempt attempt) {
        if (attempt.getQuiz().getTimeLimitMinutes() == null) return false;
        LocalDateTime expirationTime = attempt.getStartTime().plusMinutes(attempt.getQuiz().getTimeLimitMinutes());
        return LocalDateTime.now().isAfter(expirationTime);
    }

    private QuizAttempt expireAttemptIfTimedOut(QuizAttempt attempt) {
        if (attempt == null) {
            return null;
        }
        if (attempt.getStatus() == AttemptStatus.IN_PROGRESS && isAttemptExpired(attempt)) {
            expireAttempt(attempt);
        }
        return attempt;
    }

    private Long calculateRemainingTimeSeconds(QuizAttempt attempt) {
        // If no time limit, return null
        if (attempt.getQuiz().getTimeLimitMinutes() == null || attempt.getQuiz().getTimeLimitMinutes() <= 0) {
            return null;
        }
        
        // If attempt has been completed or expired, remaining time is 0
        if (attempt.getCompletedTime() != null || attempt.getStatus() == AttemptStatus.EXPIRED) {
            return 0L;
        }
        
        // Calculate elapsed time in seconds
        long elapsedSeconds = ChronoUnit.SECONDS.between(attempt.getStartTime(), LocalDateTime.now());
        
        // Calculate total time limit in seconds
        long totalSeconds = (long) attempt.getQuiz().getTimeLimitMinutes() * 60;
        
        // Calculate remaining time
        long remainingSeconds = totalSeconds - elapsedSeconds;
        
        // Return 0 if time has already expired
        return Math.max(0L, remainingSeconds);
    }

    private void expireAttempt(QuizAttempt attempt) {
        updateAttemptStatistics(attempt);
        // Mốc nộp = thời điểm hết hạn thật (startTime + timeLimit), KHÔNG dùng now():
        // expire là lazy nên nếu attempt bị phát hiện hết giờ muộn (vài ngày sau),
        // dùng now() sẽ khiến "thời gian làm bài" hiển thị sai lệch tới vài ngày.
        Integer timeLimitMinutes = attempt.getQuiz().getTimeLimitMinutes();
        LocalDateTime expiredAt = timeLimitMinutes != null
                ? attempt.getStartTime().plusMinutes(timeLimitMinutes)
                : LocalDateTime.now();
        attempt.setCompletedTime(expiredAt);
        attempt.setStatus(AttemptStatus.EXPIRED);
        Integer passingScore = attempt.getQuiz().getMinPassScore() != null ? attempt.getQuiz().getMinPassScore() : 0;
        attempt.setIsPassed(attempt.getGrade() >= passingScore);
        quizAttemptRepository.save(attempt);
        if (Boolean.TRUE.equals(attempt.getIsPassed())) {
            markProgressIfPassed(attempt);
        }
        notifyAttemptExpired(attempt);
    }

    /** Báo cho HS khi bài quiz bị hệ thống tự nộp do hết thời gian làm bài. */
    private void notifyAttemptExpired(QuizAttempt attempt) {
        if (attempt.getStudent() == null) {
            return;
        }
        ClassSection classSection = resolveClassSection(attempt);
        Integer csId = classSection != null ? classSection.getId() : null;
        Integer quizId = attempt.getQuiz() != null ? attempt.getQuiz().getId() : null;
        notificationService.createNotification(
                attempt.getStudent(),
                "Bài quiz đã tự nộp do hết giờ",
                "Hệ thống đã tự động nộp bài quiz của bạn vì đã hết thời gian làm bài.",
                "QUIZ_ATTEMPT_EXPIRED",
                null,
                (csId != null && quizId != null)
                        ? "/class-sections/" + csId + "/quizzes/" + quizId + "/result"
                        : null,
                null,
                csId,
                classSection != null ? classSection.getTitle() : null,
                "QUIZ_ATTEMPT",
                attempt.getId(),
                null
        );
    }

    // Láº¤Y Káº¾T QUáº¢ Cá»¦A Báº¢N THÃ‚N SINH VIÃŠN THEO 1 QUIZ
    @Override
    public List<QuizAttemptResponse> getStudentAttemptsHistory(Integer chapterItemId) {
        throw new UnsupportedOperationException("Legacy chapter-item quiz flow has been removed");
    }


    // Láº¤Y Káº¾T QUáº¢ Cá»¦A Táº¤T Cáº¢ Má»ŒI NGÆ¯á»œI THEO 1 QUIZ
    @Override
    public List<QuizAttemptResponse> getStudentAttemptsHistoryForClassContentItem(Integer classContentItemId) {
        User currentUser = userService.getCurrentUser();
        return quizAttemptRepository.findByClassContentItem_IdAndStudent_Id(classContentItemId, currentUser.getId())
                .stream()
                .filter(a -> a.getStatus() == AttemptStatus.COMPLETED || a.getStatus() == AttemptStatus.EXPIRED)
                .map(this::convertQuizAttemptToDTO)
                .toList();
    }

    @Override
    public PageResponse<QuizAttemptResponse> getAttemptsForTeacherOrAdmin(Integer chapterItemId, Pageable pageable) {
        throw new UnsupportedOperationException("Legacy chapter-item quiz flow has been removed");
    }

    //Láº¤Y Káº¾T QUáº¢

    @Override
    public PageResponse<QuizAttemptResponse> getAttemptsForTeacherOrAdminByClassContentItem(Integer classContentItemId, Pageable pageable) {
        User currentUser = userService.getCurrentUser();
        ClassContentItem classContentItem = classContentItemRepository.findById(classContentItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Class content item not found"));

        if (!canReviewClassSection(classContentItem.getClassChapter().getClassSection(), currentUser)
                && currentUser.getRole().getRoleName() != RoleType.ADMIN) {
            throw new UnauthorizedException("Bạn không có quyền xem lịch sử bài làm");
        }

        Page<QuizAttemptResponse> dtoPage = quizAttemptRepository.findByClassContentItem_IdAndStatusIn(
                classContentItemId,
                List.of(AttemptStatus.COMPLETED, AttemptStatus.EXPIRED),
                pageable
        ).map(this::convertQuizAttemptToDTO);

        return new PageResponse<>(dtoPage.getNumber() + 1, dtoPage.getTotalPages(), dtoPage.getNumberOfElements(), dtoPage.getContent());
    }

    @Override
    public Integer getStudentBestScore(Integer chapterItemId) {
        throw new UnsupportedOperationException("Legacy chapter-item quiz flow has been removed");
    }

    @Override
    public Integer getStudentBestScoreForClassContentItem(Integer classContentItemId) {
        User currentUser = userService.getCurrentUser();
        Integer maxGrade = quizAttemptRepository.findMaxGradeByClassContentItemAndStudent(classContentItemId, currentUser.getId());
        return maxGrade == null ? 0 : maxGrade;
    }

    private void checkQuizAvailability(Quiz quiz) {
        LocalDateTime now = LocalDateTime.now();

        if (quiz.getAvailableFrom() != null && now.isBefore(quiz.getAvailableFrom())) {
            throw new BusinessException("ChÆ°a Ä‘áº¿n giá» lÃ m bÃ i");
        }

        if (quiz.getAvailableUntil() != null && now.isAfter(quiz.getAvailableUntil())) {
            throw new BusinessException("ÄÃ£ háº¿t háº¡n lÃ m bÃ i");
        }
    }

    // =========================================================================
    // MAPPERS & SECURITY LOGIC (QUAN TRá»ŒNG)
    // =========================================================================

    /**
     * Logic quyáº¿t Ä‘á»‹nh xem user cÃ³ Ä‘Æ°á»£c phÃ©p nhÃ¬n tháº¥y Ä‘Ã¡p Ã¡n Ä‘Ãºng hay khÃ´ng?
     * @return true: ÄÆ°á»£c xem (GV, Admin, hoáº·c HS Ä‘Ã£ ná»™p bÃ i)
     * @return false: Bá»‹ áº©n (HS Ä‘ang lÃ m bÃ i)
     */
    private boolean shouldShowCorrectAnswers(QuizAttempt attempt) {
        User currentUser = userService.getCurrentUser();

        // 1. Admin luÃ´n xem Ä‘Æ°á»£c
        if (currentUser.getRole().getRoleName() == RoleType.ADMIN) return true;

        // 2. GiÃ¡o viÃªn sá»Ÿ há»¯u khÃ³a há»c luÃ´n xem Ä‘Æ°á»£c
        if (canReviewAttempt(attempt, currentUser)) return true;

        // 3. Há»c sinh: Chá»‰ xem Ä‘Æ°á»£c khi Ä‘Ã£ Ná»™p bÃ i hoáº·c Háº¿t giá»
        if (attempt.getStudent().getId().equals(currentUser.getId())) {
            return attempt.getQuiz().isShowCorrectAnswer()
                    && (attempt.getStatus() == AttemptStatus.COMPLETED
                    || attempt.getStatus() == AttemptStatus.EXPIRED);
        }

        return false;
    }

    // =========================================================================
    // GRADEBOOK / Báº¢NG ÄIá»‚M
    // =========================================================================

    /**
     * API cho Sinh viÃªn xem báº£ng Ä‘iá»ƒm cÃ¡ nhÃ¢n cá»§a mÃ¬nh (Táº¥t cáº£ quiz Ä‘Ã£ lÃ m)
     */

    @Override
    public List<StudentQuizResultResponse> getMyGradeBook(Integer courseId) {
        throw new UnsupportedOperationException("Legacy course gradebook flow has been removed");
    }
    /**
     * API cho GiÃ¡o viÃªn/Admin xem báº£ng Ä‘iá»ƒm cá»§a khÃ³a há»c
     */
    @Override
    public List<ClassSectionStudentQuizResultResponse> getMyGradeBookForClassSection(Integer classSectionId) {
        User currentUser = userService.getCurrentUser();

        boolean isEnrolled = enrollmentRepository.existsByStudent_IdAndClassSection_IdAndApprovalStatus(
                currentUser.getId(), classSectionId, EnrollmentStatus.APPROVED);
        if (!isEnrolled) {
            throw new UnauthorizedException("Bạn không có quyền truy cập vào tài nguyên này!");
        }

        return quizAttemptRepository.findMaxGradesByStudentAndClassSection(currentUser.getId(), classSectionId);
    }


    @Override
    @Cacheable(value = CacheNames.QUIZ_GRADEBOOK_COURSE, key = "@cacheKeyBuilder.courseGradeBookKey(#courseId)", sync = true)
    public List<CourseQuizResultResponse> getCourseGradeBook(Integer courseId) {
        throw new UnsupportedOperationException("Legacy course gradebook flow has been removed");
    }

    @Override
    @Cacheable(value = CacheNames.QUIZ_GRADEBOOK_CLASS_SECTION, key = "@cacheKeyBuilder.classSectionGradeBookKey(#classSectionId)", sync = true)
    public List<ClassSectionQuizGradeResponse> getClassSectionGradeBook(Integer classSectionId) {
        User currentUser = userService.getCurrentUser();
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));

        if (!classMemberAuthorizationService.canViewProgress(classSection, currentUser)
                && currentUser.getRole().getRoleName() != RoleType.ADMIN) {
            throw new UnauthorizedException("Bạn không có quyền xem bảng điểm của lớp học này");
        }

        return quizAttemptRepository.findMaxGradesByClassSection(classSectionId);
    }

    private Quiz resolveQuizFromClassContentItem(ClassContentItem classContentItem) {
        if (classContentItem.getItemType() != ContentItemType.QUIZ) {
            throw new BusinessException("Nội dung này không phải quiz");
        }

        Quiz quiz = classContentItem.getQuiz();
        if (quiz == null) {
            throw new ResourceNotFoundException("Quiz chua duoc gan vao class content item");
        }
        return quiz;
    }

    private ClassSection resolveClassSection(QuizAttempt attempt) {
        if (attempt == null) {
            return null;
        }
        if (attempt.getClassContentItem() != null
                && attempt.getClassContentItem().getClassChapter() != null) {
            return attempt.getClassContentItem().getClassChapter().getClassSection();
        }
        return null;
    }

    private void ensureClassSectionInteractive(ClassSection classSection) {
        ClassSectionGuard.ensureInteractive(classSection);
    }

    private void markProgressIfPassed(QuizAttempt attempt) {
        if (!Boolean.TRUE.equals(attempt.getIsPassed())) {
            return;
        }

        User student = attempt.getStudent();
        if (attempt.getClassContentItem() != null) {
            ClassContentItem classContentItem = attempt.getClassContentItem();
            Progress progress = progressRepository
                    .findByStudent_IdAndClassContentItem_Id(student.getId(), classContentItem.getId())
                    .orElse(Progress.builder()
                            .student(student)
                            .classContentItem(classContentItem)
                            .isCompleted(false)
                            .build());

            if (!Boolean.TRUE.equals(progress.getIsCompleted())) {
                progress.setIsCompleted(true);
                progress.setCompletedAt(LocalDateTime.now());
                progressRepository.save(progress);
                enrollmentService.recalculateAndSaveProgressForClassSection(
                        student.getId(),
                        classContentItem.getClassChapter().getClassSection().getId()
                );
            }
        }
    }

    private Integer resolveClassSectionId(QuizAttempt attempt) {
        if (attempt.getClassContentItem() != null
                && attempt.getClassContentItem().getClassChapter() != null
                && attempt.getClassContentItem().getClassChapter().getClassSection() != null) {
            return attempt.getClassContentItem().getClassChapter().getClassSection().getId();
        }
        return null;
    }

    private QuestionType resolveAnswerQuestionType(QuizAttemptAnswer answer) {
        if (answer == null) {
            return null;
        }
        if (answer.getAttemptQuestion() != null) {
            return answer.getAttemptQuestion().getType();
        }
        return answer.getQuestion() != null ? answer.getQuestion().getType() : null;
    }

    private BigDecimal resolveAnswerPointValue(QuizAttemptAnswer answer) {
        if (answer == null) {
            return BigDecimal.ONE;
        }
        if (answer.getAttemptQuestion() != null) {
            return answer.getAttemptQuestion().getPoints() != null ? answer.getAttemptQuestion().getPoints() : BigDecimal.ONE;
        }
        if (answer.getQuestion() != null) {
            return answer.getQuestion().getPoints() != null ? answer.getQuestion().getPoints() : BigDecimal.ONE;
        }
        return BigDecimal.ONE;
    }

    private boolean canReviewAttempt(QuizAttempt attempt, User currentUser) {
        if (currentUser.getRole().getRoleName() == RoleType.ADMIN) {
            return true;
        }

        if (attempt.getClassContentItem() != null
                && attempt.getClassContentItem().getClassChapter() != null
                && attempt.getClassContentItem().getClassChapter().getClassSection() != null) {
            return canReviewClassSection(attempt.getClassContentItem().getClassChapter().getClassSection(), currentUser);
        }

        return false;
    }

    private boolean canReviewClassSection(ClassSection classSection, User currentUser) {
        return classMemberAuthorizationService.canReviewQuizzes(classSection, currentUser);
    }

    private QuizAttemptDetailResponse convertToDetailResponse(QuizAttempt attempt) {
        QuizAttemptDetailResponse response = new QuizAttemptDetailResponse();

        // Map basic fields
        response.setId(attempt.getId());
        response.setQuizId(attempt.getQuiz().getId());
        response.setStudentId(attempt.getStudent().getId());
        response.setClassContentItemId(attempt.getClassContentItem() != null ? attempt.getClassContentItem().getId() : null);
        response.setClassSectionId(resolveClassSectionId(attempt));
        response.setStartTime(attempt.getStartTime());
        response.setGrade(attempt.getGrade());
        response.setIsPassed(attempt.getIsPassed());
        response.setCompletedTime(attempt.getCompletedTime());
        response.setTotalQuestions(attempt.getTotalQuestions());
        response.setCorrectAnswers(attempt.getCorrectAnswers());
        response.setIncorrectAnswers(attempt.getIncorrectAnswers());
        response.setUnansweredQuestions(attempt.getUnansweredQuestions());
        response.setEarnedPoints(attempt.getEarnedPoints());
        response.setTotalPoints(attempt.getTotalPoints());
        response.setGradingStatus(attempt.getGradingStatus());
        response.setInstructorFeedback(attempt.getInstructorFeedback());
        response.setQuizTitle(attempt.getQuiz() != null ? attempt.getQuiz().getTitle() : null);
        response.setStudentName(attempt.getStudent() != null ? attempt.getStudent().getFullName() : null);
        response.setStudentEmail(attempt.getStudent() != null ? attempt.getStudent().getGmail() : null);
        if (attempt.getClassContentItem() != null
                && attempt.getClassContentItem().getClassChapter() != null
                && attempt.getClassContentItem().getClassChapter().getClassSection() != null) {
            response.setClassSectionTitle(attempt.getClassContentItem().getClassChapter().getClassSection().getTitle());
        }
        
        // Calculate remaining time (in seconds)
        response.setRemainingTimeSeconds(calculateRemainingTimeSeconds(attempt));

        // QUAN TRá»ŒNG: Kiá»ƒm tra quyá»n Ä‘á»ƒ áº©n/hiá»‡n Ä‘Ã¡p Ã¡n
        boolean showCorrectAnswer = shouldShowCorrectAnswers(attempt);

        // Fetch answers vÃ  map vá»›i cá» báº£o máº­t
        List<QuizAttemptAnswer> answers = quizAttemptAnswerRepository.findByAttempt_Id(attempt.getId());
        answers.sort(Comparator.comparingInt(answer -> {
            if (answer.getAttemptQuestion() != null && answer.getAttemptQuestion().getOrderIndex() != null) {
                return answer.getAttemptQuestion().getOrderIndex();
            }
            if (answer.getQuestion() != null && answer.getQuestion().getId() != null) {
                return answer.getQuestion().getId();
            }
            return Integer.MAX_VALUE;
        }));
        response.setAnswers(
                answers.stream()
                        .map(ans -> this.convertQuizAttemptAnswerToDTO(ans, showCorrectAnswer))
                        .toList()
        );

        return response;
    }

    private QuizAttemptAnswerResponse convertQuizAttemptAnswerToDTO(QuizAttemptAnswer entity, boolean showCorrectAnswer) {
        if (entity == null) return null;

        QuizAttemptAnswerResponse response = new QuizAttemptAnswerResponse();
        response.setId(entity.getId());
        response.setMaxPoints(entity.getMaxPoints());
        response.setEarnedPoints(entity.getEarnedPoints());
        response.setGradingStatus(entity.getGradingStatus());
        response.setTeacherFeedback(entity.getTeacherFeedback());
        response.setReviewedAt(entity.getReviewedAt());

        QuizAttemptQuestion attemptQuestion = entity.getAttemptQuestion();
        QuizQuestion question = entity.getQuestion();
        QuestionType questionType = attemptQuestion != null ? attemptQuestion.getType() : (question != null ? question.getType() : null);

        // Truyá»n cá» showCorrectAnswer xuá»‘ng Question Ä‘á»ƒ map
        response.setQuizQuestion(attemptQuestion != null
                ? convertAttemptQuestionToDTO(attemptQuestion, showCorrectAnswer)
                : convertQuizQuestionToDTO(question, showCorrectAnswer));

        // Logic áº©n/hiá»‡n káº¿t quáº£ Ä‘Ãºng sai cá»§a chÃ­nh cÃ¢u tráº£ lá»i nÃ y
        if (showCorrectAnswer) {
            response.setIsCorrect(entity.getIsCorrect()); // Hiá»‡n khi Ä‘Ã£ ná»™p
        } else {
            response.setIsCorrect(null); // áº¨n khi Ä‘ang lÃ m
        }

        if (isInteractionQuestionType(questionType)) {
            response.setAnswerItems(convertAttemptAnswerItems(entity, showCorrectAnswer));
            response.setSelectedAnswers(null);
            response.setTextAnswer(null);
        } else if (isTextAnswerQuestionType(questionType)) {
            response.setTextAnswer(entity.getTextAnswer());
            response.setSelectedAnswers(null);
            response.setAnswerItems(List.of());
        } else {
            if (attemptQuestion != null) {
                response.setSelectedAnswers(
                        entity.getSelectedOptions() == null
                                ? List.of()
                                : entity.getSelectedOptions().stream()
                                .map(opt -> this.convertAttemptOptionToDTO(opt, showCorrectAnswer))
                                .toList()
                );
            } else {
                response.setSelectedAnswers(
                        entity.getSelectedAnswers() == null
                                ? List.of()
                                : entity.getSelectedAnswers().stream()
                                // Map selected answers (váº«n pháº£i truyá»n cá» Ä‘á»ƒ áº©n isCorrect bÃªn trong nÃ³)
                                .map(ans -> this.convertQuizAnswerToDTO(ans, showCorrectAnswer))
                                .toList()
                );
            }
            response.setTextAnswer(null);
            response.setAnswerItems(List.of());
        }
        return response;
    }

    private List<QuizAttemptAnswerItemResponse> convertAttemptAnswerItems(
            QuizAttemptAnswer entity,
            boolean showCorrectAnswer
    ) {
        List<QuizAttemptAnswerItem> answerItems = entity.getAnswerItems() != null
                ? entity.getAnswerItems()
                : quizAttemptAnswerItemRepository.findByAttemptAnswer_IdOrderByIdAsc(entity.getId());
        return answerItems.stream()
                .filter(item -> !isContentBlockAnswerItem(item))
                .map(item -> new QuizAttemptAnswerItemResponse(
                        item.getId(),
                        item.getAttemptQuestionItem() != null ? item.getAttemptQuestionItem().getId() : null,
                        item.getSelectedItem() != null ? item.getSelectedItem().getId() : null,
                        item.getAnswerText(),
                        item.getSubmittedOrderIndex(),
                        item.getBlankIndex(),
                        item.getBlankKey(),
                        showCorrectAnswer ? item.getIsCorrect() : null
                ))
                .toList();
    }

    private List<QuizAttemptAnswerItemResponse> convertContentBlockAnswerItems(QuizAttemptAnswer entity) {
        List<QuizAttemptAnswerItem> answerItems = entity.getAnswerItems() != null
                ? entity.getAnswerItems()
                : quizAttemptAnswerItemRepository.findByAttemptAnswer_IdOrderByIdAsc(entity.getId());
        return answerItems.stream()
                .filter(this::isContentBlockAnswerItem)
                .map(item -> new QuizAttemptAnswerItemResponse(
                        item.getId(),
                        null,
                        null,
                        item.getAnswerText(),
                        null,
                        null,
                        item.getBlankKey(),
                        null
                ))
                .toList();
    }

    private QuizQuestionResponse convertAttemptQuestionToDTO(QuizAttemptQuestion question, boolean showCorrectAnswer) {
        if (question == null) return null;
        QuizQuestionResponse response = new QuizQuestionResponse();
        response.setId(question.getId());
        response.setSourceBankQuestionId(question.getSourceBankQuestion() != null
                ? question.getSourceBankQuestion().getId()
                : (question.getSourceQuizQuestion() != null && question.getSourceQuizQuestion().getSourceBankQuestion() != null
                ? question.getSourceQuizQuestion().getSourceBankQuestion().getId()
                : null));
        response.setContent(sanitizeQuestionContent(question.getType(), question.getContent(), showCorrectAnswer));
        response.setType(question.getType());
        response.setPoints(question.getPoints());
        response.setResource(convertResourceToDTO(question.getResource()));
        response.setAnswers(question.getOptions() != null
                ? question.getOptions().stream().map(opt -> this.convertAttemptOptionToDTO(opt, showCorrectAnswer)).toList()
                : List.of());
        response.setItems(getAttemptQuestionItems(question).stream()
                .map(item -> this.convertAttemptItemToDTO(item, showCorrectAnswer))
                .toList());
        return response;
    }

    private QuizQuestionResponse convertQuizQuestionToDTO(QuizQuestion question, boolean showCorrectAnswer) {
        if (question == null) return null;
        QuizQuestionResponse response = new QuizQuestionResponse();
        response.setId(question.getId());
        response.setSourceBankQuestionId(question.getSourceBankQuestion() != null ? question.getSourceBankQuestion().getId() : null);
        response.setContent(sanitizeQuestionContent(question.getType(), question.getContent(), showCorrectAnswer));
        response.setType(question.getType());
        response.setResource(convertResourceToDTO(question.getResource()));
        response.setPoints(question.getPoints());

        if (question.getAnswers() != null) {
            response.setAnswers(
                    question.getAnswers().stream()
                            .map(ans -> this.convertQuizAnswerToDTO(ans, showCorrectAnswer))
                            .toList()
            );
        }
        response.setItems(question.getInteractionItems() != null
                ? question.getInteractionItems().stream()
                .map(item -> this.convertQuestionItemToDTO(item, showCorrectAnswer))
                .toList()
                : List.of());
        return response;
    }

    private QuestionInteractionItemResponse convertAttemptItemToDTO(
            QuizAttemptQuestionItem item,
            boolean showCorrectAnswer
    ) {
        if (item == null) return null;
        return new QuestionInteractionItemResponse(
                item.getId(),
                resolveInteractionItemContent(item.getRole(), item.getContent(), showCorrectAnswer),
                item.getItemKey(),
                item.getRole(),
                showCorrectAnswer ? item.getCorrectMatchKey() : null,
                showCorrectAnswer ? item.getCorrectOrderIndex() : null,
                item.getBlankIndex(),
                showCorrectAnswer ? parseAcceptedAnswers(item.getAcceptedAnswers()) : null,
                item.getSourceItem() != null ? item.getSourceItem().getBlankType() : null,
                item.getSourceItem() != null ? item.getSourceItem().getBlankOptions() : null,
                item.getResource() != null ? item.getResource().getId() : null,
                convertResourceToDTO(item.getResource()),
                item.getOrderIndex()
        );
    }

    private QuizAnswerResponse convertAttemptOptionToDTO(QuizAttemptQuestionOption option, boolean showCorrectAnswer) {
        if (option == null) return null;
        QuizAnswerResponse response = new QuizAnswerResponse();
        response.setId(option.getId());
        response.setContent(option.getContent());
        response.setExplanation(showCorrectAnswer ? option.getExplanation() : null);
        response.setResourceId(option.getResource() != null ? option.getResource().getId() : null);
        response.setResource(convertResourceToDTO(option.getResource()));
        response.setIsCorrect(showCorrectAnswer ? option.getIsCorrect() : null);
        return response;
    }

    private ResourceResponse convertResourceToDTO(Resource resource) {
        if (resource == null) return null;
        ResourceResponse response = new ResourceResponse();
        response.setId(resource.getId());
        response.setTitle(resource.getTitle());
        response.setFileUrl(resource.getFileUrl());
        response.setEmbedUrl(resource.getEmbedUrl());
        response.setCloudinaryId(resource.getCloudinaryId());
        response.setDescription(resource.getDescription());
        response.setMimeType(resource.getMimeType());
        response.setFileSize(resource.getFileSize());
        response.setType(resource.getType());
        response.setSource(resource.getSource());
        return response;
    }

    private QuestionInteractionItemResponse convertQuestionItemToDTO(
            QuestionInteractionItem item,
            boolean showCorrectAnswer
    ) {
        if (item == null) return null;
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

    private QuizAnswerResponse convertQuizAnswerToDTO(QuizAnswer quizAnswer, boolean showCorrectAnswer) {
        QuizAnswerResponse response = new QuizAnswerResponse();
        response.setId(quizAnswer.getId());
        response.setContent(quizAnswer.getContent());
        response.setExplanation(showCorrectAnswer ? quizAnswer.getExplanation() : null);
        response.setResourceId(quizAnswer.getResource() != null ? quizAnswer.getResource().getId() : null);
        response.setResource(convertResourceToDTO(quizAnswer.getResource()));

        // LOGIC Báº¢O Máº¬T: Chá»‰ hiá»‡n true/false náº¿u showCorrectAnswer = true
        if (showCorrectAnswer) {
            response.setIsCorrect(quizAnswer.getIsCorrect());
        } else {
            response.setIsCorrect(null); // Tráº£ vá» null Ä‘á»ƒ json khÃ´ng hiá»‡n hoáº·c hiá»‡n null
        }

        return response;
    }

    // Mapper cÅ© dÃ¹ng cho list history (khÃ´ng cáº§n detail questions)
    private QuizAttemptResponse convertQuizAttemptToDTO(QuizAttempt attempt) {
        if (attempt == null) return null;
        return QuizAttemptResponse.builder()
                .id(attempt.getId())
                .startTime(attempt.getStartTime())
                .completedTime(attempt.getCompletedTime())
                .grade(attempt.getGrade())
                .isPassed(attempt.getIsPassed())
                .quizId(attempt.getQuiz() != null ? attempt.getQuiz().getId() : null)
                .studentId(attempt.getStudent() != null ? attempt.getStudent().getId() : null)
                .classContentItemId(attempt.getClassContentItem() != null ? attempt.getClassContentItem().getId() : null)
                .classSectionId(resolveClassSectionId(attempt))
                .totalQuestions(attempt.getTotalQuestions())
                .correctAnswers(attempt.getCorrectAnswers())
                .incorrectAnswers(attempt.getIncorrectAnswers())
                .unansweredQuestions(attempt.getUnansweredQuestions())
                .earnedPoints(attempt.getEarnedPoints())
                .totalPoints(attempt.getTotalPoints())
                .gradingStatus(attempt.getGradingStatus())
                .instructorFeedback(attempt.getInstructorFeedback())
                .remainingTimeSeconds(calculateRemainingTimeSeconds(attempt))
                .quizTitle(attempt.getQuiz() != null ? attempt.getQuiz().getTitle() : null)
                .studentName(attempt.getStudent() != null ? attempt.getStudent().getFullName() : null)
                .studentEmail(attempt.getStudent() != null ? attempt.getStudent().getGmail() : null)
                .classSectionTitle(attempt.getClassContentItem() != null
                        && attempt.getClassContentItem().getClassChapter() != null
                        && attempt.getClassContentItem().getClassChapter().getClassSection() != null
                        ? attempt.getClassContentItem().getClassChapter().getClassSection().getTitle()
                        : null)
                .build();
    }
}
