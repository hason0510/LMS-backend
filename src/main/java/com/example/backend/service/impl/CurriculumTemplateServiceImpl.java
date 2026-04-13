package com.example.backend.service.impl;

import com.example.backend.constant.ContentItemType;
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
import com.example.backend.entity.template.ChapterTemplate;
import com.example.backend.entity.template.ContentItemTemplate;
import com.example.backend.entity.template.CurriculumTemplate;
import com.example.backend.entity.template.LessonTemplate;
import com.example.backend.entity.template.QuizTemplate;
import com.example.backend.entity.Subject;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.AssignmentRepository;
import com.example.backend.repository.AssignmentTemplateRepository;
import com.example.backend.repository.ChapterTemplateRepository;
import com.example.backend.repository.ContentItemTemplateRepository;
import com.example.backend.repository.CurriculumTemplateRepository;
import com.example.backend.repository.LessonRepository;
import com.example.backend.repository.LessonTemplateRepository;
import com.example.backend.repository.QuizRepository;
import com.example.backend.repository.QuizTemplateRepository;
import com.example.backend.repository.SubjectRepository;
import com.example.backend.service.CurriculumTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CurriculumTemplateServiceImpl implements CurriculumTemplateService {
    private final CurriculumTemplateRepository curriculumTemplateRepository;
    private final SubjectRepository subjectRepository;
    private final ChapterTemplateRepository chapterTemplateRepository;
    private final ContentItemTemplateRepository contentItemTemplateRepository;
    private final LessonRepository lessonRepository;
    private final LessonTemplateRepository lessonTemplateRepository;
    private final QuizRepository quizRepository;
    private final QuizTemplateRepository quizTemplateRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentTemplateRepository assignmentTemplateRepository;

    @Override
    @Transactional
    public CurriculumTemplateResponse createTemplate(CurriculumTemplateRequest request) {
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        CurriculumTemplate template = new CurriculumTemplate();
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setDefault(Boolean.TRUE.equals(request.getIsDefault()));
        template.setSubject(subject);

        return convertToResponse(curriculumTemplateRepository.save(template), false);
    }

    @Override
    @Transactional
    public CurriculumTemplateResponse updateTemplate(Integer id, CurriculumTemplateRequest request) {
        CurriculumTemplate template = curriculumTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curriculum template not found"));

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setDefault(Boolean.TRUE.equals(request.getIsDefault()));
        template.setSubject(subject);

        return convertToResponse(curriculumTemplateRepository.save(template), false);
    }

    @Override
    @Transactional(readOnly = true)
    public CurriculumTemplateResponse getTemplateById(Integer id) {
        CurriculumTemplate template = curriculumTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curriculum template not found"));
        return convertToResponse(template, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CurriculumTemplateResponse> getTemplates(Integer subjectId, boolean includeChapters) {
        List<CurriculumTemplate> templates = subjectId != null
                ? curriculumTemplateRepository.findBySubject_Id(subjectId)
                : curriculumTemplateRepository.findAll();

        return templates.stream()
                .map(template -> convertToResponse(template, includeChapters))
                .toList();
    }

    @Override
    @Transactional
    public ChapterTemplateResponse createChapter(Integer templateId, ChapterTemplateUpsertRequest request) {
        CurriculumTemplate template = getTemplate(templateId);

        ChapterTemplate chapter = new ChapterTemplate();
        chapter.setCurriculumTemplate(template);
        chapter.setTitle(request.getTitle().trim());
        chapter.setDescription(request.getDescription());
        chapter.setOrderIndex(resolveChapterOrderIndex(templateId, request.getOrderIndex()));

        return convertChapterToResponse(chapterTemplateRepository.save(chapter));
    }

    @Override
    @Transactional
    public ChapterTemplateResponse updateChapter(Integer templateId, Integer chapterId, ChapterTemplateUpsertRequest request) {
        getTemplate(templateId);
        ChapterTemplate chapter = chapterTemplateRepository.findByIdAndCurriculumTemplate_Id(chapterId, templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter template not found in this curriculum template"));

        chapter.setTitle(request.getTitle().trim());
        chapter.setDescription(request.getDescription());
        if (request.getOrderIndex() != null) {
            chapter.setOrderIndex(request.getOrderIndex());
        }

        return convertChapterToResponse(chapterTemplateRepository.save(chapter));
    }

    @Override
    @Transactional
    public void deleteChapter(Integer templateId, Integer chapterId) {
        getTemplate(templateId);
        ChapterTemplate chapter = chapterTemplateRepository.findByIdAndCurriculumTemplate_Id(chapterId, templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter template not found in this curriculum template"));
        chapterTemplateRepository.delete(chapter);
    }

    @Override
    @Transactional
    public ContentItemTemplateResponse createContentItem(
            Integer templateId,
            Integer chapterId,
            ContentItemTemplateRequest request
    ) {
        getTemplate(templateId);
        ChapterTemplate chapter = chapterTemplateRepository.findByIdAndCurriculumTemplate_Id(chapterId, templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter template not found in this curriculum template"));

        ContentItemTemplate contentItem = new ContentItemTemplate();
        contentItem.setChapterTemplate(chapter);
        contentItem.setItemType(request.getItemType());
        contentItem.setOrderIndex(resolveContentItemOrderIndex(chapterId, request.getOrderIndex()));
        applyContentReference(contentItem, request);

        return convertContentItemToResponse(contentItemTemplateRepository.save(contentItem));
    }

    @Override
    @Transactional
    public ContentItemTemplateResponse updateContentItem(
            Integer templateId,
            Integer chapterId,
            Integer contentItemId,
            ContentItemTemplateRequest request
    ) {
        getTemplate(templateId);
        chapterTemplateRepository.findByIdAndCurriculumTemplate_Id(chapterId, templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter template not found in this curriculum template"));

        ContentItemTemplate contentItem = contentItemTemplateRepository.findByIdAndChapterTemplate_Id(contentItemId, chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Content item template not found in this chapter"));

        contentItem.setItemType(request.getItemType());
        if (request.getOrderIndex() != null) {
            contentItem.setOrderIndex(request.getOrderIndex());
        }
        applyContentReference(contentItem, request);

        return convertContentItemToResponse(contentItemTemplateRepository.save(contentItem));
    }

    @Override
    @Transactional
    public void deleteContentItem(Integer templateId, Integer chapterId, Integer contentItemId) {
        getTemplate(templateId);
        chapterTemplateRepository.findByIdAndCurriculumTemplate_Id(chapterId, templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter template not found in this curriculum template"));

        ContentItemTemplate contentItem = contentItemTemplateRepository.findByIdAndChapterTemplate_Id(contentItemId, chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Content item template not found in this chapter"));
        contentItemTemplateRepository.delete(contentItem);
    }

    @Override
    @Transactional
    public void deleteTemplate(Integer id) {
        CurriculumTemplate template = getTemplate(id);
        curriculumTemplateRepository.delete(template);
    }

    private CurriculumTemplate getTemplate(Integer id) {
        return curriculumTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curriculum template not found"));
    }

    private Integer resolveChapterOrderIndex(Integer templateId, Integer requestedOrderIndex) {
        if (requestedOrderIndex != null) {
            return requestedOrderIndex;
        }

        return chapterTemplateRepository.findByCurriculumTemplate_IdOrderByOrderIndexAsc(templateId).stream()
                .map(ChapterTemplate::getOrderIndex)
                .filter(orderIndex -> orderIndex != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private Integer resolveContentItemOrderIndex(Integer chapterId, Integer requestedOrderIndex) {
        if (requestedOrderIndex != null) {
            return requestedOrderIndex;
        }

        return contentItemTemplateRepository.findByChapterTemplate_IdOrderByOrderIndexAsc(chapterId).stream()
                .map(ContentItemTemplate::getOrderIndex)
                .filter(orderIndex -> orderIndex != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private void applyContentReference(ContentItemTemplate item, ContentItemTemplateRequest request) {
        item.setLessonTemplate(null);
        item.setQuizTemplate(null);
        item.setAssignmentTemplate(null);

        if (request.getItemType() == ContentItemType.LESSON) {
            if (request.getLessonTemplateId() != null) {
                item.setLessonTemplate(lessonTemplateRepository.findById(request.getLessonTemplateId())
                        .orElseThrow(() -> new ResourceNotFoundException("Lesson template not found")));
            }
            return;
        }
        if (request.getItemType() == ContentItemType.QUIZ) {
            if (request.getQuizTemplateId() != null) {
                item.setQuizTemplate(quizTemplateRepository.findById(request.getQuizTemplateId())
                        .orElseThrow(() -> new ResourceNotFoundException("Quiz template not found")));
            }
            return;
        }
        if (request.getItemType() == ContentItemType.ASSIGNMENT) {
            if (request.getAssignmentTemplateId() != null) {
                item.setAssignmentTemplate(assignmentTemplateRepository.findById(request.getAssignmentTemplateId())
                        .orElseThrow(() -> new ResourceNotFoundException("Assignment template not found")));
            }
            return;
        }
        throw new BusinessException("Unsupported content item type");
    }

    private CurriculumTemplateResponse convertToResponse(CurriculumTemplate template, boolean includeChapters) {
        CurriculumTemplateResponse response = new CurriculumTemplateResponse();
        response.setId(template.getId());
        response.setName(template.getName());
        response.setDescription(template.getDescription());
        response.setIsDefault(template.isDefault());
        response.setSubjectId(template.getSubject() != null ? template.getSubject().getId() : null);
        response.setSubjectTitle(template.getSubject() != null ? template.getSubject().getTitle() : null);
        if (template.getSubject() != null && template.getSubject().getCategory() != null) {
            response.setCategoryId(template.getSubject().getCategory().getId());
            response.setCategoryTitle(template.getSubject().getCategory().getTitle());
        }
        response.setChapters(includeChapters
                ? (template.getChapters() != null ? template.getChapters() : Collections.<ChapterTemplate>emptyList()).stream()
                .map(this::convertChapterToResponse)
                .toList()
                : null);
        return response;
    }

    private ChapterTemplateResponse convertChapterToResponse(ChapterTemplate chapter) {
        ChapterTemplateResponse response = new ChapterTemplateResponse();
        response.setId(chapter.getId());
        response.setTitle(chapter.getTitle());
        response.setDescription(chapter.getDescription());
        response.setOrderIndex(chapter.getOrderIndex());
        response.setContentItems((chapter.getContentItems() != null ? chapter.getContentItems() : Collections.<ContentItemTemplate>emptyList()).stream()
                .map(this::convertContentItemToResponse)
                .toList());
        return response;
    }

    private ContentItemTemplateResponse convertContentItemToResponse(ContentItemTemplate item) {
        ContentItemTemplateResponse response = new ContentItemTemplateResponse();
        response.setId(item.getId());
        response.setItemType(item.getItemType());
        response.setOrderIndex(item.getOrderIndex());
        response.setLessonTemplateId(item.getLessonTemplate() != null ? item.getLessonTemplate().getId() : null);
        response.setLessonTemplateTitle(item.getLessonTemplate() != null ? item.getLessonTemplate().getTitle() : null);
        response.setQuizTemplateId(item.getQuizTemplate() != null ? item.getQuizTemplate().getId() : null);
        response.setQuizTemplateTitle(item.getQuizTemplate() != null ? item.getQuizTemplate().getTitle() : null);
        response.setAssignmentTemplateId(item.getAssignmentTemplate() != null ? item.getAssignmentTemplate().getId() : null);
        response.setAssignmentTemplateTitle(item.getAssignmentTemplate() != null ? item.getAssignmentTemplate().getTitle() : null);
        return response;
    }

    // ── Lesson Template CRUD ──────────────────────────────────────────────────

    @Override
    @Transactional
    public LessonTemplateResponse createLessonTemplate(LessonTemplateRequest request) {
        LessonTemplate lessonTemplate = new LessonTemplate();
        lessonTemplate.setTitle(request.getTitle().trim());
        lessonTemplate.setContent(request.getContent());
        lessonTemplate.setVideoUrl(request.getVideoUrl());
        lessonTemplate.setNotes(request.getNotes());
        return convertLessonTemplateToResponse(lessonTemplateRepository.save(lessonTemplate));
    }

    @Override
    public LessonTemplateResponse getLessonTemplateById(Integer id) {
        LessonTemplate lessonTemplate = lessonTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson template not found"));
        return convertLessonTemplateToResponse(lessonTemplate);
    }

    @Override
    @Transactional
    public LessonTemplateResponse updateLessonTemplate(Integer id, LessonTemplateRequest request) {
        LessonTemplate lessonTemplate = lessonTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson template not found"));
        lessonTemplate.setTitle(request.getTitle().trim());
        lessonTemplate.setContent(request.getContent());
        lessonTemplate.setVideoUrl(request.getVideoUrl());
        lessonTemplate.setNotes(request.getNotes());
        return convertLessonTemplateToResponse(lessonTemplateRepository.save(lessonTemplate));
    }

    private LessonTemplateResponse convertLessonTemplateToResponse(LessonTemplate lessonTemplate) {
        LessonTemplateResponse response = new LessonTemplateResponse();
        response.setId(lessonTemplate.getId());
        response.setTitle(lessonTemplate.getTitle());
        response.setContent(lessonTemplate.getContent());
        response.setVideoUrl(lessonTemplate.getVideoUrl());
        response.setNotes(lessonTemplate.getNotes());
        return response;
    }

    // ── Quiz Template CRUD ────────────────────────────────────────────────────

    @Override
    @Transactional
    public QuizTemplateResponse createQuizTemplate(QuizTemplateRequest request) {
        QuizTemplate quizTemplate = new QuizTemplate();
        quizTemplate.setTitle(request.getTitle().trim());
        quizTemplate.setDescription(request.getDescription());
        quizTemplate.setMinPassScore(request.getMinPassScore());
        quizTemplate.setTimeLimitMinutes(request.getTimeLimitMinutes());
        quizTemplate.setMaxAttempts(request.getMaxAttempts());
        quizTemplate.setAvailableFrom(request.getAvailableFrom());
        quizTemplate.setAvailableTo(request.getAvailableTo());
        return convertQuizTemplateToResponse(quizTemplateRepository.save(quizTemplate));
    }

    @Override
    @Transactional(readOnly = true)
    public QuizTemplateResponse getQuizTemplateById(Integer id) {
        QuizTemplate quizTemplate = quizTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz template not found"));
        return convertQuizTemplateToResponse(quizTemplate);
    }

    @Override
    @Transactional
    public QuizTemplateResponse updateQuizTemplate(Integer id, QuizTemplateRequest request) {
        QuizTemplate quizTemplate = quizTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz template not found"));
        quizTemplate.setTitle(request.getTitle().trim());
        quizTemplate.setDescription(request.getDescription());
        quizTemplate.setMinPassScore(request.getMinPassScore());
        quizTemplate.setTimeLimitMinutes(request.getTimeLimitMinutes());
        quizTemplate.setMaxAttempts(request.getMaxAttempts());
        quizTemplate.setAvailableFrom(request.getAvailableFrom());
        quizTemplate.setAvailableTo(request.getAvailableTo());
        return convertQuizTemplateToResponse(quizTemplateRepository.save(quizTemplate));
    }

    private QuizTemplateResponse convertQuizTemplateToResponse(QuizTemplate quizTemplate) {
        QuizTemplateResponse response = new QuizTemplateResponse();
        response.setId(quizTemplate.getId());
        response.setTitle(quizTemplate.getTitle());
        response.setDescription(quizTemplate.getDescription());
        response.setMinPassScore(quizTemplate.getMinPassScore());
        response.setTimeLimitMinutes(quizTemplate.getTimeLimitMinutes());
        response.setMaxAttempts(quizTemplate.getMaxAttempts());
        response.setAvailableFrom(quizTemplate.getAvailableFrom());
        response.setAvailableTo(quizTemplate.getAvailableTo());
        return response;
    }
}
