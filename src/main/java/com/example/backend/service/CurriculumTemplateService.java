package com.example.backend.service;

import com.example.backend.dto.request.curriculum.ChapterTemplateUpsertRequest;
import com.example.backend.dto.request.curriculum.ContentItemTemplateRequest;
import com.example.backend.dto.request.curriculum.CurriculumTemplateRequest;
import com.example.backend.dto.request.curriculum.LessonTemplateRequest;
import com.example.backend.dto.request.curriculum.QuizTemplateRequest;
import com.example.backend.dto.response.curriculum.ChapterTemplateResponse;
import com.example.backend.dto.response.curriculum.ContentItemTemplateResponse;
import com.example.backend.dto.response.curriculum.CurriculumTemplateResponse;
import com.example.backend.dto.response.curriculum.LessonTemplateResponse;
import com.example.backend.dto.response.curriculum.QuizTemplateResponse;

import java.util.List;

public interface CurriculumTemplateService {
    CurriculumTemplateResponse createTemplate(CurriculumTemplateRequest request);

    CurriculumTemplateResponse updateTemplate(Integer id, CurriculumTemplateRequest request);

    CurriculumTemplateResponse getTemplateById(Integer id);

    List<CurriculumTemplateResponse> getTemplates(String keyword, Integer categoryId, Integer subjectId, boolean includeChapters);

    ChapterTemplateResponse createChapter(Integer templateId, ChapterTemplateUpsertRequest request);

    ChapterTemplateResponse updateChapter(Integer templateId, Integer chapterId, ChapterTemplateUpsertRequest request);

    void deleteChapter(Integer templateId, Integer chapterId);

    ContentItemTemplateResponse createContentItem(Integer templateId, Integer chapterId, ContentItemTemplateRequest request);

    ContentItemTemplateResponse updateContentItem(
            Integer templateId,
            Integer chapterId,
            Integer contentItemId,
            ContentItemTemplateRequest request
    );

    void deleteContentItem(Integer templateId, Integer chapterId, Integer contentItemId);

    void deleteTemplate(Integer id);

    LessonTemplateResponse createLessonTemplate(LessonTemplateRequest request);

    LessonTemplateResponse getLessonTemplateById(Integer id);

    LessonTemplateResponse updateLessonTemplate(Integer id, LessonTemplateRequest request);

    QuizTemplateResponse createQuizTemplate(QuizTemplateRequest request);

    QuizTemplateResponse getQuizTemplateById(Integer id);
    QuizTemplateResponse getQuizTemplatePreviewSample(Integer id, Long seed);

    QuizTemplateResponse updateQuizTemplate(Integer id, QuizTemplateRequest request);
}
