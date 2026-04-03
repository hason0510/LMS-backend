package com.example.backend.service.impl;

import com.example.backend.constant.ContentItemType;
import com.example.backend.constant.CourseStatus;
import com.example.backend.constant.ItemType;
import com.example.backend.dto.request.classsection.ClassSectionRequest;
import com.example.backend.dto.request.classsection.LegacyCourseMigrationRequest;
import com.example.backend.dto.response.classsection.ClassChapterResponse;
import com.example.backend.dto.response.classsection.ClassContentItemResponse;
import com.example.backend.dto.response.classsection.ClassSectionResponse;
import com.example.backend.entity.*;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.*;
import com.example.backend.service.ClassSectionService;
import com.example.backend.service.CurriculumVersionService;
import com.example.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassSectionServiceImpl implements ClassSectionService {
    private static final String ALPHANUMERIC_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final ClassSectionRepository classSectionRepository;
    private final ClassChapterRepository classChapterRepository;
    private final ClassContentItemRepository classContentItemRepository;
    private final CurriculumVersionRepository curriculumVersionRepository;
    private final SubjectRepository subjectRepository;
    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final ChapterItemRepository chapterItemRepository;
    private final LessonRepository lessonRepository;
    private final QuizRepository quizRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final CurriculumVersionService curriculumVersionService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public ClassSectionResponse createFromTemplate(Integer curriculumVersionId, ClassSectionRequest request) {
        CurriculumVersion curriculumVersion = curriculumVersionRepository.findById(curriculumVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("Curriculum version not found"));

        User currentUser = userService.getCurrentUser();
        User teacher = resolveUser(request.getTeacherId(), currentUser);
        User manager = resolveUser(request.getManagerId(), teacher);

        ClassSection classSection = new ClassSection();
        classSection.setClassCode(resolveClassCode(request.getClassCode()));
        classSection.setTitle(request.getTitle());
        classSection.setDescription(request.getDescription());
        classSection.setStatus(resolveStatus(request.getStatus()));
        classSection.setStartDate(request.getStartDate());
        classSection.setEndDate(request.getEndDate());
        classSection.setTeacher(teacher);
        classSection.setManager(manager);
        classSection.setSubject(curriculumVersion.getTemplate().getSubject());
        classSection.setCurriculumVersion(curriculumVersion);

        classSection = classSectionRepository.save(classSection);

        for (ChapterTemplate chapterTemplate : safeList(curriculumVersion.getChapters())) {
            ClassChapter classChapter = new ClassChapter();
            classChapter.setClassSection(classSection);
            classChapter.setChapterTemplate(chapterTemplate);
            classChapter.setOrderIndex(chapterTemplate.getOrderIndex());
            classChapter.setIsHidden(false);
            classChapter = classChapterRepository.save(classChapter);

            for (ContentItemTemplate templateItem : safeList(chapterTemplate.getContentItems())) {
                ClassContentItem classContentItem = new ClassContentItem();
                classContentItem.setClassChapter(classChapter);
                classContentItem.setContentItemTemplate(templateItem);
                classContentItem.setItemType(templateItem.getItemType());
                classContentItem.setOrderIndex(templateItem.getOrderIndex());
                classContentItem.setIsHidden(false);
                classContentItemRepository.save(classContentItem);
            }
        }

        return getClassSectionById(classSection.getId());
    }

    @Override
    @Transactional
    public ClassSectionResponse createFromLatestPublishedVersion(Integer templateId, ClassSectionRequest request) {
        Integer publishedVersionId = curriculumVersionService.getLatestPublishedVersion(templateId).getId();
        return createFromTemplate(publishedVersionId, request);
    }

    @Override
    @Transactional
    public ClassSectionResponse migrateFromLegacyCourse(Integer legacyCourseId, LegacyCourseMigrationRequest request) {
        Course legacyCourse = courseRepository.findById(legacyCourseId)
                .orElseThrow(() -> new ResourceNotFoundException("Legacy course not found"));

        if (classSectionRepository.findByLegacyCourse_Id(legacyCourseId).isPresent()) {
            throw new BusinessException("Legacy course already migrated to a class section");
        }

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        User currentUser = userService.getCurrentUser();
        User teacher = resolveUser(
                request.getTeacherId(),
                legacyCourse.getTeacher() != null ? legacyCourse.getTeacher() : currentUser
        );
        User manager = resolveUser(request.getManagerId(), teacher);

        ClassSection classSection = new ClassSection();
        classSection.setClassCode(resolveClassCode(StringUtils.hasText(request.getClassCode())
                ? request.getClassCode()
                : legacyCourse.getClassCode()));
        classSection.setTitle(StringUtils.hasText(request.getTitle()) ? request.getTitle() : legacyCourse.getTitle());
        classSection.setDescription(StringUtils.hasText(request.getDescription())
                ? request.getDescription()
                : legacyCourse.getDescription());
        classSection.setStatus(resolveStatus(request.getStatus() != null ? request.getStatus() : legacyCourse.getStatus()));
        classSection.setStartDate(request.getStartDate());
        classSection.setEndDate(request.getEndDate());
        classSection.setTeacher(teacher);
        classSection.setManager(manager);
        classSection.setSubject(subject);
        classSection.setLegacyCourse(legacyCourse);

        classSection = classSectionRepository.save(classSection);

        List<Chapter> legacyChapters = chapterRepository.findByCourse_IdOrderByOrderIndexAsc(legacyCourseId);
        for (Chapter legacyChapter : legacyChapters) {
            ClassChapter classChapter = new ClassChapter();
            classChapter.setClassSection(classSection);
            classChapter.setTitleOverride(legacyChapter.getTitle());
            classChapter.setDescriptionOverride(legacyChapter.getDescription());
            classChapter.setOrderIndex(legacyChapter.getOrderIndex());
            classChapter.setIsHidden(false);
            classChapter = classChapterRepository.save(classChapter);

            List<ChapterItem> legacyItems = chapterItemRepository.findByChapter_IdOrderByOrderIndexAsc(legacyChapter.getId());
            for (ChapterItem legacyItem : legacyItems) {
                ClassContentItem classContentItem = new ClassContentItem();
                classContentItem.setClassChapter(classChapter);
                classContentItem.setItemType(mapLegacyItemType(legacyItem.getType()));
                classContentItem.setOrderIndex(legacyItem.getOrderIndex());
                classContentItem.setIsHidden(false);

                if (legacyItem.getType() == ItemType.LESSON) {
                    Lesson lesson = lessonRepository.findById(legacyItem.getRefId())
                            .orElseThrow(() -> new ResourceNotFoundException("Legacy lesson not found: " + legacyItem.getRefId()));
                    classContentItem.setTitleOverride(lesson.getTitle());
                    classContentItem.setOverrideLesson(lesson);
                } else if (legacyItem.getType() == ItemType.QUIZ) {
                    Quiz quiz = quizRepository.findById(legacyItem.getRefId())
                            .orElseThrow(() -> new ResourceNotFoundException("Legacy quiz not found: " + legacyItem.getRefId()));
                    classContentItem.setTitleOverride(quiz.getTitle());
                    classContentItem.setOverrideQuiz(quiz);
                } else {
                    throw new BusinessException("Unsupported legacy item type: " + legacyItem.getType());
                }

                classContentItemRepository.save(classContentItem);
            }
        }

        return getClassSectionById(classSection.getId());
    }

    @Override
    public ClassSectionResponse getClassSectionById(Integer id) {
        ClassSection classSection = classSectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        return convertToResponse(classSection, true);
    }

    @Override
    public ClassSectionResponse getClassSectionByLegacyCourseId(Integer legacyCourseId) {
        ClassSection classSection = classSectionRepository.findByLegacyCourse_Id(legacyCourseId)
                .orElseThrow(() -> new ResourceNotFoundException("Migrated class section not found for legacy course"));
        return convertToResponse(classSection, true);
    }

    @Override
    public List<ClassSectionResponse> getClassSections(
            Integer teacherId,
            Integer subjectId,
            Integer curriculumVersionId,
            boolean includeChapters
    ) {
        List<ClassSection> classSections;
        if (curriculumVersionId != null) {
            classSections = classSectionRepository.findByCurriculumVersion_Id(curriculumVersionId);
        } else if (teacherId != null) {
            classSections = classSectionRepository.findByTeacher_Id(teacherId);
        } else if (subjectId != null) {
            classSections = classSectionRepository.findBySubject_Id(subjectId);
        } else {
            classSections = classSectionRepository.findAll();
        }

        return classSections.stream()
                .map(classSection -> convertToResponse(classSection, includeChapters))
                .toList();
    }

    private ClassSectionResponse convertToResponse(ClassSection classSection, boolean includeChapters) {
        ClassSectionResponse response = new ClassSectionResponse();
        response.setId(classSection.getId());
        response.setClassCode(classSection.getClassCode());
        response.setTitle(classSection.getTitle());
        response.setDescription(classSection.getDescription());
        response.setStatus(classSection.getStatus());
        response.setStartDate(classSection.getStartDate());
        response.setEndDate(classSection.getEndDate());
        response.setSubjectId(classSection.getSubject() != null ? classSection.getSubject().getId() : null);
        response.setSubjectTitle(classSection.getSubject() != null ? classSection.getSubject().getTitle() : null);
        response.setTeacherId(classSection.getTeacher() != null ? classSection.getTeacher().getId() : null);
        response.setTeacherName(classSection.getTeacher() != null ? classSection.getTeacher().getFullName() : null);
        response.setManagerId(classSection.getManager() != null ? classSection.getManager().getId() : null);
        response.setManagerName(classSection.getManager() != null ? classSection.getManager().getFullName() : null);
        response.setCurriculumVersionId(classSection.getCurriculumVersion() != null ? classSection.getCurriculumVersion().getId() : null);
        response.setLegacyCourseId(classSection.getLegacyCourse() != null ? classSection.getLegacyCourse().getId() : null);
        response.setTemplateBased(classSection.getCurriculumVersion() != null);
        response.setMigratedFromLegacyCourse(classSection.getLegacyCourse() != null);
        response.setChapters(includeChapters
                ? classChapterRepository.findByClassSection_IdOrderByOrderIndexAsc(classSection.getId()).stream()
                .map(this::convertChapterToResponse)
                .toList()
                : null);
        return response;
    }

    private ClassChapterResponse convertChapterToResponse(ClassChapter classChapter) {
        List<ClassContentItem> contentItems = classContentItemRepository.findByClassChapter_IdOrderByOrderIndexAsc(classChapter.getId());

        ClassChapterResponse response = new ClassChapterResponse();
        response.setId(classChapter.getId());
        response.setChapterTemplateId(classChapter.getChapterTemplate() != null ? classChapter.getChapterTemplate().getId() : null);
        response.setTitle(resolveChapterTitle(classChapter));
        response.setDescription(resolveChapterDescription(classChapter));
        response.setOrderIndex(classChapter.getOrderIndex());
        response.setHidden(classChapter.getIsHidden());
        response.setContentItems(contentItems.stream().map(this::convertContentItemToResponse).toList());
        return response;
    }

    private ClassContentItemResponse convertContentItemToResponse(ClassContentItem classContentItem) {
        ClassContentItemResponse response = new ClassContentItemResponse();
        response.setId(classContentItem.getId());
        response.setClassChapterId(classContentItem.getClassChapter() != null ? classContentItem.getClassChapter().getId() : null);
        response.setContentItemTemplateId(classContentItem.getContentItemTemplate() != null ? classContentItem.getContentItemTemplate().getId() : null);
        response.setItemType(classContentItem.getItemType());
        response.setOrderIndex(classContentItem.getOrderIndex());
        response.setHidden(classContentItem.getIsHidden());
        response.setOverridden(hasOverrides(classContentItem));
        response.setTitle(resolveContentItemTitle(classContentItem));
        response.setLessonId(resolveLessonId(classContentItem));
        response.setQuizId(resolveQuizId(classContentItem));
        response.setAssignmentId(resolveAssignmentId(classContentItem));
        return response;
    }

    private String resolveChapterTitle(ClassChapter classChapter) {
        if (StringUtils.hasText(classChapter.getTitleOverride())) {
            return classChapter.getTitleOverride();
        }
        return classChapter.getChapterTemplate() != null ? classChapter.getChapterTemplate().getTitle() : null;
    }

    private String resolveChapterDescription(ClassChapter classChapter) {
        if (StringUtils.hasText(classChapter.getDescriptionOverride())) {
            return classChapter.getDescriptionOverride();
        }
        return classChapter.getChapterTemplate() != null ? classChapter.getChapterTemplate().getDescription() : null;
    }

    private String resolveContentItemTitle(ClassContentItem classContentItem) {
        if (StringUtils.hasText(classContentItem.getTitleOverride())) {
            return classContentItem.getTitleOverride();
        }
        if (classContentItem.getOverrideLesson() != null) {
            return classContentItem.getOverrideLesson().getTitle();
        }
        if (classContentItem.getOverrideQuiz() != null) {
            return classContentItem.getOverrideQuiz().getTitle();
        }
        if (classContentItem.getOverrideAssignment() != null) {
            return classContentItem.getOverrideAssignment().getTitle();
        }
        ContentItemTemplate templateItem = classContentItem.getContentItemTemplate();
        if (templateItem == null) {
            return null;
        }
        if (templateItem.getLesson() != null) {
            return templateItem.getLesson().getTitle();
        }
        if (templateItem.getQuiz() != null) {
            return templateItem.getQuiz().getTitle();
        }
        if (templateItem.getAssignment() != null) {
            return templateItem.getAssignment().getTitle();
        }
        return null;
    }

    private Integer resolveLessonId(ClassContentItem classContentItem) {
        if (classContentItem.getOverrideLesson() != null) {
            return classContentItem.getOverrideLesson().getId();
        }
        return classContentItem.getContentItemTemplate() != null && classContentItem.getContentItemTemplate().getLesson() != null
                ? classContentItem.getContentItemTemplate().getLesson().getId()
                : null;
    }

    private Integer resolveQuizId(ClassContentItem classContentItem) {
        if (classContentItem.getOverrideQuiz() != null) {
            return classContentItem.getOverrideQuiz().getId();
        }
        return classContentItem.getContentItemTemplate() != null && classContentItem.getContentItemTemplate().getQuiz() != null
                ? classContentItem.getContentItemTemplate().getQuiz().getId()
                : null;
    }

    private Integer resolveAssignmentId(ClassContentItem classContentItem) {
        if (classContentItem.getOverrideAssignment() != null) {
            return classContentItem.getOverrideAssignment().getId();
        }
        return classContentItem.getContentItemTemplate() != null && classContentItem.getContentItemTemplate().getAssignment() != null
                ? classContentItem.getContentItemTemplate().getAssignment().getId()
                : null;
    }

    private boolean hasOverrides(ClassContentItem classContentItem) {
        return StringUtils.hasText(classContentItem.getTitleOverride())
                || classContentItem.getOverrideLesson() != null
                || classContentItem.getOverrideQuiz() != null
                || classContentItem.getOverrideAssignment() != null;
    }

    private ContentItemType mapLegacyItemType(ItemType itemType) {
        if (itemType == ItemType.LESSON) {
            return ContentItemType.LESSON;
        }
        if (itemType == ItemType.QUIZ) {
            return ContentItemType.QUIZ;
        }
        throw new BusinessException("Unsupported legacy item type: " + itemType);
    }

    private User resolveUser(Integer userId, User fallbackUser) {
        if (userId != null) {
            return userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }
        if (fallbackUser != null) {
            return fallbackUser;
        }
        throw new ResourceNotFoundException("User not found");
    }

    private CourseStatus resolveStatus(CourseStatus status) {
        return status != null ? status : CourseStatus.PRIVATE;
    }

    private String resolveClassCode(String requestedClassCode) {
        if (StringUtils.hasText(requestedClassCode)) {
            if (classSectionRepository.existsByClassCode(requestedClassCode)) {
                throw new BusinessException("Class code already exists in class_sections");
            }
            return requestedClassCode;
        }

        String code;
        do {
            StringBuilder builder = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                int randomIndex = secureRandom.nextInt(ALPHANUMERIC_CHARS.length());
                builder.append(ALPHANUMERIC_CHARS.charAt(randomIndex));
            }
            code = builder.toString();
        } while (classSectionRepository.existsByClassCode(code));

        return code;
    }

    private <T> List<T> safeList(List<T> values) {
        return values != null ? values : new ArrayList<>();
    }
}
