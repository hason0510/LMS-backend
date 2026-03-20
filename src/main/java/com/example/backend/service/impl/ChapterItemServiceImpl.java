package com.example.backend.service.impl;

import com.example.backend.constant.EnrollmentStatus;
import com.example.backend.constant.ItemType;
import com.example.backend.dto.request.LessonRequest;
import com.example.backend.dto.request.quiz.QuizRequest;
import com.example.backend.dto.response.chapter.ChapterItemResponse;
import com.example.backend.dto.response.LessonResponse;
import com.example.backend.dto.response.quiz.QuizResponse;
import com.example.backend.entity.*;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.repository.*;
import com.example.backend.service.ChapterItemService;
import com.example.backend.service.LessonService;
import com.example.backend.service.QuizService;
import com.example.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChapterItemServiceImpl implements ChapterItemService {

    private final ChapterItemRepository chapterItemRepository;
    private final LessonRepository lessonRepository;
    private final LessonService lessonService;
    private final QuizRepository quizRepository;
    private final ChapterRepository chapterRepository;
    private final QuizService quizService;
    private final ProgressRepository progressRepository;
    private final UserService userService;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    @Override
    public void updateOrder(Integer chapterId, List<Integer> orderedItemIds) {
        // 1. Lấy tất cả item hiện có trong DB thuộc chapter này
        List<ChapterItem> items = chapterItemRepository.findByChapter_Id(chapterId);

        // 2. Tạo Map để truy xuất nhanh item theo ID
        // Key: ID, Value: ChapterItem Entity
        Map<Integer, ChapterItem> itemMap = items.stream()
                .collect(Collectors.toMap(ChapterItem::getId, Function.identity()));

        // 3. Duyệt qua danh sách ID mới từ Frontend gửi về
        for (int i = 0; i < orderedItemIds.size(); i++) {
            Integer itemId = orderedItemIds.get(i);
            ChapterItem item = itemMap.get(itemId);

            if (item != null) {
                // Cập nhật orderIndex = vị trí trong mảng + 1
                item.setOrderIndex(i + 1);
            }
        }
        chapterItemRepository.saveAll(items);
    }

    @Override
    public List<ChapterItemResponse> getItemsByChapterForStudent(Integer chapterId) {
        // 1. Lấy User hiện tại
        User currentUser = userService.getCurrentUser();

        Chapter chapter = chapterRepository.findById(chapterId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương!"));

        if(!enrollmentRepository.existsByStudent_IdAndCourse_IdAndApprovalStatus(
                currentUser.getId(), chapter.getCourse().getId(), EnrollmentStatus.APPROVED)){
            throw new UnauthorizedException("Bạn không nằm trong khóa học này!");
        }
        // 2. Lấy danh sách ID bài đã học xong (Query mới thêm)
        List<Integer> completedIds = progressRepository.findCompletedItemIdsByUserAndChapter(
                currentUser.getId(),
                chapterId
        );
        // 3. Lấy danh sách Items từ DB (Logic cũ)
        List<ChapterItem> items = chapterItemRepository.findByChapter_IdOrderByOrderIndexAsc(chapterId);
        if (items.isEmpty()) return new ArrayList<>();

        // 4. Batch query để lấy chi tiết Lesson/Quiz (Logic cũ)
        List<Integer> lessonIds = items.stream().filter(i -> i.getType() == ItemType.LESSON).map(ChapterItem::getRefId).toList();
        List<Integer> quizIds = items.stream().filter(i -> i.getType() == ItemType.QUIZ).map(ChapterItem::getRefId).toList();

        Map<Integer, Lesson> lessonMap = lessonRepository.findAllById(lessonIds).stream()
                .collect(Collectors.toMap(Lesson::getId, Function.identity()));

        Map<Integer, Quiz> quizMap = quizRepository.findAllById(quizIds).stream()
                .collect(Collectors.toMap(Quiz::getId, Function.identity()));
        // 5. Map sang DTO và set isCompleted
        return items.stream().map(ci -> {
            Object detail = null;
            if (ci.getType() == ItemType.LESSON) {
                Lesson lesson = lessonMap.get(ci.getRefId());
                if (lesson != null) detail = lessonService.convertEntityToDTO(lesson);
            }
            else if (ci.getType() == ItemType.QUIZ) {
                Quiz quiz = quizMap.get(ci.getRefId());
                if (quiz != null) detail = quizService.convertQuizToDTO(quiz);
            }
            // --- ĐOẠN KHÁC BIỆT ---
            boolean isCompleted = completedIds.contains(ci.getId());
            return buildResponseForStudent(ci, detail, isCompleted);
        }).toList();
    }

    private ChapterItemResponse buildResponseForStudent(ChapterItem ci, Object itemDetail, Boolean isCompleted) {
        ChapterItemResponse response = new ChapterItemResponse();
        response.setId(ci.getId());
        response.setType(ci.getType());
        response.setOrderIndex(ci.getOrderIndex());
        response.setItem(itemDetail);
        response.setCompleted(isCompleted); // Set trạng thái

        if (ci.getChapter() != null) {
            response.setChapterId(ci.getChapter().getId());
        }
        return response;
    }

    @Override
    public List<ChapterItemResponse> getItemsByChapter(Integer chapterId) {
        // Lấy danh sách Item đã sort
        List<ChapterItem> items = chapterItemRepository.findByChapter_IdOrderByOrderIndexAsc(chapterId);
        if (items.isEmpty()) return new ArrayList<>();

        // Tách ID để query batch (tránh lỗi N+1)
        List<Integer> lessonIds = items.stream()
                .filter(i -> i.getType() == ItemType.LESSON)
                .map(ChapterItem::getRefId).toList();

        List<Integer> quizIds = items.stream()
                .filter(i -> i.getType() == ItemType.QUIZ)
                .map(ChapterItem::getRefId).toList();

        // Lấy data Lesson và Quiz từ DB
        Map<Integer, Lesson> lessonMap = lessonRepository.findAllById(lessonIds).stream()
                .collect(Collectors.toMap(Lesson::getId, Function.identity()));

        Map<Integer, Quiz> quizMap = quizRepository.findAllById(quizIds).stream()
                .collect(Collectors.toMap(Quiz::getId, Function.identity()));

        // Map sang DTO Response
        return items.stream().map(ci -> {
            Object detail = null;
            if (ci.getType() == ItemType.LESSON) {
                Lesson lesson = lessonMap.get(ci.getRefId());
                if (lesson != null) {
                    detail = lessonService.convertEntityToDTO(lesson);
                }
            }
            else if (ci.getType() == ItemType.QUIZ) {
                    Quiz quiz = quizMap.get(ci.getRefId());
                    if (quiz != null) {
                        detail = quizService.convertQuizToDTO(quiz);
                    }
                }

            return buildResponse(ci, detail);
        }).toList();
    }

    @Transactional
    @Override
    public ChapterItemResponse createLessonInChapter(
            Integer chapterId,
            LessonRequest request
    ) {
        // 1. Check chapter
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found"));

        // 2. Tạo lesson mới
        Lesson lesson = new Lesson();
        lesson.setTitle(request.getTitle());
        lesson.setContent(request.getContent());
        lessonRepository.save(lesson);

        // 3. Tạo chapter item
        ChapterItem ci = createChapterItem(
                chapter,
                ItemType.LESSON,
                lesson.getId()
        );

        // 4. Build response
        return buildResponse(
                ci,
                lessonService.convertEntityToDTO(lesson)
        );
    }

    @Transactional
    @Override
    public ChapterItemResponse createQuizInChapter(
            Integer chapterId,
            QuizRequest request
    ) {
        // 1. Check chapter
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found"));
        // 2. Tạo quiz mới
        Quiz quiz = new Quiz();
        quiz.setTitle(request.getTitle());
        quiz.setDescription(request.getDescription());
        quiz.setMinPassScore(request.getMinPassScore());
        quiz.setTimeLimitMinutes(request.getTimeLimitMinutes());
        quiz.setMaxAttempts(request.getMaxAttempts());
        quizRepository.save(quiz);
        
        // 3. Tạo questions và answers nếu có
        if (request.getQuestions() != null && !request.getQuestions().isEmpty()) {
            for (var questionReq : request.getQuestions()) {
                System.out.println("Creating question: " + questionReq.getContent() + " with points: " + questionReq.getPoints());
                QuizQuestion question = new QuizQuestion();
                question.setContent(questionReq.getContent());
                question.setType(questionReq.getType());
                question.setPoints(questionReq.getPoints());
                question.setQuiz(quiz);
                
                // Tạo answers nếu có
                if (questionReq.getAnswers() != null && !questionReq.getAnswers().isEmpty()) {
                    var answers = questionReq.getAnswers().stream().map(answerReq -> {
                        QuizAnswer answer = new QuizAnswer();
                        answer.setContent(answerReq.getContent());
                        answer.setIsCorrect(answerReq.getIsCorrect());
                        answer.setQuizQuestion(question);
                        return answer;
                    }).toList();
                    question.setAnswers(answers);
                }
                
                // Lưu question với answers (cascade)
                quizService.createQuestionForQuiz(quiz.getId(), question);
            }
        }
        
        // 4. Tạo chapter item
        ChapterItem ci = createChapterItem(
                chapter,
                ItemType.QUIZ,
                quiz.getId()
        );
        // 5. Build response
        return buildResponse(
                ci,
                quizService.convertQuizToDTO(quiz)
        );
    }

    @Transactional
    @Override
    public void deleteChapterItem(Integer id){
        ChapterItem chapterItem = chapterItemRepository.findById(id).orElseThrow(()
                -> new ResourceNotFoundException("Lesson not found"));
        if(chapterItem.getType() == ItemType.LESSON){
            lessonService.deleteLesson(chapterItem.getRefId());
        }
        if(chapterItem.getType() == ItemType.QUIZ){
            quizService.deleteQuiz(chapterItem.getRefId());
        }
        chapterItemRepository.delete(chapterItem);
    }

    @Transactional
    @Override
    public ChapterItemResponse addLessonToChapter(Integer chapterId, Integer lessonId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Chapter not found"));
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));
        ChapterItem ci = createChapterItem(chapter, ItemType.LESSON, lessonId);
        LessonResponse lessonResponse = lessonService.convertEntityToDTO(lesson);
        return buildResponse(ci, lessonResponse);
    }

    @Transactional
    @Override
    public ChapterItemResponse addQuizToChapter(Integer chapterId, Integer quizId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Chapter not found"));
        Quiz quiz = quizRepository.findById(quizId).orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));
        ChapterItem ci = createChapterItem(chapter, ItemType.QUIZ, quizId);
        QuizResponse quizResponse = quizService.convertQuizToDTO(quiz);
        return buildResponse(ci, quizResponse);
    }

    private ChapterItem createChapterItem(Chapter chapter, ItemType type, Integer refId) {
        ChapterItem ci = new ChapterItem();
        ci.setChapter(chapter);
        ci.setType(type);
        ci.setRefId(refId);
        Integer maxOrder = chapterItemRepository.findMaxOrderIndexByChapter(chapter.getId());
        ci.setOrderIndex(maxOrder == null ? 1 : maxOrder + 1);
        return chapterItemRepository.save(ci);
    }

    private ChapterItemResponse buildResponse(ChapterItem ci, Object itemDetail) {
        ChapterItemResponse response = new ChapterItemResponse();
        response.setId(ci.getId());
        response.setType(ci.getType());
        response.setOrderIndex(ci.getOrderIndex());
        response.setItem(itemDetail);
        if (ci.getChapter() != null) {
            response.setChapterId(ci.getChapter().getId());
        }
        return response;
    }
}
