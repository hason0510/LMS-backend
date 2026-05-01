package com.example.backend.service.impl;

import com.example.backend.constant.*;
import com.example.backend.dto.request.quiz.QuizAttemptAnswerItemRequest;
import com.example.backend.dto.request.quiz.QuizAttemptAnswerRequest;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.quiz.*;
import com.example.backend.entity.*;
import com.example.backend.entity.old.ChapterItem;
import com.example.backend.entity.old.Course;
import com.example.backend.entity.quiz.*;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.repository.*;
import com.example.backend.repository.old.ChapterItemRepository;
import com.example.backend.repository.old.CourseRepository;
import com.example.backend.service.ClassMemberAuthorizationService;
import com.example.backend.service.EnrollmentService;
import com.example.backend.service.QuizAttemptService;
import com.example.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizAttemptServiceImpl implements QuizAttemptService {

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
    private final ChapterItemRepository chapterItemRepository;
    private final ClassContentItemRepository classContentItemRepository;
    private final ClassSectionRepository classSectionRepository;
    private final ProgressRepository progressRepository;
    private final EnrollmentService enrollmentService;
    private final CourseRepository courseRepository;
    private final ClassMemberAuthorizationService classMemberAuthorizationService;


    // =========================================================================
    // MAIN LOGIC
    // =========================================================================

    @Override
    @Transactional
    public QuizAttemptDetailResponse startQuizAttempt(Integer quizId, Integer chapterItemId) {
        User currentUser = userService.getCurrentUser();
        if(!userService.isCurrentUser(currentUser.getId())) {
            throw new UnauthorizedException("Chỉ học sinh đăng ký khóa học mới được truy cập vào nội dung này");
        }

        Optional<ClassContentItem> directClassContentItem = classContentItemRepository.findById(chapterItemId);
        if (directClassContentItem.isPresent()) {
            ClassContentItem classContentItem = directClassContentItem.get();
            if (classContentItem.getItemType() == ContentItemType.QUIZ
                    && classContentItem.getQuiz() != null
                    && classContentItem.getQuiz().getId().equals(quizId)) {
                return startQuizAttemptForClassContentItem(quizId, chapterItemId);
            }
        }

        Optional<ChapterItem> chapterItemOptional = chapterItemRepository.findById(chapterItemId);
        if (chapterItemOptional.isEmpty()) {
            return startQuizAttemptForClassContentItem(quizId, chapterItemId);
        }

        // Validate ChapterItem
        ChapterItem chapterItem = chapterItemOptional.get();

        if (chapterItem.getType() != ItemType.QUIZ || !chapterItem.getRefId().equals(quizId)) {
            Optional<ClassContentItem> classContentItemOptional = classContentItemRepository.findById(chapterItemId);
            if (classContentItemOptional.isPresent()) {
                ClassContentItem classContentItem = classContentItemOptional.get();
                if (classContentItem.getItemType() == ContentItemType.QUIZ
                        && classContentItem.getQuiz() != null
                        && classContentItem.getQuiz().getId().equals(quizId)) {
                    return startQuizAttemptForClassContentItem(quizId, chapterItemId);
                }
            }
            throw new ResourceNotFoundException("Quiz không tồn tại");
        }

        boolean isEnrolled = enrollmentRepository.existsByStudent_IdAndCourse_IdAndApprovalStatus(
                currentUser.getId(), chapterItem.getChapter().getCourse().getId(), EnrollmentStatus.APPROVED);
        if (!isEnrolled) {
            throw new UnauthorizedException("Bạn không có quyền truy cập vào tài nguyên này!");
        }

        // Validate Quiz
        Quiz chosenQuiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz không tồn tại"));

        checkQuizAvailability(chosenQuiz);
        // Check if exists attempt in progress
        Optional<QuizAttempt> inProgressAttempt = quizAttemptRepository.findLatestByChapterItem_IdAndStudent_IdAndStatus(
                chapterItem.getId(), currentUser.getId(), AttemptStatus.IN_PROGRESS);

        // Nếu đã có bài đang làm dở -> Trả về chi tiết bài đó luôn (để FE render)
        if(inProgressAttempt.isPresent()){
            return convertToDetailResponse(inProgressAttempt.get());
        }

        // Check max attempts
        int currentAttempt = quizAttemptRepository.countByChapterItem_IdAndStudent_Id(chapterItem.getId(), currentUser.getId());
        if(chosenQuiz.getMaxAttempts() != null && currentAttempt >= chosenQuiz.getMaxAttempts()){
            throw new BusinessException("Đã vượt quá số lần làm bài!");
        }

        // Create new attempt
        QuizAttempt attempt = QuizAttempt.builder()
                .quiz(chosenQuiz)
                .student(currentUser)
                .chapterItem(chapterItem)
                .attemptNumber(currentAttempt + 1)
                .grade(0)
                .isPassed(false)
                .totalQuestions(0)
                .unansweredQuestions(0)
                .incorrectAnswers(0)
                .correctAnswers(0)
                .startTime(LocalDateTime.now())
                .status(AttemptStatus.IN_PROGRESS)
                .build();

        quizAttemptRepository.save(attempt);

        initializeAttemptContent(attempt, chosenQuiz);

        // Return detail response (bao gồm list câu hỏi vừa tạo)
        return convertToDetailResponse(attempt);
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
            throw new ResourceNotFoundException("Quiz khong ton tai");
        }

        Integer classSectionId = classContentItem.getClassChapter().getClassSection().getId();
        boolean isEnrolled = enrollmentRepository.existsByStudent_IdAndClassSection_IdAndApprovalStatus(
                currentUser.getId(), classSectionId, EnrollmentStatus.APPROVED);
        if (!isEnrolled) {
            throw new UnauthorizedException("Ban khong co quyen truy cap vao tai nguyen nay!");
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
            attemptQuestion.setFileUrl(sourceQuestion.getFileUrl());
            attemptQuestion.setEmbedUrl(sourceQuestion.getEmbedUrl());
            attemptQuestion.setCloudinaryId(sourceQuestion.getCloudinaryId());
            attemptQuestion.setPoints(sourceQuestion.getPoints() != null ? sourceQuestion.getPoints() : 1);

            List<QuizAttemptQuestionOption> options = new ArrayList<>();
            if (sourceQuestion.getAnswers() != null) {
                for (QuizAnswer sourceAnswer : sourceQuestion.getAnswers()) {
                    QuizAttemptQuestionOption option = new QuizAttemptQuestionOption();
                    option.setAttemptQuestion(attemptQuestion);
                    option.setSourceQuizAnswer(sourceAnswer);
                    option.setContent(sourceAnswer.getContent());
                    option.setIsCorrect(sourceAnswer.getIsCorrect());
                    option.setFileUrl(sourceAnswer.getFileUrl());
                    option.setEmbedUrl(sourceAnswer.getEmbedUrl());
                    option.setCloudinaryId(sourceAnswer.getCloudinaryId());
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
            attemptQuestion.setFileUrl(sourceQuestion.getFileUrl());
            attemptQuestion.setEmbedUrl(sourceQuestion.getEmbedUrl());
            attemptQuestion.setCloudinaryId(sourceQuestion.getCloudinaryId());
            attemptQuestion.setPoints(sourceQuestion.getDefaultPoints() != null ? sourceQuestion.getDefaultPoints() : 1);

            List<QuizAttemptQuestionOption> options = new ArrayList<>();
            if (sourceQuestion.getOptions() != null) {
                for (BankQuestionOption sourceOption : sourceQuestion.getOptions()) {
                    QuizAttemptQuestionOption option = new QuizAttemptQuestionOption();
                    option.setAttemptQuestion(attemptQuestion);
                    option.setSourceBankQuestionOption(sourceOption);
                    option.setContent(sourceOption.getContent());
                    option.setIsCorrect(sourceOption.getIsCorrect());
                    option.setFileUrl(sourceOption.getFileUrl());
                    option.setEmbedUrl(sourceOption.getEmbedUrl());
                    option.setCloudinaryId(sourceOption.getCloudinaryId());
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
        targetItem.setFileUrl(sourceItem.getFileUrl());
        targetItem.setEmbedUrl(sourceItem.getEmbedUrl());
        targetItem.setCloudinaryId(sourceItem.getCloudinaryId());
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

        if (question.getType() == QuestionType.MATCHING) {
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
        List<BankQuestion> matchedQuestions = bankQuestionRepository.findSelectableQuestions(
                source.getQuestionBank().getId(),
                source.getDifficultyLevel(),
                source.getTag() != null ? source.getTag().getId() : null
        );

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

        if (source.getSelectionMode() == QuizSourceSelectionMode.MANUAL) {
            List<Integer> selectedIds = parseCsvIds(source.getManualQuestionIds());
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
            selectedQuestions.sort((left, right) -> Integer.compare(selectedIds.indexOf(left.getId()), selectedIds.indexOf(right.getId())));
            if (excludedQuestionIds == null || excludedQuestionIds.isEmpty()) {
                return selectedQuestions;
            }
            return selectedQuestions.stream()
                    .filter(q -> !excludedQuestionIds.contains(q.getId()))
                    .toList();
        }

        throw new BusinessException("Unsupported question bank selection mode");
    }

    private List<Integer> parseCsvIds(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return List.of();
        }
        List<Integer> ids = new ArrayList<>();
        for (String value : csv.split(",")) {
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                ids.add(Integer.valueOf(trimmed));
            }
        }
        return ids;
    }

    @Override
    @Transactional
    public void answerQuestion(Integer attemptId, Integer questionId, QuizAttemptAnswerRequest request) {
        log.info("Answering question {} in attempt {}", questionId, attemptId);

        QuizAttempt attempt = quizAttemptRepository.findById((attemptId))
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found"));

        User currentUser = userService.getCurrentUser();
        if (!attempt.getStudent().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Không có quyền làm bài này");
        }

        if (isAttemptExpired(attempt)) {
            expireAttempt(attempt);
            throw new BusinessException("Đã hết thời gian làm bài!");
        }

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new BusinessException("Bài làm đã kết thúc");
        }

        QuizAttemptAnswer attemptAnswer = quizAttemptAnswerRepository
                .findByAttempt_IdAndAttemptQuestion_Id(attemptId, questionId)
                .or(() -> quizAttemptAnswerRepository.findByAttempt_IdAndQuestion_Id(attemptId, questionId))
                .orElseThrow(() -> new ResourceNotFoundException("Answer attempt not found"));

        QuizAttemptQuestion attemptQuestion = attemptAnswer.getAttemptQuestion();
        QuizQuestion question = attemptAnswer.getQuestion();
        QuestionType questionType = attemptQuestion != null ? attemptQuestion.getType() : question.getType();

        // Xử lý Single Choice
        if (questionType == QuestionType.SINGLE_CHOICE) {
            List<Integer> selectedIds = request.getSelectedAnswerIds() != null ? request.getSelectedAnswerIds() : List.of();
            if (attemptQuestion != null) {
                List<QuizAttemptQuestionOption> selectedOptions = quizAttemptQuestionOptionRepository.findAllById(selectedIds);
                if (selectedOptions.size() != selectedIds.size()) {
                    throw new BusinessException("Một số đáp án không tồn tại");
                }
                for (QuizAttemptQuestionOption opt : selectedOptions) {
                    if (opt.getAttemptQuestion() == null || !opt.getAttemptQuestion().getId().equals(attemptQuestion.getId())) {
                        throw new BusinessException("Đáp án không thuộc về câu hỏi này");
                    }
                }
                attemptAnswer.setSelectedOptions(selectedOptions);
                attemptAnswer.setSelectedAnswers(null);
                attemptAnswer.setTextAnswer(null);
            } else {
                List<QuizAnswer> selectedAnswers = quizAnswerRepository.findAllById(selectedIds);

                if (selectedAnswers.size() != selectedIds.size()) {
                    throw new BusinessException("Một số đáp án không tồn tại");
                }
                for (QuizAnswer ans : selectedAnswers) {
                    if (!ans.getQuizQuestion().getId().equals(questionId)) {
                        throw new BusinessException("Đáp án không thuộc về câu hỏi này");
                    }
                }
                attemptAnswer.setSelectedAnswers(selectedAnswers);
                attemptAnswer.setSelectedOptions(null);
                attemptAnswer.setTextAnswer(null);
            }
        }
        // Xử lý Multiple Choice
        else if (questionType == QuestionType.MULTIPLE_CHOICE) {
            List<Integer> selectedIds = request.getSelectedAnswerIds() != null ? request.getSelectedAnswerIds() : List.of();
            if (attemptQuestion != null) {
                List<QuizAttemptQuestionOption> selectedOptions = quizAttemptQuestionOptionRepository.findAllById(selectedIds);
                if (selectedOptions.size() != selectedIds.size()) {
                    throw new BusinessException("Một số đáp án không tồn tại");
                }
                for (QuizAttemptQuestionOption opt : selectedOptions) {
                    if (opt.getAttemptQuestion() == null || !opt.getAttemptQuestion().getId().equals(attemptQuestion.getId())) {
                        throw new BusinessException("Đáp án không thuộc về câu hỏi này");
                    }
                }
                attemptAnswer.setSelectedOptions(selectedOptions);
                attemptAnswer.setSelectedAnswers(null);
                attemptAnswer.setTextAnswer(null);
            } else {
                List<QuizAnswer> selectedAnswers = quizAnswerRepository.findAllById(selectedIds);

                if (selectedAnswers.size() != selectedIds.size()) {
                    throw new BusinessException("Một số đáp án không tồn tại");
                }
                for (QuizAnswer ans : selectedAnswers) {
                    if (!ans.getQuizQuestion().getId().equals(questionId)) {
                        throw new BusinessException("Đáp án không thuộc về câu hỏi này");
                    }
                }
                attemptAnswer.setSelectedAnswers(selectedAnswers);
                attemptAnswer.setSelectedOptions(null);
                attemptAnswer.setTextAnswer(null);
            }
        }
        // Xử lý Short answer
        else if (questionType == QuestionType.SHORT_ANSWER) {
            String userAnswer = request.getTextAnswer() != null ? request.getTextAnswer().trim() : "";
            attemptAnswer.setTextAnswer(userAnswer);
            attemptAnswer.setSelectedAnswers(null);
            attemptAnswer.setSelectedOptions(null);
        }
        // Xử lý câu hỏi tương tác
        else if (isInteractionQuestionType(questionType)) {
            saveInteractionAnswerItems(attemptAnswer, attemptQuestion, questionType, request);
        }

        attemptAnswer.setCompletedAt(LocalDateTime.now());
        quizAttemptAnswerRepository.save(attemptAnswer);
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

        if (attemptAnswer.getAnswerItems() == null) {
            attemptAnswer.setAnswerItems(new ArrayList<>());
        } else {
            attemptAnswer.getAnswerItems().clear();
        }
        attemptAnswer.getAnswerItems().addAll(incomingAnswerItems);
        attemptAnswer.setSelectedAnswers(null);
        attemptAnswer.setSelectedOptions(null);
        attemptAnswer.setTextAnswer(null);
    }

    private void validateInteractionAnswerItem(
            QuestionType questionType,
            QuizAttemptQuestionItem questionItem,
            QuizAttemptQuestionItem selectedItem,
            QuizAttemptAnswerItemRequest itemRequest
    ) {
        if (questionType == QuestionType.MATCHING) {
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

        User currentUser = userService.getCurrentUser();
        if (!attempt.getStudent().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Không có quyền nộp bài này");
        }
        if (attempt.getCompletedTime() != null) {
            throw new BusinessException("Bạn đã nộp bài rồi!");
        }
        if (isAttemptExpired(attempt)) {
            expireAttempt(attempt);
            return convertQuizAttemptToDTO(attempt);
        }

        updateAttemptStatistics(attempt);
        attempt.setCompletedTime(LocalDateTime.now());
        Integer passingScore = attempt.getQuiz().getMinPassScore();
        boolean isPassed = attempt.getGrade() >= passingScore; // Check pass
        attempt.setIsPassed(isPassed);
        attempt.setStatus(AttemptStatus.COMPLETED);
        quizAttemptRepository.save(attempt);

        if (isPassed) {
            markProgressIfPassed(attempt);
        }
        if (false) {
            User student = attempt.getStudent();
            ChapterItem chapterItem = attempt.getChapterItem();
            Progress progress = progressRepository
                    .findByStudent_IdAndChapterItem_Id(student.getId(), chapterItem.getId())
                    .orElse(Progress.builder()
                            .student(student)
                            .chapterItem(chapterItem)
                            .isCompleted(false) // Mặc định false nếu chưa có bản ghi
                            .build());
            // CHỈ cập nhật và tính toán lại nếu chưa hoàn thành trước đó
            if (Boolean.FALSE.equals(progress.getIsCompleted())) {
                progress.setIsCompleted(true);
                progress.setCompletedAt(LocalDateTime.now());
                progressRepository.save(progress);

                // Cập nhật progress tổng của Enrollment
                if (chapterItem.getChapter() != null && chapterItem.getChapter().getCourse() != null) {
                    Integer courseId = chapterItem.getChapter().getCourse().getId();
                    enrollmentService.recalculateAndSaveProgress(student.getId(), courseId);
                }
            }
        }

        return convertQuizAttemptToDTO(attempt);
    }

    @Override
    public QuizAttemptDetailResponse getCurrentAttempt(Integer chapterItemId) {
        User currentUser = userService.getCurrentUser();
        Optional<QuizAttempt> classContentAttempt = quizAttemptRepository
                .findTopByClassContentItem_IdAndStudent_IdAndStatusOrderByIdDesc(
                        chapterItemId,
                        currentUser.getId(),
                        AttemptStatus.IN_PROGRESS
                );
        if (classContentAttempt.isPresent()) {
            return convertToDetailResponse(classContentAttempt.get());
        }

        Optional<QuizAttempt> legacyAttempt = Optional.empty();
        if (chapterItemRepository.findById(chapterItemId).isPresent()) {
            legacyAttempt = quizAttemptRepository.findLatestByChapterItem_IdAndStudent_IdAndStatus(
                    chapterItemId,
                    currentUser.getId(),
                    AttemptStatus.IN_PROGRESS
            );
        }

        if (legacyAttempt.isPresent()) {
            return convertToDetailResponse(legacyAttempt.get());
        }

        throw new ResourceNotFoundException("Không có bài làm nào đang diễn ra");
    }

    @Override
    public QuizAttemptDetailResponse getCurrentAttemptForClassContentItem(Integer classContentItemId) {
        User currentUser = userService.getCurrentUser();
        QuizAttempt attempt = quizAttemptRepository
                .findTopByClassContentItem_IdAndStudent_IdAndStatusOrderByIdDesc(
                        classContentItemId,
                        currentUser.getId(),
                        AttemptStatus.IN_PROGRESS
                )
                .orElseThrow(() -> new ResourceNotFoundException("Khong co bai lam nao dang dien ra"));

        return convertToDetailResponse(attempt);
    }

    // =========================================================================
    // THỐNG KÊ BÀI LÀM CỦA CÁ NHÂN SINH VIÊN VÀ SINH VIÊN NÓI CHUNG TRONG KHÓA HỌC
    // =========================================================================

    @Override
    public QuizAttemptDetailResponse getAttemptDetail(Integer attemptId) {
        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found"));

        User currentUser = userService.getCurrentUser();
        boolean isStudent = attempt.getStudent().getId().equals(currentUser.getId());
        boolean isTeacher = canManageAttempt(attempt, currentUser);
        boolean isAdmin = currentUser.getRole().getRoleName() == RoleType.ADMIN;

        // Check Access Control (Quyền truy cập)
        if (!isStudent && !isTeacher && !isAdmin) {
            throw new UnauthorizedException("Bạn không có quyền xem bài làm này");
        }

        // Gọi hàm convert chung (nó sẽ tự check quyền xem đáp án bên trong)
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
        int fullCorrectCount = 0;   // Đếm số câu Full điểm
        int incorrectCount = 0;     // Đếm số câu 0 điểm (đã trả lời)

        double totalEarnedScore = 0.0;
        int totalMaxScore = 0;

        for (QuizAttemptAnswer submitAnswer : attemptAnswers) {
            QuizAttemptQuestion attemptQuestion = submitAnswer.getAttemptQuestion();
            QuizQuestion quizQuestion = submitAnswer.getQuestion();

            QuestionType questionType = attemptQuestion != null ? attemptQuestion.getType() : quizQuestion.getType();
            int questionPoints = attemptQuestion != null
                    ? (attemptQuestion.getPoints() != null ? attemptQuestion.getPoints() : 1)
                    : (quizQuestion.getPoints() != null ? quizQuestion.getPoints() : 1);
            totalMaxScore += questionPoints;

            boolean answeredThisQuestion = false;
            boolean isFullScore = false;
            double earnedPoints = 0.0;

            // ===== 1. SINGLE CHOICE =====
            if (questionType == QuestionType.SINGLE_CHOICE) {
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
                                    earnedPoints = questionPoints / 2.0;
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
                                    earnedPoints = questionPoints / 2.0;
                                }
                            }
                        }
                    }
                }
            }
            // ===== 4. INTERACTION QUESTIONS =====
            else if (isInteractionQuestionType(questionType) && attemptQuestion != null) {
                InteractionGrade interactionGrade = gradeInteractionQuestion(submitAnswer, attemptQuestion, questionType, questionPoints);
                answeredThisQuestion = interactionGrade.answered();
                isFullScore = interactionGrade.fullScore();
                earnedPoints = interactionGrade.earnedPoints();
            }

            if (answeredThisQuestion) {
                answeredCount++;
            }

            submitAnswer.setIsCorrect(isFullScore);
            totalEarnedScore += earnedPoints;

            if (earnedPoints == questionPoints) {
                fullCorrectCount++;
            } else if (answeredThisQuestion && earnedPoints == 0.0) {
                incorrectCount++;
            }
        }

        attempt.setCorrectAnswers(fullCorrectCount);
        attempt.setIncorrectAnswers(incorrectCount);
        attempt.setUnansweredQuestions(attempt.getTotalQuestions() != null
                ? attempt.getTotalQuestions() - answeredCount
                : 0);

        if (totalMaxScore > 0) {
            int finalGrade = (int) Math.round((totalEarnedScore / totalMaxScore) * 100);
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
            int questionPoints
    ) {
        List<QuizAttemptAnswerItem> answerItems = submitAnswer.getAnswerItems() != null
                ? submitAnswer.getAnswerItems()
                : quizAttemptAnswerItemRepository.findByAttemptAnswer_IdOrderByIdAsc(submitAnswer.getId());
        boolean answered = answerItems.stream().anyMatch(item -> hasMeaningfulInteractionAnswer(item, questionType));
        if (!answered) {
            return new InteractionGrade(false, false, 0.0);
        }

        double ratio = 0.0;
        if (questionType == QuestionType.MATCHING) {
            ratio = gradeMatchingQuestion(attemptQuestion, answerItems);
        } else if (questionType == QuestionType.DRAG_ORDER) {
            ratio = gradeDragOrderQuestion(attemptQuestion, answerItems);
        } else if (questionType == QuestionType.CLOZE) {
            ratio = gradeClozeQuestion(attemptQuestion, answerItems);
        }

        boolean fullScore = ratio >= 1.0;
        return new InteractionGrade(true, fullScore, fullScore ? questionPoints : questionPoints * ratio);
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
        if (questionType == QuestionType.MATCHING) {
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
        return type == QuestionType.MATCHING
                || type == QuestionType.DRAG_ORDER
                || type == QuestionType.CLOZE;
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

    private record InteractionGrade(boolean answered, boolean fullScore, double earnedPoints) {
    }

    private boolean isAttemptExpired(QuizAttempt attempt) {
        if (attempt.getQuiz().getTimeLimitMinutes() == null) return false;
        LocalDateTime expirationTime = attempt.getStartTime().plusMinutes(attempt.getQuiz().getTimeLimitMinutes());
        return LocalDateTime.now().isAfter(expirationTime);
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
        attempt.setCompletedTime(LocalDateTime.now());
        attempt.setStatus(AttemptStatus.EXPIRED);
        Integer passingScore = attempt.getQuiz().getMinPassScore();
        attempt.setIsPassed(attempt.getGrade() >= passingScore);
        quizAttemptRepository.save(attempt);
        if (Boolean.TRUE.equals(attempt.getIsPassed())) {
            markProgressIfPassed(attempt);
        }
    }

    // LẤY KẾT QUẢ CỦA BẢN THÂN SINH VIÊN THEO 1 QUIZ
    @Override
    public List<QuizAttemptResponse> getStudentAttemptsHistory(Integer chapterItemId) {
        User currentUser = userService.getCurrentUser();
        return quizAttemptRepository.findByChapterItem_IdAndStudent_Id(chapterItemId, currentUser.getId())
                .stream()
                .filter(a -> a.getStatus() == AttemptStatus.COMPLETED || a.getStatus() == AttemptStatus.EXPIRED)
                .map(this::convertQuizAttemptToDTO)
                .toList();
    }


    // LẤY KẾT QUẢ CỦA TẤT CẢ MỌI NGƯỜI THEO 1 QUIZ
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
        User currentUser = userService.getCurrentUser();
        ChapterItem chapterItem = chapterItemRepository.findById(chapterItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter item not found"));
        Integer teacherId = chapterItem.getChapter().getCourse().getTeacher().getId();
        boolean isAdmin = currentUser.getRole().getRoleName() == RoleType.ADMIN;
        boolean isTeacher = teacherId.equals(currentUser.getId());

        if (!isAdmin && !isTeacher) {
            throw new UnauthorizedException("Bạn không có quyền xem lịch sử bài làm");
        }

        Page<QuizAttemptResponse> dtoPage = quizAttemptRepository.findByChapterItem_IdAndStatusIn(
                chapterItemId, List.of(AttemptStatus.COMPLETED, AttemptStatus.EXPIRED), pageable
        ).map(this::convertQuizAttemptToDTO);

        return new PageResponse<>(dtoPage.getNumber() + 1, dtoPage.getTotalPages(), dtoPage.getNumberOfElements(), dtoPage.getContent());
    }

    //LẤY KẾT QUẢ

    @Override
    public PageResponse<QuizAttemptResponse> getAttemptsForTeacherOrAdminByClassContentItem(Integer classContentItemId, Pageable pageable) {
        User currentUser = userService.getCurrentUser();
        ClassContentItem classContentItem = classContentItemRepository.findById(classContentItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Class content item not found"));

        if (!canManageClassSection(classContentItem.getClassChapter().getClassSection(), currentUser)
                && currentUser.getRole().getRoleName() != RoleType.ADMIN) {
            throw new UnauthorizedException("Ban khong co quyen xem lich su bai lam");
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
        User currentUser = userService.getCurrentUser();
        Integer maxGrade = quizAttemptRepository.findMaxGradeByChapterItemAndStudent(chapterItemId, currentUser.getId());
        return maxGrade == null ? 0 : maxGrade;
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
            throw new BusinessException("Chưa đến giờ làm bài");
        }

        if (quiz.getAvailableUntil() != null && now.isAfter(quiz.getAvailableUntil())) {
            throw new BusinessException("Đã hết hạn làm bài");
        }
    }

    // =========================================================================
    // MAPPERS & SECURITY LOGIC (QUAN TRỌNG)
    // =========================================================================

    /**
     * Logic quyết định xem user có được phép nhìn thấy đáp án đúng hay không?
     * @return true: Được xem (GV, Admin, hoặc HS đã nộp bài)
     * @return false: Bị ẩn (HS đang làm bài)
     */
    private boolean shouldShowCorrectAnswers(QuizAttempt attempt) {
        User currentUser = userService.getCurrentUser();

        // 1. Admin luôn xem được
        if (currentUser.getRole().getRoleName() == RoleType.ADMIN) return true;

        // 2. Giáo viên sở hữu khóa học luôn xem được
        if (canManageAttempt(attempt, currentUser)) return true;

        // 3. Học sinh: Chỉ xem được khi đã Nộp bài hoặc Hết giờ
        if (attempt.getStudent().getId().equals(currentUser.getId())) {
            return attempt.getStatus() == AttemptStatus.COMPLETED
                    || attempt.getStatus() == AttemptStatus.EXPIRED;
        }

        return false;
    }

    // =========================================================================
    // GRADEBOOK / BẢNG ĐIỂM
    // =========================================================================

    /**
     * API cho Sinh viên xem bảng điểm cá nhân của mình (Tất cả quiz đã làm)
     */
    @Override
    public List<StudentQuizResultResponse> getMyGradeBook(Integer courseId) { // Thêm tham số courseId
        User currentUser = userService.getCurrentUser();

        boolean isEnrolled = enrollmentRepository.existsByStudent_IdAndCourse_IdAndApprovalStatus(
                currentUser.getId(), courseId, EnrollmentStatus.APPROVED);
        if (!isEnrolled) {
            throw new UnauthorizedException("Bạn không có quyền truy cập vào tài nguyên này!");
        }

        return quizAttemptRepository.findMaxGradesByStudentAndCourse(currentUser.getId(), courseId);
    }
    /**
     * API cho Giáo viên/Admin xem bảng điểm của khóa học
     */
    @Override
    public List<ClassSectionStudentQuizResultResponse> getMyGradeBookForClassSection(Integer classSectionId) {
        User currentUser = userService.getCurrentUser();

        boolean isEnrolled = enrollmentRepository.existsByStudent_IdAndClassSection_IdAndApprovalStatus(
                currentUser.getId(), classSectionId, EnrollmentStatus.APPROVED);
        if (!isEnrolled) {
            throw new UnauthorizedException("Ban khong co quyen truy cap vao tai nguyen nay!");
        }

        return quizAttemptRepository.findMaxGradesByStudentAndClassSection(currentUser.getId(), classSectionId);
    }

    @Override
    public List<CourseQuizResultResponse> getCourseGradeBook(Integer courseId) {
        User currentUser = userService.getCurrentUser();

        // 1. Validate quyền truy cập
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        boolean isAdmin = currentUser.getRole().getRoleName() == RoleType.ADMIN;
        boolean isTeacher = course.getTeacher().getId().equals(currentUser.getId());

        if (!isAdmin && !isTeacher) {
            throw new UnauthorizedException("Bạn không có quyền xem bảng điểm của khóa học này");
        }

        // 2. Gọi Repository lấy dữ liệu tổng hợp
        return quizAttemptRepository.findMaxGradesByCourse(courseId);
    }

    @Override
    public List<ClassSectionQuizGradeResponse> getClassSectionGradeBook(Integer classSectionId) {
        User currentUser = userService.getCurrentUser();
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));

        if (!canManageClassSection(classSection, currentUser) && currentUser.getRole().getRoleName() != RoleType.ADMIN) {
            throw new UnauthorizedException("Ban khong co quyen xem bang diem cua lop hoc nay");
        }

        return quizAttemptRepository.findMaxGradesByClassSection(classSectionId);
    }

    private Quiz resolveQuizFromClassContentItem(ClassContentItem classContentItem) {
        if (classContentItem.getItemType() != ContentItemType.QUIZ) {
            throw new BusinessException("Noi dung nay khong phai quiz");
        }

        Quiz quiz = classContentItem.getQuiz();
        if (quiz == null) {
            throw new ResourceNotFoundException("Quiz chua duoc gan vao class content item");
        }
        return quiz;
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
            return;
        }

        if (attempt.getChapterItem() != null) {
            ChapterItem chapterItem = attempt.getChapterItem();
            Progress progress = progressRepository
                    .findByStudent_IdAndChapterItem_Id(student.getId(), chapterItem.getId())
                    .orElse(Progress.builder()
                            .student(student)
                            .chapterItem(chapterItem)
                            .isCompleted(false)
                            .build());

            if (!Boolean.TRUE.equals(progress.getIsCompleted())) {
                progress.setIsCompleted(true);
                progress.setCompletedAt(LocalDateTime.now());
                progressRepository.save(progress);
                if (chapterItem.getChapter() != null && chapterItem.getChapter().getCourse() != null) {
                    enrollmentService.recalculateAndSaveProgress(
                            student.getId(),
                            chapterItem.getChapter().getCourse().getId()
                    );
                }
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

    private boolean canManageAttempt(QuizAttempt attempt, User currentUser) {
        if (currentUser.getRole().getRoleName() == RoleType.ADMIN) {
            return true;
        }

        if (attempt.getChapterItem() != null
                && attempt.getChapterItem().getChapter() != null
                && attempt.getChapterItem().getChapter().getCourse() != null
                && attempt.getChapterItem().getChapter().getCourse().getTeacher() != null) {
            return attempt.getChapterItem().getChapter().getCourse().getTeacher().getId().equals(currentUser.getId());
        }

        if (attempt.getClassContentItem() != null
                && attempt.getClassContentItem().getClassChapter() != null
                && attempt.getClassContentItem().getClassChapter().getClassSection() != null) {
            return canManageClassSection(attempt.getClassContentItem().getClassChapter().getClassSection(), currentUser);
        }

        return false;
    }

    private boolean canManageClassSection(ClassSection classSection, User currentUser) {
        return classMemberAuthorizationService.isTeacher(classSection, currentUser);
    }

    private QuizAttemptDetailResponse convertToDetailResponse(QuizAttempt attempt) {
        QuizAttemptDetailResponse response = new QuizAttemptDetailResponse();

        // Map basic fields
        response.setId(attempt.getId());
        response.setQuizId(attempt.getQuiz().getId());
        response.setStudentId(attempt.getStudent().getId());
        response.setChapterItemId(attempt.getChapterItem() != null ? attempt.getChapterItem().getId() : null);
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
        
        // Calculate remaining time (in seconds)
        response.setRemainingTimeSeconds(calculateRemainingTimeSeconds(attempt));

        // QUAN TRỌNG: Kiểm tra quyền để ẩn/hiện đáp án
        boolean showCorrectAnswer = shouldShowCorrectAnswers(attempt);

        // Fetch answers và map với cờ bảo mật
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

        QuizAttemptQuestion attemptQuestion = entity.getAttemptQuestion();
        QuizQuestion question = entity.getQuestion();
        QuestionType questionType = attemptQuestion != null ? attemptQuestion.getType() : (question != null ? question.getType() : null);

        // Truyền cờ showCorrectAnswer xuống Question để map
        response.setQuizQuestion(attemptQuestion != null
                ? convertAttemptQuestionToDTO(attemptQuestion, showCorrectAnswer)
                : convertQuizQuestionToDTO(question, showCorrectAnswer));

        // Logic ẩn/hiện kết quả đúng sai của chính câu trả lời này
        if (showCorrectAnswer) {
            response.setIsCorrect(entity.getIsCorrect()); // Hiện khi đã nộp
        } else {
            response.setIsCorrect(null); // Ẩn khi đang làm
        }

        if (isInteractionQuestionType(questionType)) {
            response.setAnswerItems(convertAttemptAnswerItems(entity, showCorrectAnswer));
            response.setSelectedAnswers(null);
            response.setTextAnswer(null);
        } else if (questionType == QuestionType.SHORT_ANSWER) {
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
                                // Map selected answers (vẫn phải truyền cờ để ẩn isCorrect bên trong nó)
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
                .map(item -> new QuizAttemptAnswerItemResponse(
                        item.getId(),
                        item.getAttemptQuestionItem() != null ? item.getAttemptQuestionItem().getId() : null,
                        item.getSelectedItem() != null ? item.getSelectedItem().getId() : null,
                        item.getAnswerText(),
                        item.getSubmittedOrderIndex(),
                        item.getBlankIndex(),
                        showCorrectAnswer ? item.getIsCorrect() : null
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
        response.setContent(question.getContent());
        response.setType(question.getType());
        response.setFileUrl(question.getFileUrl());
        response.setEmbedUrl(question.getEmbedUrl());
        response.setCloudinaryId(question.getCloudinaryId());
        response.setPoints(question.getPoints());
        response.setAnswers(question.getOptions() != null
                ? question.getOptions().stream().map(opt -> this.convertAttemptOptionToDTO(opt, showCorrectAnswer)).toList()
                : List.of());
        response.setItems(getAttemptQuestionItems(question).stream()
                .map(item -> this.convertAttemptItemToDTO(item, showCorrectAnswer))
                .toList());
        return response;
    }

    private QuestionInteractionItemResponse convertAttemptItemToDTO(
            QuizAttemptQuestionItem item,
            boolean showCorrectAnswer
    ) {
        if (item == null) return null;
        return new QuestionInteractionItemResponse(
                item.getId(),
                item.getContent(),
                item.getItemKey(),
                item.getRole(),
                showCorrectAnswer ? item.getCorrectMatchKey() : null,
                showCorrectAnswer ? item.getCorrectOrderIndex() : null,
                item.getBlankIndex(),
                showCorrectAnswer ? parseAcceptedAnswers(item.getAcceptedAnswers()) : null,
                item.getFileUrl(),
                item.getEmbedUrl(),
                item.getCloudinaryId(),
                item.getOrderIndex()
        );
    }

    private QuizAnswerResponse convertAttemptOptionToDTO(QuizAttemptQuestionOption option, boolean showCorrectAnswer) {
        if (option == null) return null;
        QuizAnswerResponse response = new QuizAnswerResponse();
        response.setId(option.getId());
        response.setContent(option.getContent());
        response.setFileUrl(option.getFileUrl());
        response.setEmbedUrl(option.getEmbedUrl());
        response.setCloudinaryId(option.getCloudinaryId());
        response.setIsCorrect(showCorrectAnswer ? option.getIsCorrect() : null);
        return response;
    }

    private QuizQuestionResponse convertQuizQuestionToDTO(QuizQuestion question, boolean showCorrectAnswer) {
        if (question == null) return null;
        QuizQuestionResponse response = new QuizQuestionResponse();
        response.setId(question.getId());
        response.setSourceBankQuestionId(question.getSourceBankQuestion() != null ? question.getSourceBankQuestion().getId() : null);
        response.setContent(question.getContent());
        response.setType(question.getType());
        response.setFileUrl(question.getFileUrl());
        response.setEmbedUrl(question.getEmbedUrl());
        response.setCloudinaryId(question.getCloudinaryId());
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

    private QuestionInteractionItemResponse convertQuestionItemToDTO(
            QuestionInteractionItem item,
            boolean showCorrectAnswer
    ) {
        if (item == null) return null;
        return new QuestionInteractionItemResponse(
                item.getId(),
                item.getContent(),
                item.getItemKey(),
                item.getRole(),
                showCorrectAnswer ? item.getCorrectMatchKey() : null,
                showCorrectAnswer ? item.getCorrectOrderIndex() : null,
                item.getBlankIndex(),
                showCorrectAnswer ? parseAcceptedAnswers(item.getAcceptedAnswers()) : null,
                item.getFileUrl(),
                item.getEmbedUrl(),
                item.getCloudinaryId(),
                item.getOrderIndex()
        );
    }

    private QuizAnswerResponse convertQuizAnswerToDTO(QuizAnswer quizAnswer, boolean showCorrectAnswer) {
        QuizAnswerResponse response = new QuizAnswerResponse();
        response.setId(quizAnswer.getId());
        response.setContent(quizAnswer.getContent());
        response.setFileUrl(quizAnswer.getFileUrl());
        response.setEmbedUrl(quizAnswer.getEmbedUrl());
        response.setCloudinaryId(quizAnswer.getCloudinaryId());

        // LOGIC BẢO MẬT: Chỉ hiện true/false nếu showCorrectAnswer = true
        if (showCorrectAnswer) {
            response.setIsCorrect(quizAnswer.getIsCorrect());
        } else {
            response.setIsCorrect(null); // Trả về null để json không hiện hoặc hiện null
        }

        return response;
    }

    // Mapper cũ dùng cho list history (không cần detail questions)
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
                .chapterItemId(attempt.getChapterItem() != null ? attempt.getChapterItem().getId() : null)
                .classContentItemId(attempt.getClassContentItem() != null ? attempt.getClassContentItem().getId() : null)
                .classSectionId(resolveClassSectionId(attempt))
                .totalQuestions(attempt.getTotalQuestions())
                .correctAnswers(attempt.getCorrectAnswers())
                .incorrectAnswers(attempt.getIncorrectAnswers())
                .unansweredQuestions(attempt.getUnansweredQuestions())
                .remainingTimeSeconds(calculateRemainingTimeSeconds(attempt))
                .build();
    }
}
