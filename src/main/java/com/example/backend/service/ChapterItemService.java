package com.example.backend.service;

import com.example.backend.dto.request.LessonRequest;
import com.example.backend.dto.request.quiz.QuizRequest;
import com.example.backend.dto.response.chapter.ChapterItemResponse;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ChapterItemService {
    List<ChapterItemResponse> getItemsByChapterForStudent(Integer chapterId);

    @Transactional(readOnly = true)
    List<ChapterItemResponse> getItemsByChapter(Integer chapterId);

    @Transactional
    void updateOrder(Integer chapterId, List<Integer> orderedItemIds);

    @Transactional
    ChapterItemResponse createLessonInChapter(
            Integer chapterId,
            LessonRequest request
    );

    @Transactional
    ChapterItemResponse createQuizInChapter(
            Integer chapterId,
            QuizRequest request
    );

    @Transactional
    void deleteChapterItem(Integer id);

    @Transactional
    ChapterItemResponse addLessonToChapter(Integer chapterId, Integer lessonId);

    @Transactional
    ChapterItemResponse addQuizToChapter(Integer chapterId, Integer lessonId);

//    @Transactional
//   // ChapterItemResponse addQuizToChapter(Integer chapterId, Quiz quiz);
}
