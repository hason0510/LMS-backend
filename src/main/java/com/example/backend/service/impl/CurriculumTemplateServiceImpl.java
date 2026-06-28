package com.example.backend.service.impl;

import com.example.backend.constant.ContentItemType;
import com.example.backend.constant.QuestionInteractionItemRole;
import com.example.backend.constant.QuestionType;
import com.example.backend.constant.QuizSourceSelectionMode;
import com.example.backend.constant.QuizTagMatchMode;
import com.example.backend.dto.request.curriculum.ChapterTemplateUpsertRequest;
import com.example.backend.dto.request.curriculum.ContentItemTemplateRequest;
import com.example.backend.dto.request.curriculum.CurriculumTemplateRequest;
import com.example.backend.dto.request.curriculum.LessonTemplateRequest;
import com.example.backend.dto.request.curriculum.QuizTemplateRequest;
import com.example.backend.dto.request.quiz.QuestionInteractionItemRequest;
import com.example.backend.dto.request.quiz.QuizAnswerRequest;
import com.example.backend.dto.request.quiz.QuizBankSourceRequest;
import com.example.backend.dto.request.quiz.QuizQuestionRequest;
import com.example.backend.dto.response.curriculum.ChapterTemplateResponse;
import com.example.backend.dto.response.curriculum.ContentItemTemplateResponse;
import com.example.backend.dto.response.curriculum.CurriculumTemplateResponse;
import com.example.backend.dto.response.curriculum.LessonTemplateResponse;
import com.example.backend.dto.response.curriculum.QuizTemplateResponse;
import com.example.backend.dto.response.quiz.QuestionInteractionItemResponse;
import com.example.backend.dto.response.quiz.QuizAnswerResponse;
import com.example.backend.dto.response.quiz.QuizBankSourceResponse;
import com.example.backend.dto.response.quiz.QuizQuestionResponse;
import com.example.backend.dto.response.ResourceResponse;
import com.example.backend.entity.quiz.QuestionBank;
import com.example.backend.entity.quiz.QuestionTag;
import com.example.backend.entity.resource.Resource;
import com.example.backend.entity.template.ChapterTemplate;
import com.example.backend.entity.template.ContentItemTemplate;
import com.example.backend.entity.template.CurriculumTemplate;
import com.example.backend.entity.template.LessonTemplate;
import com.example.backend.entity.template.QuizTemplate;
import com.example.backend.entity.template.QuizTemplateAnswer;
import com.example.backend.entity.template.QuizTemplateBankSource;
import com.example.backend.entity.template.QuizTemplateQuestion;
import com.example.backend.entity.template.QuizTemplateQuestionItem;
import com.example.backend.entity.Subject;
import com.example.backend.entity.quiz.BankQuestion;
import com.example.backend.entity.quiz.BankQuestionOption;
import com.example.backend.entity.quiz.QuestionInteractionItem;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.AssignmentRepository;
import com.example.backend.repository.BankQuestionRepository;
import com.example.backend.repository.ChapterTemplateRepository;
import com.example.backend.repository.ContentItemTemplateRepository;
import com.example.backend.repository.CurriculumTemplateRepository;
import com.example.backend.repository.LessonRepository;
import com.example.backend.repository.LessonTemplateRepository;
import com.example.backend.repository.QuestionBankRepository;
import com.example.backend.repository.QuestionTagRepository;
import com.example.backend.repository.QuizRepository;
import com.example.backend.repository.QuizTemplateBankSourceRepository;
import com.example.backend.repository.QuizTemplateQuestionRepository;
import com.example.backend.repository.QuizTemplateRepository;
import com.example.backend.repository.ResourceRepository;
import com.example.backend.repository.SubjectRepository;
import com.example.backend.service.CurriculumTemplateService;
import com.example.backend.service.ResourceService;
import com.example.backend.specification.CurriculumTemplateSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CurriculumTemplateServiceImpl implements CurriculumTemplateService {
    private static final String ENTITY_LESSON_TEMPLATE = "LESSON_TEMPLATE";

    private final CurriculumTemplateRepository curriculumTemplateRepository;
    private final SubjectRepository subjectRepository;
    private final ChapterTemplateRepository chapterTemplateRepository;
    private final ContentItemTemplateRepository contentItemTemplateRepository;
    private final LessonRepository lessonRepository;
    private final LessonTemplateRepository lessonTemplateRepository;
    private final QuizRepository quizRepository;
    private final QuizTemplateRepository quizTemplateRepository;
    private final QuizTemplateQuestionRepository quizTemplateQuestionRepository;
    private final QuizTemplateBankSourceRepository quizTemplateBankSourceRepository;
    private final AssignmentRepository assignmentRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionTagRepository questionTagRepository;
    private final BankQuestionRepository bankQuestionRepository;
    private final ResourceRepository resourceRepository;
    private final ResourceService resourceService;

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
    public List<CurriculumTemplateResponse> getTemplates(
            String keyword,
            Integer categoryId,
            Integer subjectId,
            boolean includeChapters
    ) {
        Specification<CurriculumTemplate> specification = Specification
                .allOf(CurriculumTemplateSpecification.nameContains(keyword))
                .and(CurriculumTemplateSpecification.hasCategoryId(categoryId))
                .and(CurriculumTemplateSpecification.hasSubjectId(subjectId));

        List<CurriculumTemplate> templates = curriculumTemplateRepository.findAll(specification);

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
        if (request.getItemType() == null) {
            throw new BusinessException("Item type is required");
        }

        int referenceCount = countNotNull(request.getLessonTemplateId(), request.getQuizTemplateId());
        if (referenceCount == 0) {
            throw new BusinessException("lessonTemplateId/quizTemplateId is required for content item template");
        }
        if (referenceCount > 1) {
            throw new BusinessException("Only one template reference can be set at a time");
        }

        if (request.getItemType() == ContentItemType.LESSON) {
            if (request.getLessonTemplateId() == null) {
                throw new BusinessException("lessonTemplateId is required for LESSON content item templates");
            }
            LessonTemplate lessonTemplate = lessonTemplateRepository.findById(request.getLessonTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lesson template not found"));
            assertTemplateReferenceAvailable(
                    contentItemTemplateRepository.findByLessonTemplate_Id(request.getLessonTemplateId()),
                    item,
                    "Lesson template is already linked to another content item template"
            );
            clearTemplateReferences(item);
            item.setLessonTemplate(lessonTemplate);
            return;
        }
        if (request.getItemType() == ContentItemType.QUIZ) {
            if (request.getQuizTemplateId() == null) {
                throw new BusinessException("quizTemplateId is required for QUIZ content item templates");
            }
            QuizTemplate quizTemplate = quizTemplateRepository.findById(request.getQuizTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Quiz template not found"));
            assertTemplateReferenceAvailable(
                    contentItemTemplateRepository.findByQuizTemplate_Id(request.getQuizTemplateId()),
                    item,
                    "Quiz template is already linked to another content item template"
            );
            clearTemplateReferences(item);
            item.setQuizTemplate(quizTemplate);
            return;
        }
        throw new BusinessException("Unsupported content item type");
    }

    private void assertTemplateReferenceAvailable(
            Optional<ContentItemTemplate> existing,
            ContentItemTemplate target,
            String message
    ) {
        existing
                .filter(item -> target.getId() == null || !item.getId().equals(target.getId()))
                .ifPresent(item -> {
                    throw new BusinessException(message);
                });
    }

    private void clearTemplateReferences(ContentItemTemplate item) {
        item.setLessonTemplate(null);
        item.setQuizTemplate(null);
    }

    private int countNotNull(Object... values) {
        int count = 0;
        for (Object value : values) {
            if (value != null) {
                count++;
            }
        }
        return count;
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
        LessonTemplate savedTemplate = lessonTemplateRepository.save(lessonTemplate);
        if (request.getResourceIds() != null) {
            resourceService.replaceAttachedResources(ENTITY_LESSON_TEMPLATE, savedTemplate.getId(), request.getResourceIds());
        }
        return convertLessonTemplateToResponse(savedTemplate);
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
        LessonTemplate savedTemplate = lessonTemplateRepository.save(lessonTemplate);
        if (request.getResourceIds() != null) {
            resourceService.replaceAttachedResources(ENTITY_LESSON_TEMPLATE, savedTemplate.getId(), request.getResourceIds());
        }
        return convertLessonTemplateToResponse(savedTemplate);
    }

    private LessonTemplateResponse convertLessonTemplateToResponse(LessonTemplate lessonTemplate) {
        LessonTemplateResponse response = new LessonTemplateResponse();
        response.setId(lessonTemplate.getId());
        response.setTitle(lessonTemplate.getTitle());
        response.setContent(lessonTemplate.getContent());
        response.setVideoUrl(lessonTemplate.getVideoUrl());
        response.setNotes(lessonTemplate.getNotes());
        response.setResources(resourceService.getResourcesByLessonTemplateId(lessonTemplate.getId()));
        return response;
    }

    // ── Quiz Template CRUD ────────────────────────────────────────────────────

    @Override
    @Transactional
    public QuizTemplateResponse createQuizTemplate(QuizTemplateRequest request) {
        validateQuizStructureRequest(request);
        QuizTemplate quizTemplate = new QuizTemplate();
        applyQuizTemplateMetadata(quizTemplate, request);
        QuizTemplate savedTemplate = quizTemplateRepository.save(quizTemplate);
        syncQuizTemplateContent(savedTemplate, request);
        return convertQuizTemplateToResponse(savedTemplate);
    }

    @Override
    @Transactional(readOnly = true)
    public QuizTemplateResponse getQuizTemplateById(Integer id) {
        QuizTemplate quizTemplate = quizTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz template not found"));
        return convertQuizTemplateToResponse(quizTemplate);
    }

    @Override
    @Transactional(readOnly = true)
    public QuizTemplateResponse getQuizTemplatePreviewSample(Integer id, Long seed) {
        QuizTemplate quizTemplate = quizTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz template not found"));
        QuizTemplateResponse response = convertQuizTemplateToResponse(quizTemplate);

        long effectiveSeed = seed != null ? seed : (20_000L + id);
        Random random = new Random(effectiveSeed);
        List<QuizQuestionResponse> previewQuestions = buildTemplatePreviewQuestions(quizTemplate, random);
        response.setQuestions(previewQuestions);
        response.setQuestionCount(previewQuestions.size());
        return response;
    }

    @Override
    @Transactional
    public QuizTemplateResponse updateQuizTemplate(Integer id, QuizTemplateRequest request) {
        validateQuizStructureRequest(request);
        QuizTemplate quizTemplate = quizTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz template not found"));
        applyQuizTemplateMetadata(quizTemplate, request);
        QuizTemplate savedTemplate = quizTemplateRepository.save(quizTemplate);
        syncQuizTemplateContent(savedTemplate, request);
        return convertQuizTemplateToResponse(savedTemplate);
    }

    private void applyQuizTemplateMetadata(QuizTemplate quizTemplate, QuizTemplateRequest request) {
        quizTemplate.setTitle(request.getTitle().trim());
        quizTemplate.setDescription(request.getDescription());
        quizTemplate.setMinPassScore(request.getMinPassScore());
        quizTemplate.setTimeLimitMinutes(request.getTimeLimitMinutes());
        quizTemplate.setMaxAttempts(request.getMaxAttempts());
        quizTemplate.setAvailableFrom(request.getAvailableFrom());
        quizTemplate.setAvailableTo(request.getAvailableTo());
        quizTemplate.setGenerateQuestionsPerAttempt(Boolean.TRUE.equals(request.getGenerateQuestionsPerAttempt()));
        quizTemplate.setShuffleQuestions(Boolean.TRUE.equals(request.getShuffleQuestions()));
        quizTemplate.setShuffleAnswers(Boolean.TRUE.equals(request.getShuffleAnswers()));
        quizTemplate.setDisplayMode(StringUtils.hasText(request.getDisplayMode()) ? request.getDisplayMode().trim() : "PAGINATION");
        quizTemplate.setShowCorrectAnswer(Boolean.TRUE.equals(request.getShowCorrectAnswer()));
    }

    private void syncQuizTemplateContent(QuizTemplate quizTemplate, QuizTemplateRequest request) {
        boolean hasBankSourcesField = request.getBankSources() != null;
        boolean hasQuestionsField = request.getQuestions() != null;
        if (!hasBankSourcesField && !hasQuestionsField) {
            return;
        }

        validateQuizTemplateContentPresence(quizTemplate, request);

        clearQuizTemplateSourcesAndQuestions(quizTemplate.getId());

        if (request.getBankSources() != null && !request.getBankSources().isEmpty()) {
            if (!quizTemplate.isGenerateQuestionsPerAttempt()) {
                quizTemplate.setGenerateQuestionsPerAttempt(true);
                quizTemplateRepository.save(quizTemplate);
            }
            saveTemplateBankSources(quizTemplate, request.getBankSources());
            return;
        }

        if (request.getQuestions() != null && !request.getQuestions().isEmpty()) {
            createTemplateManualQuestions(quizTemplate, request.getQuestions());
        }
    }

    private void validateQuizTemplateContentPresence(QuizTemplate quizTemplate, QuizTemplateRequest request) {
        boolean hasBankSources = request.getBankSources() != null && !request.getBankSources().isEmpty();
        boolean hasManualQuestions = request.getQuestions() != null && !request.getQuestions().isEmpty();

        if (!hasBankSources && !hasManualQuestions) {
            throw new BusinessException("Quiz template must contain at least one question");
        }

        if (hasBankSources) {
            validateTemplateBankSourcesCanProvideQuestions(quizTemplate, request.getBankSources());
        }
    }

    private void validateTemplateBankSourcesCanProvideQuestions(
            QuizTemplate quizTemplate,
            List<QuizBankSourceRequest> sourceRequests
    ) {
        boolean hasAtLeastOneQuestion = false;

        for (QuizBankSourceRequest sourceRequest : sourceRequests) {
            if (sourceRequest.getQuestionBankId() == null) {
                throw new BusinessException("questionBankId is required for each question bank source");
            }

            QuestionBank questionBank = questionBankRepository.findById(sourceRequest.getQuestionBankId())
                    .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));

            List<Integer> tagIds = normalizeTagIds(sourceRequest.getTagIds());
            List<QuestionTag> tags = resolveTags(tagIds);

            if (sourceRequest.getSelectionMode() == null) {
                throw new BusinessException("selectionMode is required for question bank sources");
            }
            if (sourceRequest.getSelectionMode() == QuizSourceSelectionMode.MANUAL) {
                throw new BusinessException("Manual selection in question bank mode is not supported. Use Questions Library for manual questions.");
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

            QuizTemplateBankSource previewSource = new QuizTemplateBankSource();
            previewSource.setQuizTemplate(quizTemplate);
            previewSource.setQuestionBank(questionBank);
            previewSource.setSelectionMode(sourceRequest.getSelectionMode());
            previewSource.setQuestionCount(sourceRequest.getQuestionCount());
            previewSource.setDifficultyLevel(sourceRequest.getDifficultyLevel());
            previewSource.setTags(new ArrayList<>(tags));
            previewSource.setTagMatchMode(sourceRequest.getTagMatchMode() != null
                    ? sourceRequest.getTagMatchMode()
                    : QuizTagMatchMode.ANY);

            List<BankQuestion> matchedQuestions = resolveTemplateMatchedQuestions(previewSource);

            if (sourceRequest.getSelectionMode() == QuizSourceSelectionMode.ALL_MATCHED) {
                hasAtLeastOneQuestion = hasAtLeastOneQuestion || !matchedQuestions.isEmpty();
                continue;
            }

            if (sourceRequest.getSelectionMode() == QuizSourceSelectionMode.RANDOM) {
                if (matchedQuestions.size() < sourceRequest.getQuestionCount()) {
                    throw new BusinessException("Not enough questions in question bank source to satisfy questionCount");
                }
                hasAtLeastOneQuestion = true;
            }
        }

        if (!hasAtLeastOneQuestion) {
            throw new BusinessException("Quiz template must contain at least one question");
        }
    }

    private void clearQuizTemplateSourcesAndQuestions(Integer quizTemplateId) {
        quizTemplateBankSourceRepository.deleteAll(
                quizTemplateBankSourceRepository.findByQuizTemplate_IdOrderByOrderIndexAsc(quizTemplateId)
        );
        quizTemplateQuestionRepository.deleteAll(
                quizTemplateQuestionRepository.findByQuizTemplate_IdOrderByOrderIndexAscIdAsc(quizTemplateId)
        );
    }

    private void saveTemplateBankSources(QuizTemplate quizTemplate, List<QuizBankSourceRequest> sourceRequests) {
        for (int i = 0; i < sourceRequests.size(); i++) {
            QuizBankSourceRequest sourceRequest = sourceRequests.get(i);
            if (sourceRequest.getQuestionBankId() == null) {
                throw new BusinessException("questionBankId is required for each question bank source");
            }

            var questionBank = questionBankRepository.findById(sourceRequest.getQuestionBankId())
                    .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));

            List<Integer> tagIds = normalizeTagIds(sourceRequest.getTagIds());
            List<com.example.backend.entity.quiz.QuestionTag> tags = resolveTags(tagIds);

            if (sourceRequest.getSelectionMode() == null) {
                throw new BusinessException("selectionMode is required for question bank sources");
            }
            if (sourceRequest.getSelectionMode() == QuizSourceSelectionMode.MANUAL) {
                throw new BusinessException("Manual selection in question bank mode is not supported. Use Questions Library for manual questions.");
            }
            if (sourceRequest.getSelectionMode() == QuizSourceSelectionMode.RANDOM
                    && (sourceRequest.getQuestionCount() == null || sourceRequest.getQuestionCount() <= 0)) {
                throw new BusinessException("questionCount is required for RANDOM question bank sources");
            }
            for (var tag : tags) {
                if (tag.getQuestionBank() != null
                        && !Objects.equals(tag.getQuestionBank().getId(), questionBank.getId())) {
                    throw new BusinessException("Question tag must belong to the same question bank");
                }
            }

            QuizTemplateBankSource source = new QuizTemplateBankSource();
            source.setQuizTemplate(quizTemplate);
            source.setQuestionBank(questionBank);
            source.setOrderIndex(i + 1);
            source.setSelectionMode(sourceRequest.getSelectionMode());
            source.setQuestionCount(sourceRequest.getQuestionCount());
            source.setDifficultyLevel(sourceRequest.getDifficultyLevel());
            source.setTags(new ArrayList<>(tags));
            source.setTagMatchMode(sourceRequest.getTagMatchMode() != null ? sourceRequest.getTagMatchMode() : QuizTagMatchMode.ANY);
            quizTemplateBankSourceRepository.save(source);
        }
    }

    private void createTemplateManualQuestions(QuizTemplate quizTemplate, List<QuizQuestionRequest> questionRequests) {
        int questionOrder = 1;
        for (QuizQuestionRequest questionRequest : questionRequests) {
            validateInteractionItems(questionRequest.getType(), questionRequest.getItems());

            QuizTemplateQuestion question = new QuizTemplateQuestion();
            question.setQuizTemplate(quizTemplate);
            question.setOrderIndex(questionOrder++);
            if (questionRequest.getSourceBankQuestionId() != null) {
                BankQuestion sourceQuestion = bankQuestionRepository.findById(questionRequest.getSourceBankQuestionId())
                        .orElseThrow(() -> new ResourceNotFoundException("Source bank question not found"));
                question.setSourceBankQuestion(sourceQuestion);
            }
            question.setContent(questionRequest.getContent());
            question.setType(questionRequest.getType());
            question.setPoints(questionRequest.getPoints() != null ? questionRequest.getPoints() : BigDecimal.ONE);
            if (questionRequest.getResourceId() != null) {
                question.setResource(resourceRepository.findById(questionRequest.getResourceId()).orElse(null));
            }

            List<QuizTemplateAnswer> answers = new ArrayList<>();
            if (!isInteractionQuestionType(questionRequest.getType()) && questionRequest.getAnswers() != null) {
                for (QuizAnswerRequest answerRequest : questionRequest.getAnswers()) {
                    QuizTemplateAnswer answer = new QuizTemplateAnswer();
                    answer.setQuizTemplateQuestion(question);
                    answer.setContent(answerRequest.getContent());
                    answer.setIsCorrect(answerRequest.getIsCorrect() != null ? answerRequest.getIsCorrect() : false);
                    answer.setExplanation(answerRequest.getExplanation());
                    if (answerRequest.getResourceId() != null) {
                        answer.setResource(resourceRepository.findById(answerRequest.getResourceId()).orElse(null));
                    }
                    answers.add(answer);
                }
            }
            question.setAnswers(answers);
            question.setItems(buildTemplateInteractionItems(question, questionRequest.getItems()));
            quizTemplateQuestionRepository.save(question);
        }
    }

    private List<QuizTemplateQuestionItem> buildTemplateInteractionItems(
            QuizTemplateQuestion question,
            List<QuestionInteractionItemRequest> requests
    ) {
        List<QuizTemplateQuestionItem> items = new ArrayList<>();
        int orderIndex = 1;
        if (requests != null) {
            for (QuestionInteractionItemRequest itemRequest : requests) {
                QuizTemplateQuestionItem item = new QuizTemplateQuestionItem();
                item.setQuizTemplateQuestion(question);
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
                    item.setResource(resourceRepository.findById(itemRequest.getResourceId()).orElse(null));
                }
                item.setOrderIndex(itemRequest.getOrderIndex() != null ? itemRequest.getOrderIndex() : orderIndex);
                items.add(item);
                orderIndex++;
            }
        }
        return items;
    }

    private QuizTemplateResponse convertQuizTemplateToResponse(QuizTemplate quizTemplate) {
        List<QuizTemplateQuestion> questions =
                quizTemplateQuestionRepository.findByQuizTemplate_IdOrderByOrderIndexAscIdAsc(quizTemplate.getId());
        List<QuizTemplateBankSource> bankSources =
                quizTemplateBankSourceRepository.findByQuizTemplate_IdOrderByOrderIndexAsc(quizTemplate.getId());

        QuizTemplateResponse response = new QuizTemplateResponse();
        response.setId(quizTemplate.getId());
        response.setTitle(quizTemplate.getTitle());
        response.setDescription(quizTemplate.getDescription());
        response.setMinPassScore(quizTemplate.getMinPassScore());
        response.setTimeLimitMinutes(quizTemplate.getTimeLimitMinutes());
        response.setMaxAttempts(quizTemplate.getMaxAttempts());
        response.setAvailableFrom(quizTemplate.getAvailableFrom());
        response.setAvailableTo(quizTemplate.getAvailableTo());
        response.setGenerateQuestionsPerAttempt(quizTemplate.isGenerateQuestionsPerAttempt());
        response.setShuffleQuestions(quizTemplate.isShuffleQuestions());
        response.setShuffleAnswers(quizTemplate.isShuffleAnswers());
        response.setDisplayMode(quizTemplate.getDisplayMode());
        response.setShowCorrectAnswer(quizTemplate.isShowCorrectAnswer());
        response.setQuestionCount(resolveTemplateQuestionCount(quizTemplate, questions, bankSources));
        response.setBankSources(bankSources.stream().map(this::convertTemplateBankSourceToDTO).toList());
        response.setQuestions(questions.stream().map(this::convertTemplateQuestionToDTO).toList());
        return response;
    }

    private Integer resolveTemplateQuestionCount(
            QuizTemplate quizTemplate,
            List<QuizTemplateQuestion> questions,
            List<QuizTemplateBankSource> bankSources
    ) {
        if (!quizTemplate.isGenerateQuestionsPerAttempt()) {
            return questions != null ? questions.size() : 0;
        }

        if (bankSources == null || bankSources.isEmpty()) {
            return questions != null ? questions.size() : 0;
        }

        int total = 0;
        Set<Integer> fixedQuestionIds = new HashSet<>();
        for (QuizTemplateBankSource source : bankSources) {
            if (source.getSelectionMode() == QuizSourceSelectionMode.RANDOM) {
                total += source.getQuestionCount() != null ? source.getQuestionCount() : 0;
                continue;
            }

            if (source.getSelectionMode() == QuizSourceSelectionMode.ALL_MATCHED) {
                List<BankQuestion> matched = resolveTemplateMatchedQuestions(source);
                for (BankQuestion question : matched) {
                    if (fixedQuestionIds.add(question.getId())) {
                        total++;
                    }
                }
            }
        }
        return total;
    }

    private QuizQuestionResponse convertTemplateQuestionToDTO(QuizTemplateQuestion question) {
        QuizQuestionResponse response = new QuizQuestionResponse();
        response.setId(question.getId());
        response.setSourceBankQuestionId(question.getSourceBankQuestion() != null ? question.getSourceBankQuestion().getId() : null);
        response.setContent(question.getContent());
        response.setType(question.getType());
        response.setResource(convertResourceToDTO(question.getResource()));
        response.setPoints(question.getPoints());
        response.setAnswers(
                question.getAnswers() != null
                        ? question.getAnswers().stream().map(this::convertTemplateAnswerToDTO).toList()
                        : List.of()
        );
        response.setItems(
                question.getItems() != null
                        ? question.getItems().stream().map(item -> convertTemplateItemToDTO(item, true)).toList()
                        : List.of()
        );
        return response;
    }

    private QuizAnswerResponse convertTemplateAnswerToDTO(QuizTemplateAnswer answer) {
        QuizAnswerResponse response = new QuizAnswerResponse();
        response.setId(answer.getId());
        response.setIsCorrect(answer.getIsCorrect());
        response.setContent(answer.getContent());
        response.setExplanation(answer.getExplanation());
        response.setResourceId(answer.getResource() != null ? answer.getResource().getId() : null);
        response.setResource(convertResourceToDTO(answer.getResource()));
        return response;
    }

    private QuestionInteractionItemResponse convertTemplateItemToDTO(
            QuizTemplateQuestionItem item,
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
                convertResourceToDTO(item.getResource()),
                item.getOrderIndex()
        );
    }

    private QuizBankSourceResponse convertTemplateBankSourceToDTO(QuizTemplateBankSource source) {
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

    private void validateQuizStructureRequest(QuizTemplateRequest request) {
        boolean hasBankSources = request.getBankSources() != null && !request.getBankSources().isEmpty();
        boolean hasManualQuestions = request.getQuestions() != null && !request.getQuestions().isEmpty();
        if (hasBankSources && hasManualQuestions) {
            throw new BusinessException("Quiz template can be configured either by manual questions or by question bank sources, not both");
        }
        if (Boolean.TRUE.equals(request.getGenerateQuestionsPerAttempt()) && !hasBankSources) {
            throw new BusinessException("bankSources is required when generateQuestionsPerAttempt is enabled");
        }
        if (hasBankSources) {
            for (QuizBankSourceRequest sourceRequest : request.getBankSources()) {
                if (sourceRequest.getSelectionMode() == QuizSourceSelectionMode.MANUAL) {
                    throw new BusinessException("Manual selection in question bank mode is not supported. Use Questions Library for manual questions.");
                }
            }
        }
    }

    private List<BankQuestion> resolveTemplateMatchedQuestions(QuizTemplateBankSource source) {
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

    private List<QuizQuestionResponse> buildTemplatePreviewQuestions(QuizTemplate quizTemplate, Random random) {
        List<QuizTemplateBankSource> bankSources =
                quizTemplateBankSourceRepository.findByQuizTemplate_IdOrderByOrderIndexAsc(quizTemplate.getId());
        if (!bankSources.isEmpty()) {
            return buildTemplatePreviewQuestionsFromBankSources(quizTemplate, bankSources, random);
        }
        return buildTemplatePreviewQuestionsFromManual(quizTemplate, random);
    }

    private List<QuizQuestionResponse> buildTemplatePreviewQuestionsFromManual(QuizTemplate quizTemplate, Random random) {
        List<QuizTemplateQuestion> questions =
                quizTemplateQuestionRepository.findByQuizTemplate_IdOrderByOrderIndexAscIdAsc(quizTemplate.getId());
        List<QuizQuestionResponse> responses = questions.stream()
                .map(this::convertTemplateQuestionToDTO)
                .collect(Collectors.toCollection(ArrayList::new));

        if (quizTemplate.isShuffleAnswers()) {
            for (QuizQuestionResponse question : responses) {
                if (question.getAnswers() != null && question.getAnswers().size() > 1) {
                    Collections.shuffle(question.getAnswers(), random);
                }
            }
        }
        if (quizTemplate.isShuffleQuestions()) {
            Collections.shuffle(responses, random);
        }
        return responses;
    }

    private List<QuizQuestionResponse> buildTemplatePreviewQuestionsFromBankSources(
            QuizTemplate quizTemplate,
            List<QuizTemplateBankSource> bankSources,
            Random random
    ) {
        LinkedHashMap<Integer, BankQuestion> selectedQuestions = new LinkedHashMap<>();
        HashSet<Integer> excludedIds = new HashSet<>();

        for (QuizTemplateBankSource source : bankSources) {
            List<BankQuestion> selectedForSource = selectTemplatePreviewQuestionsForSource(source, random, excludedIds);
            for (BankQuestion question : selectedForSource) {
                if (selectedQuestions.putIfAbsent(question.getId(), question) == null) {
                    excludedIds.add(question.getId());
                }
            }
        }

        List<QuizQuestionResponse> responses = selectedQuestions.values().stream()
                .map(this::convertBankQuestionToTemplatePreviewDTO)
                .collect(Collectors.toCollection(ArrayList::new));

        if (quizTemplate.isShuffleAnswers()) {
            for (QuizQuestionResponse question : responses) {
                if (question.getAnswers() != null && question.getAnswers().size() > 1) {
                    Collections.shuffle(question.getAnswers(), random);
                }
            }
        }
        if (quizTemplate.isShuffleQuestions()) {
            Collections.shuffle(responses, random);
        }
        return responses;
    }

    private List<BankQuestion> selectTemplatePreviewQuestionsForSource(
            QuizTemplateBankSource source,
            Random random,
            Set<Integer> excludedQuestionIds
    ) {
        List<BankQuestion> matchedQuestions = resolveTemplateMatchedQuestions(source);
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

        throw new BusinessException("Unsupported question bank selection mode");
    }

    private QuizQuestionResponse convertBankQuestionToTemplatePreviewDTO(BankQuestion question) {
        QuizQuestionResponse response = new QuizQuestionResponse();
        response.setId(question.getId());
        response.setSourceBankQuestionId(question.getId());
        response.setContent(question.getContent());
        response.setType(question.getType());
        response.setResource(convertResourceToDTO(question.getResource()));
        response.setPoints(question.getDefaultPoints() != null ? question.getDefaultPoints() : BigDecimal.ONE);
        response.setAnswers(question.getOptions() != null
                ? question.getOptions().stream().map(this::convertBankTemplateAnswerToDTO).collect(Collectors.toCollection(ArrayList::new))
                : new ArrayList<>());
        response.setItems(question.getInteractionItems() != null
                ? question.getInteractionItems().stream().map(this::convertBankTemplateItemToDTO).toList()
                : List.of());
        return response;
    }

    private QuizAnswerResponse convertBankTemplateAnswerToDTO(BankQuestionOption answer) {
        QuizAnswerResponse response = new QuizAnswerResponse();
        response.setId(answer.getId());
        response.setIsCorrect(answer.getIsCorrect());
        response.setContent(answer.getContent());
        response.setExplanation(answer.getExplanation());
        response.setResourceId(answer.getResource() != null ? answer.getResource().getId() : null);
        response.setResource(convertResourceToDTO(answer.getResource()));
        return response;
    }

    private QuestionInteractionItemResponse convertBankTemplateItemToDTO(QuestionInteractionItem item) {
        return new QuestionInteractionItemResponse(
                item.getId(),
                item.getContent(),
                item.getItemKey(),
                item.getRole(),
                item.getCorrectMatchKey(),
                item.getCorrectOrderIndex(),
                item.getBlankIndex(),
                parseAcceptedAnswers(item.getAcceptedAnswers()),
                item.getBlankType(),
                item.getBlankOptions(),
                item.getResource() != null ? item.getResource().getId() : null,
                convertResourceToDTO(item.getResource()),
                item.getOrderIndex()
        );
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

    private List<com.example.backend.entity.quiz.QuestionTag> resolveTags(List<Integer> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        List<com.example.backend.entity.quiz.QuestionTag> tags = questionTagRepository.findAllById(tagIds);
        if (tags.size() != tagIds.size()) {
            throw new ResourceNotFoundException("One or more question tags not found");
        }
        return tags;
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
        return isMatchingQuestionType(type) || type == QuestionType.DRAG_ORDER || type == QuestionType.CLOZE;
    }

    private boolean isMatchingQuestionType(QuestionType type) {
        return type == QuestionType.MATCHING || type == QuestionType.IMAGE_MATCHING;
    }

    private boolean hasContentOrResource(QuestionInteractionItemRequest item) {
        return item != null && (StringUtils.hasText(item.getContent()) || item.getResourceId() != null);
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

    private List<Integer> toTagIds(List<com.example.backend.entity.quiz.QuestionTag> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        return tags.stream()
                .filter(Objects::nonNull)
                .map(com.example.backend.entity.quiz.QuestionTag::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private ResourceResponse convertResourceToDTO(Resource resource) {
        if (resource == null) {
            return null;
        }
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
}
