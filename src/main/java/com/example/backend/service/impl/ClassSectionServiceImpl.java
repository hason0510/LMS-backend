package com.example.backend.service.impl;

import com.example.backend.constant.*;
import com.example.backend.dto.request.classsection.ClassChapterCreateRequest;
import com.example.backend.dto.request.classsection.ClassChapterOverrideRequest;
import com.example.backend.dto.request.classsection.ClassContentItemCreateRequest;
import com.example.backend.dto.request.classsection.ClassContentItemOverrideRequest;
import com.example.backend.dto.request.classsection.ClassMemberPermissionsRequest;
import com.example.backend.dto.request.classsection.ClassMemberRequest;
import com.example.backend.dto.request.classsection.ClassMemberRoleRequest;
import com.example.backend.dto.request.classsection.ClassSectionRequest;
import com.example.backend.dto.request.classsection.ClassSectionSearchRequest;
import com.example.backend.dto.request.classsection.ClassSectionUpdateRequest;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.classsection.ClassChapterResponse;
import com.example.backend.dto.response.classsection.ClassContentCompletionRowResponse;
import com.example.backend.dto.response.classsection.ClassContentItemResponse;
import com.example.backend.dto.response.classsection.ClassSectionJoinPreviewResponse;
import com.example.backend.dto.response.classsection.ClassMemberResponse;
import com.example.backend.dto.response.classsection.ClassSectionResponse;
import com.example.backend.entity.Progress;
import com.example.backend.entity.assignment.Assignment;
import com.example.backend.entity.assignment.Submission;
import com.example.backend.entity.template.AssignmentTemplate;
import com.example.backend.entity.template.ChapterTemplate;
import com.example.backend.entity.ClassChapter;
import com.example.backend.entity.ClassContentItem;
import com.example.backend.entity.ClassMember;
import com.example.backend.entity.Resource;
import com.example.backend.entity.ClassSection;
import com.example.backend.entity.template.ContentItemTemplate;
import com.example.backend.entity.template.CurriculumTemplate;
import com.example.backend.entity.Enrollment;
import com.example.backend.entity.Lesson;
import com.example.backend.entity.template.LessonTemplate;
import com.example.backend.entity.quiz.Quiz;
import com.example.backend.entity.quiz.QuestionInteractionItem;
import com.example.backend.entity.quiz.QuizAnswer;
import com.example.backend.entity.quiz.QuizBankSource;
import com.example.backend.entity.quiz.QuizQuestion;
import com.example.backend.entity.template.QuizTemplate;
import com.example.backend.entity.template.QuizTemplateAnswer;
import com.example.backend.entity.template.QuizTemplateBankSource;
import com.example.backend.entity.template.QuizTemplateQuestion;
import com.example.backend.entity.template.QuizTemplateQuestionItem;
import com.example.backend.entity.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.repository.AssignmentRepository;
import com.example.backend.repository.AssignmentTemplateRepository;
import com.example.backend.repository.BankQuestionRepository;
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
import com.example.backend.repository.ProgressRepository;
import com.example.backend.repository.QuizRepository;
import com.example.backend.repository.QuizBankSourceRepository;
import com.example.backend.repository.QuizQuestionRepository;
import com.example.backend.repository.QuizTemplateRepository;
import com.example.backend.repository.QuizTemplateBankSourceRepository;
import com.example.backend.repository.QuizTemplateQuestionRepository;
import com.example.backend.repository.ResourceRepository;
import com.example.backend.repository.SubmissionRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.ClassMemberAuthorizationService;
import com.example.backend.service.ClassContentAccessResult;
import com.example.backend.service.ClassContentAccessService;
import com.example.backend.service.ClassSectionService;
import com.example.backend.service.EnrollmentService;
import com.example.backend.service.ResourceAuthorizationService;
import com.example.backend.service.UserService;
import com.example.backend.specification.ClassSectionSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
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
    private final ProgressRepository progressRepository;
    private final QuizRepository quizRepository;
    private final QuizBankSourceRepository quizBankSourceRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizTemplateRepository quizTemplateRepository;
    private final QuizTemplateQuestionRepository quizTemplateQuestionRepository;
    private final QuizTemplateBankSourceRepository quizTemplateBankSourceRepository;
    private final ResourceRepository resourceRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentTemplateRepository assignmentTemplateRepository;
    private final SubmissionRepository submissionRepository;
    private final BankQuestionRepository bankQuestionRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final ClassMemberAuthorizationService classMemberAuthorizationService;
    private final ClassContentAccessService classContentAccessService;
    private final EnrollmentService enrollmentService;
    private final ResourceAuthorizationService resourceAuthorizationService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public ClassSectionResponse createFromTemplate(Integer curriculumTemplateId, ClassSectionRequest request) {
        CurriculumTemplate curriculumTemplate = curriculumTemplateRepository.findById(curriculumTemplateId)
                .orElseThrow(() -> new ResourceNotFoundException("Curriculum template not found"));

        User currentUser = userService.getCurrentUser();
        User teacher = resolveTeacherForClassCreation(request.getTeacherId(), currentUser);

        ClassSection classSection = new ClassSection();
        classSection.setClassCode(StringUtils.hasText(request.getClassCode())
                ? request.getClassCode()
                : resolveClassCode());
        classSection.setTitle(request.getTitle());
        classSection.setDescription(request.getDescription());
        Resource imageResource = resolveImageResource(request.getImageResourceId());
        classSection.setImageResource(imageResource);
        classSection.setImageUrl(resolveImageUrl(request.getImageUrl(), imageResource));
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
                .sorted(Comparator
                        .comparing(ClassSection::getCreatedDate, Comparator.nullsLast(LocalDate::compareTo))
                        .thenComparing(ClassSection::getId, Comparator.nullsLast(Integer::compareTo)))
                .map(classSection -> convertToResponse(classSection, includeChapters))
                .toList();
    }

    @Override
    public PageResponse<ClassSectionResponse> searchClassSections(ClassSectionSearchRequest request) {
        ClassSectionSearchRequest safeRequest = request != null ? request : new ClassSectionSearchRequest();
        User currentUser = userService.getCurrentUser();
        Pageable pageable = PageRequest.of(
                resolvePageNumber(safeRequest.getPageNumber()) - 1,
                resolvePageSize(safeRequest.getPageSize()),
                resolveSort(safeRequest.getSortBy(), safeRequest.getSortDirection())
        );

        Specification<ClassSection> spec = Specification.where(ClassSectionSpecification.titleContains(safeRequest.getKeyword()))
                .and(ClassSectionSpecification.teacherNameContains(safeRequest.getTeacherKeyword()))
                .and(ClassSectionSpecification.subjectCodeContains(safeRequest.getSubjectKeyword()))
                .and(ClassSectionSpecification.hasCategoryId(safeRequest.getCategoryId()))
                .and(ClassSectionSpecification.hasSubjectId(safeRequest.getSubjectId()))
                .and(ClassSectionSpecification.hasStatus(safeRequest.getStatus()))
                .and(ClassSectionSpecification.startDateBetween(safeRequest.getStartDateFrom(), safeRequest.getStartDateTo()));

        String scope = normalizeScope(safeRequest.getScope(), currentUser);
        if ("MY".equals(scope)) {
            spec = spec.and(ClassSectionSpecification.enrolledByStudent(currentUser.getId()));
        } else if ("PUBLIC".equals(scope)) {
            spec = spec.and(ClassSectionSpecification.visiblePublicClasses())
                    .and(ClassSectionSpecification.notEnrolledByStudent(currentUser.getId()));
        }

        Page<ClassSection> page = classSectionRepository.findAll(spec, pageable);
        List<ClassSectionResponse> items = page.getContent().stream()
                .map(classSection -> convertToResponse(classSection, false))
                .toList();

        return new PageResponse<>(
                page.getNumber() + 1,
                page.getTotalPages(),
                page.getTotalElements(),
                items
        );
    }

    @Override
    public ClassSectionJoinPreviewResponse getJoinPreview(String classCode) {
        if (!StringUtils.hasText(classCode)) {
            throw new BusinessException("Ma lop khong hop le");
        }

        User currentUser = userService.getCurrentUser();
        ClassSection classSection = classSectionRepository.findByClassCode(classCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay lop hoc!"));

        if (classSection.getStatus() == ClassSectionStatus.ARCHIVED) {
            throw new BusinessException("Lop hoc da luu tru, khong the tham gia");
        }
        ensureCurrentUserIsNotClassStaff(currentUser, classSection);

        Enrollment enrollment = enrollmentRepository.findByStudent_IdAndClassSection_Id(currentUser.getId(), classSection.getId());
        ClassSectionJoinPreviewResponse response = new ClassSectionJoinPreviewResponse();
        response.setId(classSection.getId());
        response.setClassCode(classSection.getClassCode());
        response.setTitle(classSection.getTitle());
        response.setImageUrl(classSection.getImageUrl());
        response.setStatus(classSection.getStatus());
        response.setStartDate(classSection.getStartDate());
        response.setEndDate(classSection.getEndDate());
        response.setSubjectId(classSection.getSubject() != null ? classSection.getSubject().getId() : null);
        response.setSubjectCode(classSection.getSubject() != null ? classSection.getSubject().getCode() : null);
        response.setSubjectTitle(classSection.getSubject() != null ? classSection.getSubject().getTitle() : null);
        response.setCategoryId(classSection.getSubject() != null && classSection.getSubject().getCategory() != null
                ? classSection.getSubject().getCategory().getId()
                : null);
        response.setCategoryTitle(classSection.getSubject() != null && classSection.getSubject().getCategory() != null
                ? classSection.getSubject().getCategory().getTitle()
                : null);

        User primaryTeacher = classMemberAuthorizationService.resolvePrimaryTeacher(classSection);
        response.setTeacherId(primaryTeacher != null ? primaryTeacher.getId() : null);
        response.setTeacherName(primaryTeacher != null ? primaryTeacher.getFullName() : null);
        response.setTeacherImageUrl(primaryTeacher != null ? primaryTeacher.getImageUrl() : null);
        response.setTotalEnrollments(enrollmentRepository.countApprovedEnrollmentsByClassSectionId(classSection.getId()));
        response.setAlreadyJoined(enrollment != null);
        response.setEnrollmentStatus(enrollment != null && enrollment.getApprovalStatus() != null
                ? enrollment.getApprovalStatus().name()
                : null);
        response.setJoinMode(classSection.getStatus() == ClassSectionStatus.PUBLIC ? "INSTANT" : "REQUEST");
        response.setJoinMessage(classSection.getStatus() == ClassSectionStatus.PUBLIC
                ? "Lop hoc mo, bat ky ai co tai khoan deu co the tham gia ngay."
                : "Lop hoc rieng tu. Yeu cau cua ban se duoc gui toi giang vien de phe duyet.");
        return response;
    }

    @Override
    @Transactional
    public ClassMemberResponse addMember(Integer classSectionId, ClassMemberRequest request) {
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        requireCapability(classSection, ClassMemberAuthorizationService.CAP_MANAGE_STAFF);

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getRole() == ClassMemberRole.TEACHER) {
            ensureUserIsNotEnrolledInClassSection(user, classSection);
            transferTeacherOwnership(classSection, user);
            return classMemberRepository.findByClassSection_IdAndUser_Id(classSectionId, user.getId())
                    .map(this::convertClassMember)
                    .orElseThrow(() -> new ResourceNotFoundException("Class member not found"));
        }

        ensureUserIsNotEnrolledInClassSection(user, classSection);
        ClassMember saved = createOrUpdateMembership(classSection, user, ClassMemberRole.TA);
        return convertClassMember(saved);
    }

    @Override
    @Transactional
    public ClassMemberResponse updateMemberRole(Integer classSectionId, Integer userId, ClassMemberRoleRequest request) {
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        requireCapability(classSection, ClassMemberAuthorizationService.CAP_MANAGE_STAFF);

        ClassMember member = classMemberRepository.findByClassSection_IdAndUser_Id(classSectionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Class member not found"));

        if (request.getRole() == ClassMemberRole.TEACHER) {
            ensureUserIsNotEnrolledInClassSection(member.getUser(), classSection);
            transferTeacherOwnership(classSection, member.getUser());
            return classMemberRepository.findByClassSection_IdAndUser_Id(classSectionId, userId)
                    .map(this::convertClassMember)
                    .orElseThrow(() -> new ResourceNotFoundException("Class member not found"));
        }

        if (member.getRole() == ClassMemberRole.TEACHER) {
            throw new BusinessException("Cannot downgrade TEACHER directly. Transfer teacher role first.");
        }
        member.setRole(ClassMemberRole.TA);
        applyDefaultPermissions(member, ClassMemberRole.TA);
        return convertClassMember(classMemberRepository.save(member));
    }

    @Override
    @Transactional
    public ClassMemberResponse updateMemberPermissions(
            Integer classSectionId,
            Integer userId,
            ClassMemberPermissionsRequest request
    ) {
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        requireCapability(classSection, ClassMemberAuthorizationService.CAP_MANAGE_STAFF);

        ClassMember member = classMemberRepository.findByClassSection_IdAndUser_Id(classSectionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Class member not found"));
        if (member.getRole() == ClassMemberRole.TEACHER) {
            throw new BusinessException("Teacher permissions are fixed by the class owner role");
        }
        if (member.getRole() != ClassMemberRole.TA) {
            throw new BusinessException("Only TA permissions can be updated");
        }

        List<String> normalizedPermissions = normalizeTaPermissions(request.getPermissions());
        member.setPermissions(normalizedPermissions);
        return convertClassMember(classMemberRepository.save(member));
    }

    @Override
    @Transactional
    public void removeMember(Integer classSectionId, Integer userId) {
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        requireCapability(classSection, ClassMemberAuthorizationService.CAP_MANAGE_STAFF);

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
        requireCapability(classSection, ClassMemberAuthorizationService.CAP_MANAGE_STAFF);
        return getTeachingMembers(classSection);
    }

    @Override
    @Transactional
    public ClassChapterResponse createClassChapter(Integer classSectionId, ClassChapterCreateRequest request) {
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        requireCapability(classSection, ClassMemberAuthorizationService.CAP_EDIT_CONTENT);
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
        requireCapability(classSection, ClassMemberAuthorizationService.CAP_EDIT_CONTENT);

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
        requireCapability(classSection, ClassMemberAuthorizationService.CAP_EDIT_CONTENT);

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
        requireContentMutationPermission(classSection, request.getItemType());

        ClassChapter classChapter = classChapterRepository.findByIdAndClassSection_Id(classChapterId, classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class chapter not found in this class section"));

        ClassContentItem classContentItem = new ClassContentItem();
        classContentItem.setClassChapter(classChapter);
        classContentItem.setOrderIndex(resolveClassContentItemOrderIndex(classChapterId, request.getOrderIndex()));
        classContentItem.setIsHidden(Boolean.TRUE.equals(request.getHidden()));
        classContentItem.setIsLocked(Boolean.TRUE.equals(request.getLocked()));
        classContentItem.setAvailableFrom(request.getAvailableFrom());
        classContentItem.setAvailableTo(request.getAvailableTo());

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
            throw new BusinessException("itemType is required");
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
        ClassContentItem savedItem = classContentItemRepository.save(classContentItem);
        recalculateLearningProgressForApprovedStudents(classSectionId);
        return convertContentItemToResponse(savedItem);
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

        ClassContentItem classContentItem = classContentItemRepository
                .findByIdAndClassChapter_ClassSection_Id(classContentItemId, classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class content item not found in this class section"));
        requireContentMutationPermission(classSection, classContentItem.getItemType());
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

        syncContentItemTitleFromReference(classContentItem);
        if (!StringUtils.hasText(classContentItem.getTitle())) {
            throw new BusinessException("Class content item must have a title");
        }
        ClassContentItem savedItem = classContentItemRepository.save(classContentItem);
        recalculateLearningProgressForApprovedStudents(classSectionId);
        return convertContentItemToResponse(savedItem);
    }

    @Override
    @Transactional
    public void deleteClassContentItem(Integer classSectionId, Integer classContentItemId) {
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));

        ClassContentItem classContentItem = classContentItemRepository
                .findByIdAndClassChapter_ClassSection_Id(classContentItemId, classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class content item not found in this class section"));
        requireContentMutationPermission(classSection, classContentItem.getItemType());
        classContentItemRepository.delete(classContentItem);
        recalculateLearningProgressForApprovedStudents(classSectionId);
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
        List<Enrollment> enrollments = enrollmentRepository.findByStudent_IdAndClassSection_IdIsNotNull(currentUser.getId());

        return enrollments.stream()
                .map(enrollment -> convertToResponse(enrollment.getClassSection(), false))
                .toList();
    }

    @Override
    public List<ClassChapterResponse> getClassChapters(Integer classSectionId) {
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        requireViewPermission(classSection);
        return classChapterRepository.findByClassSection_IdOrderByOrderIndexAsc(classSectionId).stream()
                .map(this::convertChapterToResponse)
                .toList();
    }

    @Override
    public List<ClassContentItemResponse> getClassContentItems(Integer classSectionId, Integer classChapterId) {
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        requireViewPermission(classSection);
        ClassChapter classChapter = classChapterRepository.findByIdAndClassSection_Id(classChapterId, classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class chapter not found in this class section"));
        User currentUser = userService.getCurrentUser();
        List<ClassContentItem> contentItems = classContentItemRepository.findByClassChapter_IdOrderByOrderIndexAsc(classChapter.getId()).stream()
                .filter(item -> shouldIncludeItemForUser(item, currentUser))
                .toList();
        Map<Integer, Progress> completedProgressByItemId = resolveCompletedProgressByItemId(contentItems, currentUser);
        Map<Integer, Submission> submissionsByAssignmentId = resolveSubmissionByAssignmentId(contentItems, currentUser);
        return contentItems.stream()
                .map(item -> convertContentItemToResponse(item, currentUser, completedProgressByItemId, submissionsByAssignmentId))
                .toList();
    }

    @Override
    public List<ClassContentCompletionRowResponse> getClassContentCompletion(Integer classSectionId, Integer classContentItemId) {
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        requireCapability(classSection, ClassMemberAuthorizationService.CAP_VIEW_PROGRESS, false);

        ClassContentItem classContentItem = classContentItemRepository.findByIdAndClassChapter_ClassSection_Id(
                        classContentItemId,
                        classSectionId
                )
                .orElseThrow(() -> new ResourceNotFoundException("Class content item not found in this class section"));

        if (classContentItem.getItemType() != ContentItemType.LESSON) {
            throw new BusinessException("Only lesson completion is supported in this view");
        }

        List<Enrollment> approvedEnrollments = enrollmentRepository.findByClassSection_IdAndApprovalStatus(
                classSectionId,
                EnrollmentStatus.APPROVED
        );
        if (approvedEnrollments.isEmpty()) {
            return List.of();
        }

        List<Integer> studentIds = approvedEnrollments.stream()
                .map(Enrollment::getStudent)
                .filter(Objects::nonNull)
                .map(User::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Integer, Progress> completedProgressByStudentId = progressRepository
                .findCompletedByClassContentItemAndStudentIds(classContentItemId, studentIds)
                .stream()
                .filter(progress -> progress.getStudent() != null && progress.getStudent().getId() != null)
                .collect(java.util.stream.Collectors.toMap(
                        progress -> progress.getStudent().getId(),
                        progress -> progress,
                        (left, right) -> {
                            java.time.LocalDateTime leftCompletedAt = left.getCompletedAt();
                            java.time.LocalDateTime rightCompletedAt = right.getCompletedAt();
                            if (leftCompletedAt == null) {
                                return right;
                            }
                            if (rightCompletedAt == null) {
                                return left;
                            }
                            return rightCompletedAt.isAfter(leftCompletedAt) ? right : left;
                        }
                ));

        return approvedEnrollments.stream()
                .map(Enrollment::getStudent)
                .filter(Objects::nonNull)
                .map(student -> {
                    Progress progress = completedProgressByStudentId.get(student.getId());
                    return new ClassContentCompletionRowResponse(
                            student.getId(),
                            student.getFullName(),
                            student.getStudentNumber(),
                            student.getGmail(),
                            student.getImageUrl(),
                            progress != null && Boolean.TRUE.equals(progress.getIsCompleted()),
                            progress != null ? progress.getCompletedAt() : null
                    );
                })
                .sorted(Comparator
                        .comparing(ClassContentCompletionRowResponse::getStudentNumber, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(ClassContentCompletionRowResponse::getStudentName, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(ClassContentCompletionRowResponse::getStudentId, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    @Override
    @Transactional
    public ClassSectionResponse updateClassSectionStatus(Integer id, ClassSectionStatus status) {
        ClassSection classSection = classSectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        requireCapability(classSection, ClassMemberAuthorizationService.CAP_MANAGE_CLASS_SETTINGS, false);
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
        response.setImageUrl(classSection.getImageUrl());
        response.setImageResourceId(classSection.getImageResource() != null ? classSection.getImageResource().getId() : null);
        response.setStatus(classSection.getStatus());
        response.setStartDate(classSection.getStartDate());
        response.setEndDate(classSection.getEndDate());
        response.setCreatedDate(classSection.getCreatedDate());
        response.setSubjectId(classSection.getSubject() != null ? classSection.getSubject().getId() : null);
        response.setSubjectCode(classSection.getSubject() != null ? classSection.getSubject().getCode() : null);
        response.setSubjectTitle(classSection.getSubject() != null ? classSection.getSubject().getTitle() : null);
        response.setCategoryId(classSection.getSubject() != null && classSection.getSubject().getCategory() != null
                ? classSection.getSubject().getCategory().getId()
                : null);
        response.setCategoryTitle(classSection.getSubject() != null && classSection.getSubject().getCategory() != null
                ? classSection.getSubject().getCategory().getTitle()
                : null);
        response.setTeacherId(primaryTeacher != null ? primaryTeacher.getId() : null);
        response.setTeacherName(primaryTeacher != null ? primaryTeacher.getFullName() : null);
        response.setTeacherImageUrl(primaryTeacher != null ? primaryTeacher.getImageUrl() : null);
        response.setTeachingMembers(getTeachingMembers(classSection));
        User currentUser = userService.getCurrentUser();
        response.setMyClassRole(classMemberAuthorizationService.resolveMyClassRole(classSection, currentUser));
        response.setMyWorkspaceType(classMemberAuthorizationService.resolveWorkspaceType(classSection, currentUser));
        response.setMyCapabilities(classMemberAuthorizationService.resolveCapabilities(classSection, currentUser));
        response.setCurriculumTemplateId(classSection.getCurriculumTemplate() != null ? classSection.getCurriculumTemplate().getId() : null);
        response.setTemplateBased(classSection.getCurriculumTemplate() != null);
        response.setChapters(includeChapters
                ? classChapterRepository.findByClassSection_IdOrderByOrderIndexAsc(classSection.getId()).stream()
                .map(this::convertChapterToResponse)
                .toList()
                : null);
        response.setTotalEnrollments(
                enrollmentRepository.countApprovedEnrollmentsByClassSectionId(classSection.getId()));
        Enrollment myEnrollment = currentUser != null
                ? enrollmentRepository.findByStudent_IdAndClassSection_Id(currentUser.getId(), classSection.getId())
                : null;
        response.setMyProgress(myEnrollment != null ? myEnrollment.getProgress() : null);
        response.setMyEnrollmentStatus(myEnrollment != null ? myEnrollment.getApprovalStatus() : null);
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
        fallbackTeacher.setEmail(classSection.getTeacher().getGmail());
        fallbackTeacher.setAvatarUrl(classSection.getTeacher().getImageUrl());
        fallbackTeacher.setRole(ClassMemberRole.TEACHER);
        fallbackTeacher.setPermissions(classMemberAuthorizationService.resolveDefaultCapabilities(ClassMemberRole.TEACHER));
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
        response.setEmail(member.getUser() != null ? member.getUser().getGmail() : null);
        response.setAvatarUrl(member.getUser() != null ? member.getUser().getImageUrl() : null);
        response.setRole(member.getRole());
        response.setPermissions(member.getUser() != null
                ? classMemberAuthorizationService.resolveCapabilities(member.getClassSection(), member.getUser())
                : classMemberAuthorizationService.resolveDefaultCapabilities(member.getRole()));
        return response;
    }

    private ClassChapterResponse convertChapterToResponse(ClassChapter classChapter) {
        User currentUser = userService.getCurrentUser();
        List<ClassContentItem> contentItems = classContentItemRepository.findByClassChapter_IdOrderByOrderIndexAsc(classChapter.getId())
                .stream()
                .filter(item -> shouldIncludeItemForUser(item, currentUser))
                .toList();
        Map<Integer, Progress> completedProgressByItemId = resolveCompletedProgressByItemId(contentItems, currentUser);
        Map<Integer, Submission> submissionsByAssignmentId = resolveSubmissionByAssignmentId(contentItems, currentUser);

        ClassChapterResponse response = new ClassChapterResponse();
        response.setId(classChapter.getId());
        response.setTitle(classChapter.getTitle());
        response.setDescription(classChapter.getDescription());
        response.setOrderIndex(classChapter.getOrderIndex());
        response.setHidden(classChapter.getIsHidden());
        response.setLocked(classChapter.getIsLocked());
        response.setAvailableFrom(classChapter.getAvailableFrom());
        response.setAvailableTo(classChapter.getAvailableTo());
        response.setContentItems(contentItems.stream()
                .map(item -> convertContentItemToResponse(item, currentUser, completedProgressByItemId, submissionsByAssignmentId))
                .toList());
        return response;
    }

    private ClassContentItemResponse convertContentItemToResponse(ClassContentItem classContentItem) {
        return convertContentItemToResponse(
                classContentItem,
                userService.getCurrentUser(),
                Collections.emptyMap(),
                Collections.emptyMap()
        );
    }

    private ClassContentItemResponse convertContentItemToResponse(ClassContentItem classContentItem, User currentUser) {
        return convertContentItemToResponse(
                classContentItem,
                currentUser,
                Collections.emptyMap(),
                Collections.emptyMap()
        );
    }

    private ClassContentItemResponse convertContentItemToResponse(
            ClassContentItem classContentItem,
            User currentUser,
            Map<Integer, Progress> completedProgressByItemId,
            Map<Integer, Submission> submissionsByAssignmentId
    ) {
        ClassContentAccessResult accessResult = classContentAccessService.evaluateForUser(classContentItem, currentUser);
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
        response.setDisplayTitle(resolveContentItemDisplayTitle(classContentItem));
        response.setAvailabilityStatus(accessResult.availabilityStatus());
        response.setAccessible(accessResult.accessible());
        response.setAccessMessageKey(accessResult.messageKey());
        response.setAccessMessage(accessResult.message());
        response.setProgressEligible(isProgressEligible(classContentItem.getItemType()));

        if (isStudent(currentUser)) {
            Progress progress = completedProgressByItemId.get(classContentItem.getId());
            response.setCompleted(progress != null && Boolean.TRUE.equals(progress.getIsCompleted()));
            response.setCompletedAt(progress != null ? progress.getCompletedAt() : null);

            if (classContentItem.getItemType() == ContentItemType.ASSIGNMENT && classContentItem.getAssignment() != null) {
                Submission submission = submissionsByAssignmentId.get(classContentItem.getAssignment().getId());
                SubmissionStatus assignmentStatus = submission != null && submission.getStatus() != null
                        ? submission.getStatus()
                        : SubmissionStatus.NOT_SUBMITTED;
                response.setAssignmentStatus(assignmentStatus.name());
                response.setCompleted(assignmentStatus != SubmissionStatus.NOT_SUBMITTED);
                response.setCompletedAt(submission != null ? submission.getSubmissionTime() : null);
            }
        }
        return response;
    }

    private Map<Integer, Progress> resolveCompletedProgressByItemId(List<ClassContentItem> contentItems, User currentUser) {
        if (!isStudent(currentUser) || contentItems == null || contentItems.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Integer> classContentItemIds = contentItems.stream()
                .map(ClassContentItem::getId)
                .filter(Objects::nonNull)
                .toList();
        if (classContentItemIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Integer, Progress> progressByItemId = new HashMap<>();
        for (Progress progress : progressRepository.findByStudent_IdAndClassContentItem_IdIn(currentUser.getId(), classContentItemIds)) {
            if (progress.getClassContentItem() == null
                    || progress.getClassContentItem().getId() == null
                    || !Boolean.TRUE.equals(progress.getIsCompleted())) {
                continue;
            }
            progressByItemId.put(progress.getClassContentItem().getId(), progress);
        }
        return progressByItemId;
    }

    private Map<Integer, Submission> resolveSubmissionByAssignmentId(List<ClassContentItem> contentItems, User currentUser) {
        if (!isStudent(currentUser) || contentItems == null || contentItems.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Integer> assignmentIds = contentItems.stream()
                .map(ClassContentItem::getAssignment)
                .filter(Objects::nonNull)
                .map(Assignment::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (assignmentIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Integer, Submission> submissionsByAssignmentId = new HashMap<>();
        for (Submission submission : submissionRepository.findByStudent_IdAndAssignment_IdIn(currentUser.getId(), assignmentIds)) {
            if (submission.getAssignment() == null || submission.getAssignment().getId() == null) {
                continue;
            }
            submissionsByAssignmentId.merge(
                    submission.getAssignment().getId(),
                    submission,
                    this::pickLatestSubmission
            );
        }
        return submissionsByAssignmentId;
    }

    private Submission pickLatestSubmission(Submission left, Submission right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        if (left.getSubmissionTime() == null) {
            return right;
        }
        if (right.getSubmissionTime() == null) {
            return left;
        }
        return right.getSubmissionTime().isAfter(left.getSubmissionTime()) ? right : left;
    }

    private boolean isProgressEligible(ContentItemType itemType) {
        return itemType == ContentItemType.LESSON || itemType == ContentItemType.QUIZ;
    }

    private void recalculateLearningProgressForApprovedStudents(Integer classSectionId) {
        enrollmentRepository.findByClassSection_IdAndApprovalStatus(classSectionId, EnrollmentStatus.APPROVED).forEach(
                enrollment -> {
                    if (enrollment.getStudent() != null && enrollment.getStudent().getId() != null) {
                        enrollmentService.recalculateAndSaveProgressForClassSection(
                                enrollment.getStudent().getId(),
                                classSectionId
                        );
                    }
                }
        );
    }

    private String resolveContentItemDisplayTitle(ClassContentItem classContentItem) {
        if (classContentItem.getLesson() != null && StringUtils.hasText(classContentItem.getLesson().getTitle())) {
            return classContentItem.getLesson().getTitle();
        }
        if (classContentItem.getQuiz() != null && StringUtils.hasText(classContentItem.getQuiz().getTitle())) {
            return classContentItem.getQuiz().getTitle();
        }
        if (classContentItem.getAssignment() != null && StringUtils.hasText(classContentItem.getAssignment().getTitle())) {
            return classContentItem.getAssignment().getTitle();
        }
        return classContentItem.getTitle();
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
            syncContentItemTitleFromReference(classContentItem);
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
            if (quiz.getClassSection() == null) {
                quiz.setClassSection(classSection);
                quizRepository.save(quiz);
            }
            classContentItem.setQuiz(quiz);
            syncContentItemTitleFromReference(classContentItem);
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
            syncContentItemTitleFromReference(classContentItem);
        }
    }

    private void syncContentItemTitleFromReference(ClassContentItem classContentItem) {
        if (classContentItem.getLesson() != null && StringUtils.hasText(classContentItem.getLesson().getTitle())) {
            classContentItem.setTitle(classContentItem.getLesson().getTitle());
            return;
        }
        if (classContentItem.getQuiz() != null && StringUtils.hasText(classContentItem.getQuiz().getTitle())) {
            classContentItem.setTitle(classContentItem.getQuiz().getTitle());
            return;
        }
        if (classContentItem.getAssignment() != null && StringUtils.hasText(classContentItem.getAssignment().getTitle())) {
            classContentItem.setTitle(classContentItem.getAssignment().getTitle());
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
        quiz.setGenerateQuestionsPerAttempt(template.isGenerateQuestionsPerAttempt());
        quiz.setShuffleQuestions(template.isShuffleQuestions());
        quiz.setShuffleAnswers(template.isShuffleAnswers());
        quiz.setDisplayMode(StringUtils.hasText(template.getDisplayMode()) ? template.getDisplayMode() : "PAGINATION");
        quiz.setShowCorrectAnswer(template.isShowCorrectAnswer());
        quiz.setClassSection(classSection);
        Quiz savedQuiz = quizRepository.save(quiz);

        List<QuizTemplateBankSource> templateSources =
                quizTemplateBankSourceRepository.findByQuizTemplate_IdOrderByOrderIndexAsc(template.getId());
        List<QuizTemplateQuestion> templateQuestions =
                quizTemplateQuestionRepository.findByQuizTemplate_IdOrderByOrderIndexAscIdAsc(template.getId());

        if (!templateSources.isEmpty()) {
            cloneBankSourcesFromTemplate(savedQuiz, templateSources);
            if (!savedQuiz.isGenerateQuestionsPerAttempt()) {
                savedQuiz.setGenerateQuestionsPerAttempt(true);
                quizRepository.save(savedQuiz);
            }
            return savedQuiz;
        }

        if (!templateQuestions.isEmpty()) {
            cloneManualQuestionsFromTemplate(savedQuiz, templateQuestions);
        }
        return savedQuiz;
    }

    private List<QuizBankSource> cloneBankSourcesFromTemplate(Quiz quiz, List<QuizTemplateBankSource> templateSources) {
        List<QuizBankSource> savedSources = new ArrayList<>();
        int orderIndex = 1;
        for (QuizTemplateBankSource templateSource : templateSources) {
            if (templateSource.getQuestionBank() == null) {
                throw new BusinessException("Quiz template has an invalid question bank source");
            }

            QuizBankSource source = new QuizBankSource();
            source.setQuiz(quiz);
            source.setQuestionBank(templateSource.getQuestionBank());
            source.setTags(templateSource.getTags() != null ? new ArrayList<>(templateSource.getTags()) : new ArrayList<>());
            source.setTagMatchMode(templateSource.getTagMatchMode() != null ? templateSource.getTagMatchMode() : com.example.backend.constant.QuizTagMatchMode.ANY);
            source.setSelectionMode(templateSource.getSelectionMode());
            source.setQuestionCount(templateSource.getQuestionCount());
            source.setDifficultyLevel(templateSource.getDifficultyLevel());
            source.setOrderIndex(
                    templateSource.getOrderIndex() != null ? templateSource.getOrderIndex() : orderIndex
            );
            savedSources.add(quizBankSourceRepository.save(source));
            orderIndex++;
        }
        return savedSources;
    }

    private void cloneManualQuestionsFromTemplate(Quiz quiz, List<QuizTemplateQuestion> templateQuestions) {
        for (QuizTemplateQuestion templateQuestion : templateQuestions) {
            QuizQuestion question = new QuizQuestion();
            question.setQuiz(quiz);
            question.setContent(templateQuestion.getContent());
            question.setType(templateQuestion.getType());
            question.setPoints(templateQuestion.getPoints() != null ? templateQuestion.getPoints() : BigDecimal.ONE);
            question.setResource(templateQuestion.getResource());
            question.setSourceBankQuestion(templateQuestion.getSourceBankQuestion());

            List<QuizAnswer> answers = new ArrayList<>();
            for (QuizTemplateAnswer templateAnswer : safeList(templateQuestion.getAnswers())) {
                QuizAnswer answer = new QuizAnswer();
                answer.setQuizQuestion(question);
                answer.setContent(templateAnswer.getContent());
                answer.setIsCorrect(templateAnswer.getIsCorrect() != null ? templateAnswer.getIsCorrect() : false);
                answer.setExplanation(templateAnswer.getExplanation());
                answer.setResource(templateAnswer.getResource());
                answers.add(answer);
            }
            question.setAnswers(answers);
            question.setInteractionItems(copyTemplateQuestionItems(templateQuestion, question));
            quizQuestionRepository.save(question);
        }
    }

    private List<QuestionInteractionItem> copyTemplateQuestionItems(
            QuizTemplateQuestion templateQuestion,
            QuizQuestion question
    ) {
        List<QuestionInteractionItem> items = new ArrayList<>();
        for (QuizTemplateQuestionItem templateItem : safeList(templateQuestion.getItems())) {
            QuestionInteractionItem item = new QuestionInteractionItem();
            item.setQuizQuestion(question);
            item.setContent(templateItem.getContent());
            item.setItemKey(templateItem.getItemKey());
            item.setRole(templateItem.getRole());
            item.setCorrectMatchKey(templateItem.getCorrectMatchKey());
            item.setCorrectOrderIndex(templateItem.getCorrectOrderIndex());
            item.setBlankIndex(templateItem.getBlankIndex());
            item.setAcceptedAnswers(templateItem.getAcceptedAnswers());
            item.setBlankType(templateItem.getBlankType());
            item.setBlankOptions(templateItem.getBlankOptions());
            item.setResource(templateItem.getResource());
            item.setOrderIndex(templateItem.getOrderIndex());
            items.add(item);
        }
        return items;
    }

    private Assignment cloneAssignmentTemplate(AssignmentTemplate template, ClassSection classSection) {
        Assignment assignment = new Assignment();
        assignment.setTitle(template.getTitle());
        assignment.setDescription(template.getDescription());
        assignment.setInstruction(template.getInstruction());
        assignment.setMaxScore(template.getMaxScore());
        assignment.setDueAt(template.getDueAt());
        assignment.setCloseAt(template.getCloseAt());
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
        ensureUserIsNotEnrolledInClassSection(user, classSection);
        ClassMember member = classMemberRepository
                .findByClassSection_IdAndUser_Id(classSection.getId(), user.getId())
                .orElseGet(() -> {
                    ClassMember created = new ClassMember();
                    created.setClassSection(classSection);
                    created.setUser(user);
                    return created;
                });
        ClassMemberRole previousRole = member.getRole();
        member.setRole(role);
        if (member.getPermissions() == null || member.getPermissions().isEmpty() || previousRole != role) {
            applyDefaultPermissions(member, role);
        }
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

        ensureUserIsNotEnrolledInClassSection(newTeacher, classSection);

        if (currentTeacher != null) {
            currentTeacher.setRole(ClassMemberRole.TA);
            applyDefaultPermissions(currentTeacher, ClassMemberRole.TA);
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

    private User resolveTeacherForClassCreation(Integer teacherId, User currentUser) {
        boolean isAdminCreator = currentUser != null
                && currentUser.getRole() != null
                && currentUser.getRole().getRoleName() == RoleType.ADMIN;

        User teacher = resolveUser(teacherId, currentUser);
        if (isTeacherAccount(teacher)) {
            return teacher;
        }
        if (isAdminCreator && isCurrentAdmin(teacher, currentUser)) {
            return teacher;
        }
        if (isAdminCreator) {
            throw new BusinessException("Class owner must be the current ADMIN or a TEACHER account");
        }
        throw new BusinessException("Class owner must be a TEACHER account");
    }

    private boolean isTeacherAccount(User user) {
        return user != null
                && user.getRole() != null
                && user.getRole().getRoleName() == RoleType.TEACHER;
    }

    private boolean isCurrentAdmin(User user, User currentUser) {
        return user != null
                && currentUser != null
                && Objects.equals(user.getId(), currentUser.getId());
    }

    @Override
    @Transactional
    public ClassSectionResponse resetClassCode(Integer id) {
        ClassSection classSection = classSectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        requireCapability(classSection, ClassMemberAuthorizationService.CAP_MANAGE_CLASS_SETTINGS);
        classSection.setClassCode(resolveClassCode());
        classSectionRepository.save(classSection);
        return getClassSectionById(id);
    }

    @Override
    @Transactional
    public ClassSectionResponse deleteClassCode(Integer id) {
        ClassSection classSection = classSectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        requireCapability(classSection, ClassMemberAuthorizationService.CAP_MANAGE_CLASS_SETTINGS);
        classSection.setClassCode(null);
        classSectionRepository.save(classSection);
        return getClassSectionById(id);
    }

    @Override
    @Transactional
    public void deleteClassSection(Integer id) {
        ClassSection classSection = classSectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        requireCapability(classSection, ClassMemberAuthorizationService.CAP_MANAGE_CLASS_SETTINGS);
        classSectionRepository.delete(classSection);
    }

    @Override
    @Transactional
    public ClassSectionResponse updateClassSection(Integer id, ClassSectionUpdateRequest request) {
        ClassSection classSection = classSectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        requireCapability(classSection, ClassMemberAuthorizationService.CAP_MANAGE_CLASS_SETTINGS);
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            classSection.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            classSection.setDescription(request.getDescription());
        }
        Resource imageResource = resolveImageResource(request.getImageResourceId());
        classSection.setImageResource(imageResource);
        classSection.setImageUrl(resolveImageUrl(request.getImageUrl(), imageResource));
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

    private Resource resolveImageResource(Integer imageResourceId) {
        if (imageResourceId == null) {
            return null;
        }
        Resource imageResource = resourceRepository.findById(imageResourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Image resource not found"));
        resourceAuthorizationService.assertCanUse(imageResource, null, null);
        return imageResource;
    }

    private String resolveImageUrl(String requestedImageUrl, Resource imageResource) {
        if (StringUtils.hasText(requestedImageUrl)) {
            return requestedImageUrl;
        }
        if (imageResource == null) {
            return null;
        }
        if (StringUtils.hasText(imageResource.getFileUrl())) {
            return imageResource.getFileUrl();
        }
        if (StringUtils.hasText(imageResource.getEmbedUrl())) {
            return imageResource.getEmbedUrl();
        }
        return imageResource.getHlsUrl();
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

    private void applyDefaultPermissions(ClassMember member, ClassMemberRole role) {
        member.setPermissions(new ArrayList<>(classMemberAuthorizationService.resolveDefaultCapabilities(role)));
    }

    private List<String> normalizeTaPermissions(List<String> requestedPermissions) {
        List<String> allowedPermissions = classMemberAuthorizationService.getAssignableTaCapabilities();
        Set<String> invalidPermissions = new LinkedHashSet<>(safeList(requestedPermissions));
        invalidPermissions.removeAll(allowedPermissions);
        if (!invalidPermissions.isEmpty()) {
            throw new BusinessException("Invalid TA permissions: " + String.join(", ", invalidPermissions));
        }

        Set<String> requested = new LinkedHashSet<>(safeList(requestedPermissions));
        requested.add(ClassMemberAuthorizationService.CAP_VIEW_CLASS);
        return allowedPermissions.stream()
                .filter(requested::contains)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private void requireCapability(ClassSection classSection, String capability) {
        requireCapability(classSection, capability, true);
    }

    private void requireCapability(ClassSection classSection, String capability, boolean blockArchived) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        if (blockArchived) {
            ensureClassSectionInteractive(classSection);
        }
        if (!classMemberAuthorizationService.hasCapability(classSection, currentUser, capability)) {
            throw new UnauthorizedException("You do not have permission in this class section");
        }
    }

    private void requireContentMutationPermission(ClassSection classSection, ContentItemType itemType) {
        if (itemType == ContentItemType.ASSIGNMENT) {
            requireCapability(classSection, ClassMemberAuthorizationService.CAP_MANAGE_ASSIGNMENTS);
            return;
        }
        requireCapability(classSection, ClassMemberAuthorizationService.CAP_EDIT_CONTENT);
    }

    private void ensureClassSectionInteractive(ClassSection classSection) {
        if (classSection != null && classSection.getStatus() == ClassSectionStatus.ARCHIVED) {
            throw new BusinessException("Class section is archived and only supports read-only access");
        }
    }

    private void requireViewPermission(ClassSection classSection) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        if (!canViewClassSection(classSection, currentUser)) {
            throw new UnauthorizedException("You do not have permission to access this class section");
        }
    }

    private boolean canViewClassSection(ClassSection classSection, User currentUser) {
        if (classMemberAuthorizationService.isTeacherOrTa(classSection, currentUser)) {
            return true;
        }
        if (currentUser.getRole().getRoleName() != RoleType.STUDENT) {
            return false;
        }
        return enrollmentRepository.findByStudent_IdAndClassSection_Id(
                currentUser.getId(),
                classSection.getId()
        ) != null;
    }

    private void ensureUserIsNotEnrolledInClassSection(User user, ClassSection classSection) {
        if (user == null || classSection == null || classSection.getId() == null) {
            return;
        }
        Enrollment enrollment = enrollmentRepository.findByStudent_IdAndClassSection_Id(user.getId(), classSection.getId());
        if (enrollment != null) {
            throw new BusinessException("Khong the them nguoi dung da la hoc vien vao vai tro giang day trong cung lop");
        }
    }

    private void ensureCurrentUserIsNotClassStaff(User user, ClassSection classSection) {
        if (user == null || classSection == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        if (classMemberAuthorizationService.isTeacherOrTa(classSection, user)) {
            throw new BusinessException("Ban dang la nhan su giang day cua lop hoc nay");
        }
    }

    private int resolvePageNumber(Integer pageNumber) {
        return pageNumber != null && pageNumber > 0 ? pageNumber : 1;
    }

    private int resolvePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return 12;
        }
        return Math.min(pageSize, 100);
    }

    private String normalizeScope(String scope, User currentUser) {
        if (currentUser == null || currentUser.getRole() == null) {
            return "ALL";
        }
        if (!StringUtils.hasText(scope)) {
            return currentUser.getRole().getRoleName() == RoleType.STUDENT ? "PUBLIC" : "ALL";
        }
        String normalized = scope.trim().toUpperCase(Locale.ROOT);
        if ("MY".equals(normalized) || "PUBLIC".equals(normalized) || "ALL".equals(normalized)) {
            return normalized;
        }
        return currentUser.getRole().getRoleName() == RoleType.STUDENT ? "PUBLIC" : "ALL";
    }

    private Sort resolveSort(String sortBy, String sortDirection) {
        String property = switch (sortBy != null ? sortBy.trim() : "") {
            case "title" -> "title";
            case "startDate" -> "startDate";
            default -> "createdDate";
        };
        Sort.Direction direction = "DESC".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, property).and(Sort.by(Sort.Direction.ASC, "id"));
    }

    private boolean shouldIncludeItemForUser(ClassContentItem item, User currentUser) {
        ClassContentAccessResult accessResult = classContentAccessService.evaluateForUser(item, currentUser);
        return !(isStudent(currentUser) && accessResult.availabilityStatus() == ClassContentAvailabilityStatus.HIDDEN);
    }

    private boolean isStudent(User user) {
        return user != null
                && user.getRole() != null
                && user.getRole().getRoleName() == RoleType.STUDENT;
    }
}
