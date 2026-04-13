package com.example.backend.service.impl;

import com.example.backend.constant.*;
import com.example.backend.dto.request.classsection.ClassChapterCreateRequest;
import com.example.backend.dto.request.classsection.ClassChapterOverrideRequest;
import com.example.backend.dto.request.classsection.ClassContentItemCreateRequest;
import com.example.backend.dto.request.classsection.ClassContentItemOverrideRequest;
import com.example.backend.dto.request.classsection.ClassMemberRequest;
import com.example.backend.dto.request.classsection.ClassMemberRoleRequest;
import com.example.backend.dto.request.classsection.ClassSectionRequest;
import com.example.backend.dto.request.classsection.ClassSectionUpdateRequest;
import com.example.backend.dto.response.classsection.ClassChapterResponse;
import com.example.backend.dto.response.classsection.ClassContentItemResponse;
import com.example.backend.dto.response.classsection.ClassMemberResponse;
import com.example.backend.dto.response.classsection.ClassSectionResponse;
import com.example.backend.entity.assignment.Assignment;
import com.example.backend.entity.template.AssignmentTemplate;
import com.example.backend.entity.template.ChapterTemplate;
import com.example.backend.entity.ClassChapter;
import com.example.backend.entity.ClassContentItem;
import com.example.backend.entity.ClassMember;
import com.example.backend.entity.ClassSection;
import com.example.backend.entity.template.ContentItemTemplate;
import com.example.backend.entity.template.CurriculumTemplate;
import com.example.backend.entity.Enrollment;
import com.example.backend.entity.Lesson;
import com.example.backend.entity.template.LessonTemplate;
import com.example.backend.entity.quiz.Quiz;
import com.example.backend.entity.template.QuizTemplate;
import com.example.backend.entity.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.repository.AssignmentRepository;
import com.example.backend.repository.AssignmentTemplateRepository;
import com.example.backend.repository.ChapterTemplateRepository;
import com.example.backend.repository.ClassChapterRepository;
import com.example.backend.repository.ClassContentItemRepository;
import com.example.backend.repository.ClassMemberRepository;
import com.example.backend.repository.ClassSectionRepository;
import com.example.backend.repository.ContentItemTemplateRepository;
import com.example.backend.repository.CurriculumTemplateRepository;
import com.example.backend.repository.EnrollmentRepository;
import com.example.backend.repository.LessonRepository;
import com.example.backend.repository.LessonTemplateRepository;
import com.example.backend.repository.QuizRepository;
import com.example.backend.repository.QuizTemplateRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.ClassMemberAuthorizationService;
import com.example.backend.service.ClassSectionService;
import com.example.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ClassSectionServiceImpl implements ClassSectionService {
    private static final String ALPHANUMERIC_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final ClassSectionRepository classSectionRepository;
    private final ClassChapterRepository classChapterRepository;
    private final ClassContentItemRepository classContentItemRepository;
    private final ClassMemberRepository classMemberRepository;
    private final ChapterTemplateRepository chapterTemplateRepository;
    private final ContentItemTemplateRepository contentItemTemplateRepository;
    private final CurriculumTemplateRepository curriculumTemplateRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final LessonTemplateRepository lessonTemplateRepository;
    private final QuizRepository quizRepository;
    private final QuizTemplateRepository quizTemplateRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentTemplateRepository assignmentTemplateRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final ClassMemberAuthorizationService classMemberAuthorizationService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public ClassSectionResponse createFromTemplate(Integer curriculumTemplateId, ClassSectionRequest request) {
        CurriculumTemplate curriculumTemplate = curriculumTemplateRepository.findById(curriculumTemplateId)
                .orElseThrow(() -> new ResourceNotFoundException("Curriculum template not found"));

        User currentUser = userService.getCurrentUser();
        User teacher = resolveUser(request.getTeacherId(), currentUser);

        ClassSection classSection = new ClassSection();
        classSection.setClassCode(StringUtils.hasText(request.getClassCode())
                ? request.getClassCode()
                : resolveClassCode());
        classSection.setTitle(request.getTitle());
        classSection.setDescription(request.getDescription());
        classSection.setStatus(resolveStatus(request.getStatus()));
        classSection.setStartDate(request.getStartDate());
        classSection.setEndDate(request.getEndDate());
        classSection.setTeacher(teacher);
        classSection.setSubject(curriculumTemplate.getSubject());
        classSection.setCurriculumTemplate(curriculumTemplate);

        classSection = classSectionRepository.save(classSection);
        syncTeachingMembers(classSection, teacher, request.getTaIds());

        for (ChapterTemplate chapterTemplate : safeList(curriculumTemplate.getChapters())) {
            ClassChapter classChapter = new ClassChapter();
            classChapter.setClassSection(classSection);
            classChapter.setTitle(chapterTemplate.getTitle());
            classChapter.setDescription(chapterTemplate.getDescription());
            classChapter.setOrderIndex(chapterTemplate.getOrderIndex());
            classChapter.setIsHidden(false);
            classChapter.setIsLocked(false);
            classChapter = classChapterRepository.save(classChapter);

            for (ContentItemTemplate templateItem : safeList(chapterTemplate.getContentItems())) {
                ClassContentItem classContentItem = new ClassContentItem();
                classContentItem.setClassChapter(classChapter);
                classContentItem.setItemType(templateItem.getItemType());
                classContentItem.setOrderIndex(templateItem.getOrderIndex());
                classContentItem.setIsHidden(false);
                classContentItem.setIsLocked(false);
                applyTemplateSnapshot(classContentItem, classSection, templateItem);
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
    public List<ClassSectionResponse> getClassSections(
            Integer teacherId,
            Integer subjectId,
            Integer curriculumTemplateId,
            boolean includeChapters
    ) {
        List<ClassSection> classSections;
        if (curriculumTemplateId != null) {
            classSections = classSectionRepository.findByCurriculumTemplate_Id(curriculumTemplateId);
        } else if (teacherId != null) {
            Map<Integer, ClassSection> merged = new LinkedHashMap<>();
            for (ClassSection section : classMemberRepository.findClassSectionsByMemberRole(teacherId, ClassMemberRole.TEACHER)) {
                merged.put(section.getId(), section);
            }
            for (ClassSection section : classSectionRepository.findByTeacher_Id(teacherId)) {
                merged.putIfAbsent(section.getId(), section);
            }
            classSections = new ArrayList<>(merged.values());
        } else if (subjectId != null) {
            classSections = classSectionRepository.findBySubject_Id(subjectId);
        } else {
            classSections = classSectionRepository.findAll();
        }

        return classSections.stream()
                .map(classSection -> convertToResponse(classSection, includeChapters))
                .toList();
    }

    @Override
    @Transactional
    public ClassMemberResponse addMember(Integer classSectionId, ClassMemberRequest request) {
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        requireTeacherPermission(classSection);

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getRole() == ClassMemberRole.TEACHER) {
            transferTeacherOwnership(classSection, user);
            return classMemberRepository.findByClassSection_IdAndUser_Id(classSectionId, user.getId())
                    .map(this::convertClassMember)
                    .orElseThrow(() -> new ResourceNotFoundException("Class member not found"));
        }

        ClassMember saved = createOrUpdateMembership(classSection, user, ClassMemberRole.TA);
        return convertClassMember(saved);
    }

    @Override
    @Transactional
    public ClassMemberResponse updateMemberRole(Integer classSectionId, Integer userId, ClassMemberRoleRequest request) {
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        requireTeacherPermission(classSection);

        ClassMember member = classMemberRepository.findByClassSection_IdAndUser_Id(classSectionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Class member not found"));

        if (request.getRole() == ClassMemberRole.TEACHER) {
            transferTeacherOwnership(classSection, member.getUser());
            return classMemberRepository.findByClassSection_IdAndUser_Id(classSectionId, userId)
                    .map(this::convertClassMember)
                    .orElseThrow(() -> new ResourceNotFoundException("Class member not found"));
        }

        if (member.getRole() == ClassMemberRole.TEACHER) {
            throw new BusinessException("Cannot downgrade TEACHER directly. Transfer teacher role first.");
        }
        member.setRole(ClassMemberRole.TA);
        return convertClassMember(classMemberRepository.save(member));
    }

    @Override
    @Transactional
    public void removeMember(Integer classSectionId, Integer userId) {
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        requireTeacherPermission(classSection);

        ClassMember member = classMemberRepository.findByClassSection_IdAndUser_Id(classSectionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Class member not found"));
        if (member.getRole() == ClassMemberRole.TEACHER) {
            throw new BusinessException("Cannot remove TEACHER from class section");
        }
        classMemberRepository.delete(member);
    }

    @Override
    public List<ClassMemberResponse> getMembers(Integer classSectionId) {
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        requireTeacherPermission(classSection);
        return getTeachingMembers(classSection);
    }

    @Override
    @Transactional
    public ClassChapterResponse createClassChapter(Integer classSectionId, ClassChapterCreateRequest request) {
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        requireTeacherPermission(classSection);
        ClassChapter classChapter = new ClassChapter();
        classChapter.setClassSection(classSection);
        classChapter.setTitle(StringUtils.hasText(request.getTitle()) ? request.getTitle() : "Chương mới");
        classChapter.setDescription(normalizeNullableText(request.getDescription()));
        classChapter.setOrderIndex(resolveClassChapterOrderIndex(classSectionId, request.getOrderIndex()));
        classChapter.setIsHidden(Boolean.TRUE.equals(request.getHidden()));
        classChapter.setIsLocked(false);

        ClassChapter created = classChapterRepository.save(classChapter);
        return convertChapterToResponse(created);
    }

    @Override
    @Transactional
    public ClassChapterResponse updateClassChapter(
            Integer classSectionId,
            Integer classChapterId,
            ClassChapterOverrideRequest request
    ) {
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        requireTeacherPermission(classSection);

        ClassChapter classChapter = classChapterRepository.findByIdAndClassSection_Id(classChapterId, classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class chapter not found in this class section"));
        if (request.getTitle() != null) {
            classChapter.setTitle(normalizeNullableText(request.getTitle()));
        }
        if (request.getDescription() != null) {
            classChapter.setDescription(normalizeNullableText(request.getDescription()));
        }

        if (request.getOrderIndex() != null) {
            classChapter.setOrderIndex(request.getOrderIndex());
        }
        if (request.getHidden() != null) {
            classChapter.setIsHidden(request.getHidden());
        }
        if (request.getLocked() != null) {
            classChapter.setIsLocked(request.getLocked());
        }
        if (request.getAvailableFrom() != null) {
            classChapter.setAvailableFrom(request.getAvailableFrom());
        }
        if (request.getAvailableTo() != null) {
            classChapter.setAvailableTo(request.getAvailableTo());
        }

        if (!StringUtils.hasText(classChapter.getTitle())) {
            throw new BusinessException("Class chapter must have a title");
        }

        return convertChapterToResponse(classChapterRepository.save(classChapter));
    }

    @Override
    @Transactional
    public void deleteClassChapter(Integer classSectionId, Integer classChapterId) {
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        requireTeacherPermission(classSection);

        ClassChapter classChapter = classChapterRepository.findByIdAndClassSection_Id(classChapterId, classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class chapter not found in this class section"));
        classChapterRepository.delete(classChapter);
    }

    @Override
    @Transactional
    public ClassContentItemResponse createClassContentItem(
            Integer classSectionId,
            Integer classChapterId,
            ClassContentItemCreateRequest request
    ) {
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        requireTeacherPermission(classSection);

        ClassChapter classChapter = classChapterRepository.findByIdAndClassSection_Id(classChapterId, classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class chapter not found in this class section"));

        ClassContentItem classContentItem = new ClassContentItem();
        classContentItem.setClassChapter(classChapter);
        classContentItem.setOrderIndex(resolveClassContentItemOrderIndex(classChapterId, request.getOrderIndex()));
        classContentItem.setIsHidden(Boolean.TRUE.equals(request.getHidden()));
        classContentItem.setIsLocked(Boolean.TRUE.equals(request.getLocked()));
        classContentItem.setAvailableFrom(request.getAvailableFrom());
        classContentItem.setAvailableTo(request.getAvailableTo());
        classContentItem.setTitle(normalizeNullableText(request.getTitle()));

       /* ContentItemTemplate contentItemTemplate = resolveContentItemTemplateForClassSection(
                classSection,
                request.getContentItemTemplateId()
        );*/
       /* if (contentItemTemplate != null) {
            classContentItem.setContentItemTemplate(contentItemTemplate);
            classContentItem.setItemType(contentItemTemplate.getItemType());
            applyTemplateSnapshot(classContentItem, classSection, contentItemTemplate);
        } else {*/
        if (request.getItemType() == null) {
            throw new BusinessException("itemType is required when contentItemTemplateId is not provided");
        }
        classContentItem.setItemType(request.getItemType());
        applyContentReferenceByIds(
                classContentItem,
                classSection,
                request.getLessonId(),
                request.getQuizId(),
                request.getAssignmentId(),
                true
        );


        return convertContentItemToResponse(classContentItemRepository.save(classContentItem));
    }

    @Override
    @Transactional
    public ClassContentItemResponse updateClassContentItem(
            Integer classSectionId,
            Integer classContentItemId,
            ClassContentItemOverrideRequest request
    ) {
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        requireTeacherPermission(classSection);

        ClassContentItem classContentItem = classContentItemRepository
                .findByIdAndClassChapter_ClassSection_Id(classContentItemId, classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class content item not found in this class section"));
        if (request.getTitle() != null) {
            classContentItem.setTitle(normalizeNullableText(request.getTitle()));
        }
        if (request.getLessonId() != null || request.getQuizId() != null || request.getAssignmentId() != null) {
            applyContentReferenceByIds(
                    classContentItem,
                    classSection,
                    request.getLessonId(),
                    request.getQuizId(),
                    request.getAssignmentId(),
                    false
            );
        }

        if (request.getOrderIndex() != null) {
            classContentItem.setOrderIndex(request.getOrderIndex());
        }
        if (request.getHidden() != null) {
            classContentItem.setIsHidden(request.getHidden());
        }
        if (request.getLocked() != null) {
            classContentItem.setIsLocked(request.getLocked());
        }
        if (request.getAvailableFrom() != null) {
            classContentItem.setAvailableFrom(request.getAvailableFrom());
        }
        if (request.getAvailableTo() != null) {
            classContentItem.setAvailableTo(request.getAvailableTo());
        }

        if (!StringUtils.hasText(classContentItem.getTitle())) {
            throw new BusinessException("Class content item must have a title");
        }

        return convertContentItemToResponse(classContentItemRepository.save(classContentItem));
    }

    @Override
    @Transactional
    public void deleteClassContentItem(Integer classSectionId, Integer classContentItemId) {
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        requireTeacherPermission(classSection);

        ClassContentItem classContentItem = classContentItemRepository
                .findByIdAndClassChapter_ClassSection_Id(classContentItemId, classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class content item not found in this class section"));
        classContentItemRepository.delete(classContentItem);
    }

    @Override
    public List<ClassSectionResponse> getApprovedClassSectionsForStudent() {
        User currentUser = userService.getCurrentUser();
        List<Enrollment> enrollments = enrollmentRepository.findByStudent_IdAndApprovalStatus(
                currentUser.getId(),
                EnrollmentStatus.APPROVED,
                org.springframework.data.domain.Pageable.unpaged()
        ).getContent();

        return enrollments.stream()
                .map(enrollment -> convertToResponse(enrollment.getClassSection(), false))
                .toList();
    }

    @Override
    public List<ClassSectionResponse> getPendingClassSectionsForStudent() {
        User currentUser = userService.getCurrentUser();
        List<Enrollment> enrollments = enrollmentRepository.findByStudent_IdAndApprovalStatus(
                currentUser.getId(),
                EnrollmentStatus.PENDING,
                org.springframework.data.domain.Pageable.unpaged()
        ).getContent();

        return enrollments.stream()
                .map(enrollment -> convertToResponse(enrollment.getClassSection(), false))
                .toList();
    }

    @Override
    public List<ClassSectionResponse> getAllClassSectionsForStudent() {
        User currentUser = userService.getCurrentUser();
        List<Enrollment> enrollments = enrollmentRepository.findAll().stream()
                .filter(e -> e.getStudent().getId().equals(currentUser.getId()))
                .toList();

        return enrollments.stream()
                .map(enrollment -> convertToResponse(enrollment.getClassSection(), false))
                .toList();
    }

    @Override
    public List<ClassChapterResponse> getClassChapters(Integer classSectionId) {
        return classChapterRepository.findByClassSection_IdOrderByOrderIndexAsc(classSectionId).stream()
                .map(this::convertChapterToResponse)
                .toList();
    }

    @Override
    public List<ClassContentItemResponse> getClassContentItems(Integer classSectionId, Integer classChapterId) {
        return classContentItemRepository.findByClassChapter_IdOrderByOrderIndexAsc(classChapterId).stream()
                .map(this::convertContentItemToResponse)
                .toList();
    }

    @Override
    @Transactional
    public ClassSectionResponse updateClassSectionStatus(Integer id, ClassSectionStatus status) {
        ClassSection classSection = classSectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        requireTeacherPermission(classSection);
        classSection.setStatus(status);
        return convertToResponse(classSectionRepository.save(classSection), false);
    }

    private ClassSectionResponse convertToResponse(ClassSection classSection, boolean includeChapters) {
        User primaryTeacher = classMemberAuthorizationService.resolvePrimaryTeacher(classSection);
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
        response.setTeacherId(primaryTeacher != null ? primaryTeacher.getId() : null);
        response.setTeacherName(primaryTeacher != null ? primaryTeacher.getFullName() : null);
        response.setTeachingMembers(getTeachingMembers(classSection));
        response.setCurriculumTemplateId(classSection.getCurriculumTemplate() != null ? classSection.getCurriculumTemplate().getId() : null);
        response.setTemplateBased(classSection.getCurriculumTemplate() != null);
        response.setChapters(includeChapters
                ? classChapterRepository.findByClassSection_IdOrderByOrderIndexAsc(classSection.getId()).stream()
                .map(this::convertChapterToResponse)
                .toList()
                : null);
        return response;
    }

    private List<ClassMemberResponse> getTeachingMembers(ClassSection classSection) {
        List<ClassMemberResponse> members = classMemberRepository.findByClassSection_Id(classSection.getId()).stream()
                .map(this::convertClassMember)
                .sorted((left, right) -> Integer.compare(roleRank(left.getRole()), roleRank(right.getRole())))
                .toList();

        if (!members.isEmpty()) {
            return members;
        }

        if (classSection.getTeacher() == null) {
            return List.of();
        }

        ClassMemberResponse fallbackTeacher = new ClassMemberResponse();
        fallbackTeacher.setId(null);
        fallbackTeacher.setUserId(classSection.getTeacher().getId());
        fallbackTeacher.setUsername(classSection.getTeacher().getUserName());
        fallbackTeacher.setFullName(classSection.getTeacher().getFullName());
        fallbackTeacher.setRole(ClassMemberRole.TEACHER);
        return List.of(fallbackTeacher);
    }

    private int roleRank(ClassMemberRole role) {
        return role == ClassMemberRole.TEACHER ? 1 : 2;
    }

    private ClassMemberResponse convertClassMember(ClassMember member) {
        ClassMemberResponse response = new ClassMemberResponse();
        response.setId(member.getId());
        response.setUserId(member.getUser() != null ? member.getUser().getId() : null);
        response.setUsername(member.getUser() != null ? member.getUser().getUserName() : null);
        response.setFullName(member.getUser() != null ? member.getUser().getFullName() : null);
        response.setRole(member.getRole());
        return response;
    }

    private ClassChapterResponse convertChapterToResponse(ClassChapter classChapter) {
        List<ClassContentItem> contentItems = classContentItemRepository.findByClassChapter_IdOrderByOrderIndexAsc(classChapter.getId());

        ClassChapterResponse response = new ClassChapterResponse();
        response.setId(classChapter.getId());
        response.setTitle(classChapter.getTitle());
        response.setDescription(classChapter.getDescription());
        response.setOrderIndex(classChapter.getOrderIndex());
        response.setHidden(classChapter.getIsHidden());
        response.setLocked(classChapter.getIsLocked());
        response.setAvailableFrom(classChapter.getAvailableFrom());
        response.setAvailableTo(classChapter.getAvailableTo());
        response.setContentItems(contentItems.stream().map(this::convertContentItemToResponse).toList());
        return response;
    }

    private ClassContentItemResponse convertContentItemToResponse(ClassContentItem classContentItem) {
        ClassContentItemResponse response = new ClassContentItemResponse();
        response.setId(classContentItem.getId());
        response.setClassChapterId(classContentItem.getClassChapter() != null ? classContentItem.getClassChapter().getId() : null);
        response.setItemType(classContentItem.getItemType());
        response.setTitle(classContentItem.getTitle());
        response.setOrderIndex(classContentItem.getOrderIndex());
        response.setHidden(classContentItem.getIsHidden());
        response.setLocked(classContentItem.getIsLocked());
        response.setAvailableFrom(classContentItem.getAvailableFrom());
        response.setAvailableTo(classContentItem.getAvailableTo());
        response.setLessonId(classContentItem.getLesson() != null ? classContentItem.getLesson().getId() : null);
        response.setQuizId(classContentItem.getQuiz() != null ? classContentItem.getQuiz().getId() : null);
        response.setAssignmentId(classContentItem.getAssignment() != null ? classContentItem.getAssignment().getId() : null);
        return response;
    }

    private void applyContentReferenceByIds(
            ClassContentItem classContentItem,
            ClassSection classSection,
            Integer lessonId,
            Integer quizId,
            Integer assignmentId,
            boolean requireReference
    ) {
        int referenceCount = countNotNull(lessonId, quizId, assignmentId);
        if (referenceCount == 0) {
            if (requireReference) {
                throw new BusinessException("lessonId/quizId/assignmentId is required for class content item");
            }
            return;
        }
        if (referenceCount > 1) {
            throw new BusinessException("Only one reference can be set at a time");
        }

        classContentItem.setLesson(null);
        classContentItem.setQuiz(null);
        classContentItem.setAssignment(null);

        if (lessonId != null) {
            if (classContentItem.getItemType() != ContentItemType.LESSON) {
                throw new BusinessException("lessonId is only valid for LESSON content items");
            }
            Lesson lesson = lessonRepository.findById(lessonId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));
            classContentItem.setLesson(lesson);
            if (!StringUtils.hasText(classContentItem.getTitle())) {
                classContentItem.setTitle(lesson.getTitle());
            }
            return;
        }

        if (quizId != null) {
            if (classContentItem.getItemType() != ContentItemType.QUIZ) {
                throw new BusinessException("quizId is only valid for QUIZ content items");
            }
            Quiz quiz = quizRepository.findById(quizId)
                    .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));
            if (quiz.getClassSection() != null && !quiz.getClassSection().getId().equals(classSection.getId())) {
                throw new BusinessException("Quiz belongs to a different class section");
            }
            classContentItem.setQuiz(quiz);
            if (!StringUtils.hasText(classContentItem.getTitle())) {
                classContentItem.setTitle(quiz.getTitle());
            }
            return;
        }

        if (assignmentId != null) {
            if (classContentItem.getItemType() != ContentItemType.ASSIGNMENT) {
                throw new BusinessException("assignmentId is only valid for ASSIGNMENT content items");
            }
            Assignment assignment = assignmentRepository.findById(assignmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
            if (assignment.getClassSection() != null && !assignment.getClassSection().getId().equals(classSection.getId())) {
                throw new BusinessException("Assignment belongs to a different class section");
            }
            classContentItem.setAssignment(assignment);
            if (!StringUtils.hasText(classContentItem.getTitle())) {
                classContentItem.setTitle(assignment.getTitle());
            }
        }
    }

    private void applyTemplateSnapshot(
            ClassContentItem classContentItem,
            ClassSection classSection,
            ContentItemTemplate templateItem
    ) {
        if (templateItem.getLessonTemplate() != null) {
            Lesson lesson = cloneLessonTemplate(templateItem.getLessonTemplate());
            classContentItem.setLesson(lesson);
            classContentItem.setTitle(lesson.getTitle());
            return;
        }

        if (templateItem.getQuizTemplate() != null) {
            Quiz quiz = cloneQuizTemplate(templateItem.getQuizTemplate(), classSection);
            classContentItem.setQuiz(quiz);
            classContentItem.setTitle(quiz.getTitle());
            return;
        }

        if (templateItem.getAssignmentTemplate() != null) {
            Assignment assignment = cloneAssignmentTemplate(templateItem.getAssignmentTemplate(), classSection);
            classContentItem.setAssignment(assignment);
            classContentItem.setTitle(assignment.getTitle());
            return;
        }

        classContentItem.setTitle(resolveTemplateContentPlaceholderTitle(templateItem.getItemType()));
    }

    private String resolveTemplateContentPlaceholderTitle(ContentItemType itemType) {
        return switch (itemType) {
            case LESSON -> "Untitled lesson";
            case QUIZ -> "Untitled quiz";
            case ASSIGNMENT -> "Untitled assignment";
        };
    }

    private Lesson cloneLessonTemplate(LessonTemplate template) {
        Lesson lesson = new Lesson();
        lesson.setTitle(template.getTitle());
        lesson.setContent(template.getContent());
        lesson.setVideoUrl(template.getVideoUrl());
        lesson.setNotes(template.getNotes());
        lesson.setIsFinished(false);
        return lessonRepository.save(lesson);
    }

    private Quiz cloneQuizTemplate(QuizTemplate template, ClassSection classSection) {
        Quiz quiz = new Quiz();
        quiz.setTitle(template.getTitle());
        quiz.setDescription(template.getDescription());
        quiz.setMinPassScore(template.getMinPassScore());
        quiz.setTimeLimitMinutes(template.getTimeLimitMinutes());
        quiz.setMaxAttempts(template.getMaxAttempts());
        quiz.setAvailableFrom(template.getAvailableFrom());
        quiz.setAvailableUntil(template.getAvailableTo());
        quiz.setClassSection(classSection);
        return quizRepository.save(quiz);
    }

    private Assignment cloneAssignmentTemplate(AssignmentTemplate template, ClassSection classSection) {
        Assignment assignment = new Assignment();
        assignment.setTitle(template.getTitle());
        assignment.setDescription(template.getDescription());
        assignment.setInstruction(template.getInstruction());
        assignment.setMaxScore(template.getMaxScore());
        assignment.setDueAt(template.getDueAt());
        assignment.setAllowLateSubmission(template.isAllowLateSubmission());
        assignment.setClassSection(classSection);
        return assignmentRepository.save(assignment);
    }

   /* private ChapterTemplate resolveChapterTemplateForClassSection(ClassSection classSection, Integer chapterTemplateId) {
        if (chapterTemplateId == null) {
            return null;
        }
        ChapterTemplate chapterTemplate = chapterTemplateRepository.findById(chapterTemplateId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter template not found"));
        if (classSection.getCurriculumTemplate() == null) {
            throw new BusinessException("This class section is not bound to a curriculum template");
        }
        if (chapterTemplate.getCurriculumTemplate() == null
                || !chapterTemplate.getCurriculumTemplate().getId().equals(classSection.getCurriculumTemplate().getId())) {
            throw new BusinessException("Chapter template does not belong to this class section curriculum template");
        }
        return chapterTemplate;
    }

    private ContentItemTemplate resolveContentItemTemplateForClassSection(
            ClassSection classSection,
            Integer contentItemTemplateId
    ) {
        if (contentItemTemplateId == null) {
            return null;
        }
        ContentItemTemplate contentItemTemplate = contentItemTemplateRepository.findById(contentItemTemplateId)
                .orElseThrow(() -> new ResourceNotFoundException("Content item template not found"));
        if (classSection.getCurriculumTemplate() == null) {
            throw new BusinessException("This class section is not bound to a curriculum template");
        }
        if (contentItemTemplate.getChapterTemplate() == null
                || contentItemTemplate.getChapterTemplate().getCurriculumTemplate() == null
                || !contentItemTemplate.getChapterTemplate().getCurriculumTemplate().getId()
                .equals(classSection.getCurriculumTemplate().getId())) {
            throw new BusinessException("Content item template does not belong to this class section curriculum template");
        }
        return contentItemTemplate;
    }
*/
    private Integer resolveClassChapterOrderIndex(Integer classSectionId, Integer requestedOrderIndex) {
        if (requestedOrderIndex != null) {
            return requestedOrderIndex;
        }

        return classChapterRepository.findByClassSection_IdOrderByOrderIndexAsc(classSectionId).stream()
                .map(ClassChapter::getOrderIndex)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private Integer resolveClassContentItemOrderIndex(Integer classChapterId, Integer requestedOrderIndex) {
        if (requestedOrderIndex != null) {
            return requestedOrderIndex;
        }

        return classContentItemRepository.findByClassChapter_IdOrderByOrderIndexAsc(classChapterId).stream()
                .map(ClassContentItem::getOrderIndex)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private String normalizeNullableText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void syncTeachingMembers(ClassSection classSection, User teacher, List<Integer> taIds) {
        createOrUpdateMembership(classSection, teacher, ClassMemberRole.TEACHER);

        Set<Integer> desiredTaIds = new LinkedHashSet<>();
        for (Integer taId : safeList(taIds)) {
            if (taId == null || taId.equals(teacher.getId())) {
                continue;
            }
            desiredTaIds.add(taId);
        }

        Map<Integer, ClassMember> existingByUserId = new LinkedHashMap<>();
        for (ClassMember member : classMemberRepository.findByClassSection_Id(classSection.getId())) {
            if (member.getUser() != null) {
                existingByUserId.put(member.getUser().getId(), member);
            }
        }

        for (Integer taId : desiredTaIds) {
            User taUser = userRepository.findById(taId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            createOrUpdateMembership(classSection, taUser, ClassMemberRole.TA);
        }

        for (ClassMember member : existingByUserId.values()) {
            if (member.getUser() == null) {
                continue;
            }
            Integer userId = member.getUser().getId();
            if (userId.equals(teacher.getId())) {
                continue;
            }
            if (desiredTaIds.contains(userId)) {
                continue;
            }
            classMemberRepository.delete(member);
        }
    }

    private ClassMember createOrUpdateMembership(ClassSection classSection, User user, ClassMemberRole role) {
        ClassMember member = classMemberRepository
                .findByClassSection_IdAndUser_Id(classSection.getId(), user.getId())
                .orElseGet(() -> {
                    ClassMember created = new ClassMember();
                    created.setClassSection(classSection);
                    created.setUser(user);
                    return created;
                });
        member.setRole(role);
        return classMemberRepository.save(member);
    }

    private void transferTeacherOwnership(ClassSection classSection, User newTeacher) {
        ClassMember currentTeacher = classMemberRepository.findByClassSection_IdAndRole(
                        classSection.getId(),
                        ClassMemberRole.TEACHER
                )
                .orElse(null);

        if (currentTeacher != null
                && currentTeacher.getUser() != null
                && currentTeacher.getUser().getId().equals(newTeacher.getId())) {
            return;
        }

        if (currentTeacher != null) {
            currentTeacher.setRole(ClassMemberRole.TA);
            classMemberRepository.save(currentTeacher);
        }

        createOrUpdateMembership(classSection, newTeacher, ClassMemberRole.TEACHER);
        classSection.setTeacher(newTeacher);
        classSectionRepository.save(classSection);
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

    @Override
    @Transactional
    public ClassSectionResponse resetClassCode(Integer id) {
        ClassSection classSection = classSectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        requireTeacherPermission(classSection);
        classSection.setClassCode(resolveClassCode());
        classSectionRepository.save(classSection);
        return getClassSectionById(id);
    }

    @Override
    @Transactional
    public ClassSectionResponse deleteClassCode(Integer id) {
        ClassSection classSection = classSectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        requireTeacherPermission(classSection);
        classSection.setClassCode(null);
        classSectionRepository.save(classSection);
        return getClassSectionById(id);
    }

    @Override
    @Transactional
    public void deleteClassSection(Integer id) {
        ClassSection classSection = classSectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        requireTeacherPermission(classSection);
        classSectionRepository.delete(classSection);
    }

    @Override
    @Transactional
    public ClassSectionResponse updateClassSection(Integer id, ClassSectionUpdateRequest request) {
        ClassSection classSection = classSectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        requireTeacherPermission(classSection);
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            classSection.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            classSection.setDescription(request.getDescription());
        }
        if (request.getStartDate() != null) {
            classSection.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            classSection.setEndDate(request.getEndDate());
        }
        classSectionRepository.save(classSection);
        return getClassSectionById(id);
    }

    private ClassSectionStatus resolveStatus(ClassSectionStatus status) {
        return status != null ? status : ClassSectionStatus.PRIVATE;
    }

    private String resolveClassCode() {
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
        return Optional.ofNullable(values).orElseGet(ArrayList::new);
    }

    private void requireTeacherPermission(ClassSection classSection) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        if (!classMemberAuthorizationService.isTeacher(classSection, currentUser)) {
            throw new UnauthorizedException("You do not manage this class section");
        }
    }
}
