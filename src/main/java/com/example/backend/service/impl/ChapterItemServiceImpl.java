package com.example.backend.service.impl;

import com.example.backend.constant.EnrollmentStatus;
import com.example.backend.constant.ItemType;
import com.example.backend.dto.request.LessonRequest;
import com.example.backend.dto.request.quiz.QuizRequest;
import com.example.backend.dto.response.LessonResponse;
import com.example.backend.dto.response.chapter.ChapterItemResponse;
import com.example.backend.dto.response.quiz.QuizResponse;
import com.example.backend.entity.Chapter;
import com.example.backend.entity.ChapterItem;
import com.example.backend.entity.Lesson;
import com.example.backend.entity.Quiz;
import com.example.backend.entity.User;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.repository.ChapterItemRepository;
import com.example.backend.repository.ChapterRepository;
import com.example.backend.repository.EnrollmentRepository;
import com.example.backend.repository.LessonRepository;
import com.example.backend.repository.ProgressRepository;
import com.example.backend.repository.QuizRepository;
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
        List<ChapterItem> items = chapterItemRepository.findByChapter_Id(chapterId);
        Map<Integer, ChapterItem> itemMap = items.stream()
                .collect(Collectors.toMap(ChapterItem::getId, Function.identity()));

        for (int i = 0; i < orderedItemIds.size(); i++) {
            Integer itemId = orderedItemIds.get(i);
            ChapterItem item = itemMap.get(itemId);
            if (item != null) {
                item.setOrderIndex(i + 1);
            }
        }
        chapterItemRepository.saveAll(items);
    }

    @Override
    public List<ChapterItemResponse> getItemsByChapterForStudent(Integer chapterId) {
        User currentUser = userService.getCurrentUser();
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found"));
        if (!enrollmentRepository.existsByStudent_IdAndCourse_IdAndApprovalStatus(
                currentUser.getId(),
                chapter.getCourse().getId(),
                EnrollmentStatus.APPROVED
        )) {
            throw new UnauthorizedException("You are not enrolled in this course");
        }

        List<Integer> completedIds = progressRepository.findCompletedItemIdsByUserAndChapter(
                currentUser.getId(),
                chapterId
        );
        List<ChapterItem> items = chapterItemRepository.findByChapter_IdOrderByOrderIndexAsc(chapterId);
        if (items.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> lessonIds = items.stream()
                .filter(item -> item.getType() == ItemType.LESSON)
                .map(ChapterItem::getRefId)
                .toList();
        List<Integer> quizIds = items.stream()
                .filter(item -> item.getType() == ItemType.QUIZ)
                .map(ChapterItem::getRefId)
                .toList();

        Map<Integer, Lesson> lessonMap = lessonRepository.findAllById(lessonIds).stream()
                .collect(Collectors.toMap(Lesson::getId, Function.identity()));
        Map<Integer, Quiz> quizMap = quizRepository.findAllById(quizIds).stream()
                .collect(Collectors.toMap(Quiz::getId, Function.identity()));

        return items.stream().map(chapterItem -> {
            Object detail = null;
            if (chapterItem.getType() == ItemType.LESSON) {
                Lesson lesson = lessonMap.get(chapterItem.getRefId());
                if (lesson != null) {
                    detail = lessonService.convertEntityToDTO(lesson);
                }
            } else if (chapterItem.getType() == ItemType.QUIZ) {
                Quiz quiz = quizMap.get(chapterItem.getRefId());
                if (quiz != null) {
                    detail = quizService.convertQuizToDTO(quiz);
                }
            }

            return buildResponseForStudent(chapterItem, detail, completedIds.contains(chapterItem.getId()));
        }).toList();
    }

    private ChapterItemResponse buildResponseForStudent(ChapterItem chapterItem, Object itemDetail, Boolean isCompleted) {
        ChapterItemResponse response = new ChapterItemResponse();
        response.setId(chapterItem.getId());
        response.setType(chapterItem.getType());
        response.setOrderIndex(chapterItem.getOrderIndex());
        response.setItem(itemDetail);
        response.setCompleted(isCompleted);
        if (chapterItem.getChapter() != null) {
            response.setChapterId(chapterItem.getChapter().getId());
        }
        return response;
    }

    @Override
    public List<ChapterItemResponse> getItemsByChapter(Integer chapterId) {
        List<ChapterItem> items = chapterItemRepository.findByChapter_IdOrderByOrderIndexAsc(chapterId);
        if (items.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> lessonIds = items.stream()
                .filter(item -> item.getType() == ItemType.LESSON)
                .map(ChapterItem::getRefId)
                .toList();
        List<Integer> quizIds = items.stream()
                .filter(item -> item.getType() == ItemType.QUIZ)
                .map(ChapterItem::getRefId)
                .toList();

        Map<Integer, Lesson> lessonMap = lessonRepository.findAllById(lessonIds).stream()
                .collect(Collectors.toMap(Lesson::getId, Function.identity()));
        Map<Integer, Quiz> quizMap = quizRepository.findAllById(quizIds).stream()
                .collect(Collectors.toMap(Quiz::getId, Function.identity()));

        return items.stream().map(chapterItem -> {
            Object detail = null;
            if (chapterItem.getType() == ItemType.LESSON) {
                Lesson lesson = lessonMap.get(chapterItem.getRefId());
                if (lesson != null) {
                    detail = lessonService.convertEntityToDTO(lesson);
                }
            } else if (chapterItem.getType() == ItemType.QUIZ) {
                Quiz quiz = quizMap.get(chapterItem.getRefId());
                if (quiz != null) {
                    detail = quizService.convertQuizToDTO(quiz);
                }
            }
            return buildResponse(chapterItem, detail);
        }).toList();
    }

    @Transactional
    @Override
    public ChapterItemResponse createLessonInChapter(Integer chapterId, LessonRequest request) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found"));

        Lesson lesson = new Lesson();
        lesson.setTitle(request.getTitle());
        lesson.setContent(request.getContent());
        lessonRepository.save(lesson);

        ChapterItem chapterItem = createChapterItem(chapter, ItemType.LESSON, lesson.getId());
        return buildResponse(chapterItem, lessonService.convertEntityToDTO(lesson));
    }

    @Transactional
    @Override
    public ChapterItemResponse createQuizInChapter(Integer chapterId, QuizRequest request) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found"));

        QuizResponse quizResponse = quizService.createQuiz(request);
        ChapterItem chapterItem = createChapterItem(chapter, ItemType.QUIZ, quizResponse.getId());
        return buildResponse(chapterItem, quizResponse);
    }

    @Transactional
    @Override
    public void deleteChapterItem(Integer id) {
        ChapterItem chapterItem = chapterItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));
        if (chapterItem.getType() == ItemType.LESSON) {
            lessonService.deleteLesson(chapterItem.getRefId());
        }
        if (chapterItem.getType() == ItemType.QUIZ) {
            quizService.deleteQuiz(chapterItem.getRefId());
        }
        chapterItemRepository.delete(chapterItem);
    }

    @Transactional
    @Override
    public ChapterItemResponse addLessonToChapter(Integer chapterId, Integer lessonId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found"));
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));
        ChapterItem chapterItem = createChapterItem(chapter, ItemType.LESSON, lessonId);
        LessonResponse lessonResponse = lessonService.convertEntityToDTO(lesson);
        return buildResponse(chapterItem, lessonResponse);
    }

    @Transactional
    @Override
    public ChapterItemResponse addQuizToChapter(Integer chapterId, Integer quizId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found"));
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));
        ChapterItem chapterItem = createChapterItem(chapter, ItemType.QUIZ, quizId);
        QuizResponse quizResponse = quizService.convertQuizToDTO(quiz);
        return buildResponse(chapterItem, quizResponse);
    }

    private ChapterItem createChapterItem(Chapter chapter, ItemType type, Integer refId) {
        ChapterItem chapterItem = new ChapterItem();
        chapterItem.setChapter(chapter);
        chapterItem.setType(type);
        chapterItem.setRefId(refId);
        Integer maxOrder = chapterItemRepository.findMaxOrderIndexByChapter(chapter.getId());
        chapterItem.setOrderIndex(maxOrder == null ? 1 : maxOrder + 1);
        return chapterItemRepository.save(chapterItem);
    }

    private ChapterItemResponse buildResponse(ChapterItem chapterItem, Object itemDetail) {
        ChapterItemResponse response = new ChapterItemResponse();
        response.setId(chapterItem.getId());
        response.setType(chapterItem.getType());
        response.setOrderIndex(chapterItem.getOrderIndex());
        response.setItem(itemDetail);
        if (chapterItem.getChapter() != null) {
            response.setChapterId(chapterItem.getChapter().getId());
        }
        return response;
    }
}
