package com.example.backend.service.impl;

import com.example.backend.constant.*;
import com.example.backend.dto.request.quiz.QuizAttemptAnswerRequest;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.quiz.*;
import com.example.backend.entity.*;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.repository.*;
import com.example.backend.service.EnrollmentService;
import com.example.backend.service.QuizAttemptService;
import com.example.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

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
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ChapterItemRepository chapterItemRepository;
    private final ClassContentItemRepository classContentItemRepository;
    private final ClassSectionRepository classSectionRepository;
    private final ProgressRepository progressRepository;
    private final EnrollmentService enrollmentService;
    private final CourseRepository courseRepository;


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

        // Validate ChapterItem
        ChapterItem chapterItem = chapterItemRepository.findById(chapterItemId)
                .orElseThrow(() -> new BusinessException("Quiz chưa được thêm vào chương tương ứng"));

        if (chapterItem.getType() != ItemType.QUIZ || !chapterItem.getRefId().equals(quizId)) {
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
                .totalQuestions(chosenQuiz.getQuestions().size())
                .unansweredQuestions(chosenQuiz.getQuestions().size())
                .incorrectAnswers(0)
                .correctAnswers(0)
                .startTime(LocalDateTime.now())
                .status(AttemptStatus.IN_PROGRESS)
                .build();

        quizAttemptRepository.save(attempt);

        // Init empty answers
        for (QuizQuestion question : chosenQuiz.getQuestions()) {
            QuizAttemptAnswer attemptAnswer = new QuizAttemptAnswer();
            attemptAnswer.setAttempt(attempt);
            attemptAnswer.setQuestion(question);
            quizAttemptAnswerRepository.save(attemptAnswer);
        }

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
                .totalQuestions(chosenQuiz.getQuestions().size())
                .unansweredQuestions(chosenQuiz.getQuestions().size())
                .incorrectAnswers(0)
                .correctAnswers(0)
                .startTime(LocalDateTime.now())
                .status(AttemptStatus.IN_PROGRESS)
                .build();
        quizAttemptRepository.save(attempt);

        for (QuizQuestion question : chosenQuiz.getQuestions()) {
            QuizAttemptAnswer attemptAnswer = new QuizAttemptAnswer();
            attemptAnswer.setAttempt(attempt);
            attemptAnswer.setQuestion(question);
            quizAttemptAnswerRepository.save(attemptAnswer);
        }

        return convertToDetailResponse(attempt);
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
                .findByAttempt_IdAndQuestion_Id(attemptId, questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Answer attempt not found"));

        QuizQuestion question = attemptAnswer.getQuestion();

        // Xử lý Single Choice
        if (question.getType() == QuestionType.SINGLE_CHOICE) {
            List<QuizAnswer> selectedAnswers = quizAnswerRepository.findAllById(request.getSelectedAnswerIds());

            if (selectedAnswers.size() != request.getSelectedAnswerIds().size()) {
                throw new BusinessException("Một số đáp án không tồn tại");
            }
            for (QuizAnswer ans : selectedAnswers) {
                if (!ans.getQuizQuestion().getId().equals(questionId)) {
                    throw new BusinessException("Đáp án không thuộc về câu hỏi này");
                }
            }
            attemptAnswer.setSelectedAnswers(selectedAnswers);
            attemptAnswer.setTextAnswer(null);
        }
        // Xử lý Multiple Choice
        else if (question.getType() == QuestionType.MULTIPLE_CHOICE) {
            List<QuizAnswer> selectedAnswers = quizAnswerRepository.findAllById(request.getSelectedAnswerIds());

            if (selectedAnswers.size() != request.getSelectedAnswerIds().size()) {
                throw new BusinessException("Một số đáp án không tồn tại");
            }
            for (QuizAnswer ans : selectedAnswers) {
                if (!ans.getQuizQuestion().getId().equals(questionId)) {
                    throw new BusinessException("Đáp án không thuộc về câu hỏi này");
                }
            }
            attemptAnswer.setSelectedAnswers(selectedAnswers);
            attemptAnswer.setTextAnswer(null);
        }
        // Xử lý Essay
        else if (question.getType() == QuestionType.ESSAY) {
            String userAnswer = request.getTextAnswer() != null ? request.getTextAnswer().trim() : "";
            attemptAnswer.setTextAnswer(userAnswer);
            attemptAnswer.setSelectedAnswers(null);
        }

        attemptAnswer.setCompletedAt(LocalDateTime.now());
        quizAttemptAnswerRepository.save(attemptAnswer);
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
            StudentChapterItemProgress progress = progressRepository
                    .findByStudent_IdAndChapterItem_Id(student.getId(), chapterItem.getId())
                    .orElse(StudentChapterItemProgress.builder()
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
        QuizAttempt attempt = quizAttemptRepository
                .findLatestByChapterItem_IdAndStudent_IdAndStatus(
                        chapterItemId, currentUser.getId(), AttemptStatus.IN_PROGRESS
                )
                .orElseThrow(() -> new ResourceNotFoundException("Không có bài làm nào đang diễn ra"));

        return convertToDetailResponse(attempt);
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

            if (question.getType() == QuestionType.ESSAY) {
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

        int answered = 0;
        int correctCount = 0;   // Đếm số câu Full điểm
        int incorrectCount = 0; // Đếm số câu 0 điểm hoặc điểm thành phần

        double totalEarnedScore = 0.0;
        int totalMaxScore = 0;

        for (QuizAttemptAnswer submitAnswer : attemptAnswers) {
            QuizQuestion question = submitAnswer.getQuestion();
            int questionPoints = (question.getPoints() != null) ? question.getPoints() : 1;
            totalMaxScore += questionPoints;

            boolean isFullScore = false;
            double earnedPoints = 0.0;

            // ===== 1. SINGLE CHOICE =====
            if (question.getType() == QuestionType.SINGLE_CHOICE) {
                List<QuizAnswer> selectedAnswers = submitAnswer.getSelectedAnswers();

                if (selectedAnswers != null && !selectedAnswers.isEmpty()) {
                    answered++;

                    // Lấy đáp án đúng của câu hỏi
                    QuizAnswer correctAnswer = question.getAnswers().stream()
                            .filter(a -> Boolean.TRUE.equals(a.getIsCorrect()))
                            .findFirst()
                            .orElse(null);

                    if (correctAnswer != null && selectedAnswers.get(0).getId().equals(correctAnswer.getId())) {
                        earnedPoints = questionPoints;
                        isFullScore = true;
                    } else {
                        earnedPoints = 0;
                    }
                }
            }
            // ===== 2. ESSAY (Giữ nguyên) =====
            else if (question.getType() == QuestionType.ESSAY) {
                String userAnswer = submitAnswer.getTextAnswer();
                if (userAnswer != null && !userAnswer.trim().isEmpty()) {
                    answered++;
                    if (!question.getAnswers().isEmpty()) {
                        QuizAnswer correctAnswer = question.getAnswers().get(0);
                        if (correctAnswer.getContent().trim().equalsIgnoreCase(userAnswer.trim())) {
                            earnedPoints = questionPoints;
                            isFullScore = true;
                        }
                    }
                }
            }
            // ===== 3. MULTIPLE CHOICE (Sửa Logic) =====
            else if (question.getType() == QuestionType.MULTIPLE_CHOICE) {
                List<QuizAnswer> selectedAnswers = submitAnswer.getSelectedAnswers();

                if (selectedAnswers != null && !selectedAnswers.isEmpty()) {
                    answered++;

                    // 1. Lấy tập ID đáp án ĐÚNG của hệ thống
                    var systemCorrectIds = question.getAnswers().stream()
                            .filter(a -> Boolean.TRUE.equals(a.getIsCorrect()))
                            .map(QuizAnswer::getId)
                            .collect(java.util.stream.Collectors.toSet());

                    int totalCorrectOptions = systemCorrectIds.size(); // Tổng số đáp án đúng có thể chọn

                    // 2. Lấy tập ID đáp án USER chọn
                    var userSelectedIds = selectedAnswers.stream()
                            .map(QuizAnswer::getId)
                            .collect(java.util.stream.Collectors.toSet());

                    // 3. Tính toán
                    // Số lượng user chọn ĐÚNG
                    long userRightCount = userSelectedIds.stream()
                            .filter(systemCorrectIds::contains)
                            .count();

                    // Số lượng user chọn SAI (Chọn thừa)
                    long userWrongCount = userSelectedIds.size() - userRightCount;

                    // 4. Logic Triệt Tiêu: Đúng trừ Sai
                    long netCorrectCount = userRightCount - userWrongCount;

                    // Chỉ tính điểm nếu số câu đúng "ròng" > 0
                    if (netCorrectCount > 0) {
                        double ratio = (double) netCorrectCount / totalCorrectOptions;

                        // Case 1: Đúng tuyệt đối (Chọn đủ ý đúng, ko chọn sai ý nào)
                        // ratio == 1.0 nghĩa là netCorrectCount == totalCorrectOptions
                        if (ratio >= 1.0) {
                            earnedPoints = questionPoints;
                            isFullScore = true;
                        }
                        // Case 2: Đúng được >= 50% (sau khi đã bị trừ câu sai)
                        else if (ratio >= 0.5) {
                            earnedPoints = questionPoints / 2.0;
                        }
                        // Case 3: Còn lại (ví dụ đúng 1/3) thì coi như chưa đạt -> 0 điểm
                        else {
                            earnedPoints = 0;
                        }
                    } else {
                        // Chọn sai nhiều hơn hoặc bằng chọn đúng -> 0 điểm
                        earnedPoints = 0;
                    }
                }
            }
            // Set kết quả vào DB
            submitAnswer.setIsCorrect(isFullScore);
            totalEarnedScore += earnedPoints;

            if (earnedPoints == questionPoints) {
                correctCount++;
            } else if (earnedPoints == 0 && answered > 0) {
                incorrectCount++;
            }

        }

        // Tổng kết Attempt
        attempt.setCorrectAnswers(correctCount);
        attempt.setIncorrectAnswers(incorrectCount);
        attempt.setUnansweredQuestions(attempt.getTotalQuestions() - answered);

        if (totalMaxScore > 0) {
            int finalGrade = (int) Math.round((totalEarnedScore / totalMaxScore) * 100);
            attempt.setGrade(finalGrade);
        } else {
            attempt.setGrade(0);
        }

        quizAttemptRepository.save(attempt);
        quizAttemptAnswerRepository.saveAll(attemptAnswers);
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

        Quiz quiz = classContentItem.getOverrideQuiz();
        if (quiz == null && classContentItem.getContentItemTemplate() != null) {
            quiz = classContentItem.getContentItemTemplate().getQuiz();
        }
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
            StudentChapterItemProgress progress = progressRepository
                    .findByStudent_IdAndClassContentItem_Id(student.getId(), classContentItem.getId())
                    .orElse(StudentChapterItemProgress.builder()
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
            StudentChapterItemProgress progress = progressRepository
                    .findByStudent_IdAndChapterItem_Id(student.getId(), chapterItem.getId())
                    .orElse(StudentChapterItemProgress.builder()
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
        boolean isTeacher = classSection.getTeacher() != null
                && classSection.getTeacher().getId().equals(currentUser.getId());
        boolean isManager = classSection.getManager() != null
                && classSection.getManager().getId().equals(currentUser.getId());
        return isTeacher || isManager;
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

        // Truyền cờ showCorrectAnswer xuống Question để map
        response.setQuizQuestion(convertQuizQuestionToDTO(entity.getQuestion(), showCorrectAnswer));

        // Logic ẩn/hiện kết quả đúng sai của chính câu trả lời này
        if (showCorrectAnswer) {
            response.setIsCorrect(entity.getIsCorrect()); // Hiện khi đã nộp
        } else {
            response.setIsCorrect(null); // Ẩn khi đang làm
        }

        if (entity.getQuestion().getType() == QuestionType.ESSAY) {
            response.setTextAnswer(entity.getTextAnswer());
            response.setSelectedAnswers(null);
        } else {
            response.setSelectedAnswers(
                    entity.getSelectedAnswers() == null
                            ? List.of()
                            : entity.getSelectedAnswers().stream()
                            // Map selected answers (vẫn phải truyền cờ để ẩn isCorrect bên trong nó)
                            .map(ans -> this.convertQuizAnswerToDTO(ans, showCorrectAnswer))
                            .toList()
            );
            response.setTextAnswer(null);
        }
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
        return response;
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
