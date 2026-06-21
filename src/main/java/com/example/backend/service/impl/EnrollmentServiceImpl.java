package com.example.backend.service.impl;

import com.example.backend.utils.ClassSectionGuard;

import com.example.backend.cache.CacheNames;
import com.example.backend.cache.RedisCacheInvalidationService;
import com.example.backend.constant.ClassMemberRole;
import com.example.backend.constant.ContentItemType;
import com.example.backend.constant.ClassSectionStatus;
import com.example.backend.constant.EnrollmentStatus;
import com.example.backend.constant.RoleType;
import com.example.backend.dto.request.EnrollmentRequest;
import com.example.backend.dto.request.course.StudentCourseRequest;
import com.example.backend.dto.request.search.SearchUserRequest;
import com.example.backend.dto.response.EnrollmentResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.user.UserViewResponse;
import com.example.backend.entity.ClassContentItem;
import com.example.backend.entity.ClassSection;
import com.example.backend.entity.Enrollment;
import com.example.backend.entity.Progress;
import com.example.backend.entity.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.repository.ClassContentItemRepository;
import com.example.backend.repository.ClassMemberRepository;
import com.example.backend.repository.ClassSectionRepository;
import com.example.backend.repository.EnrollmentRepository;
import com.example.backend.repository.ProgressRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.ClassMemberAuthorizationService;
import com.example.backend.service.EnrollmentService;
import com.example.backend.service.NotificationService;
import com.example.backend.service.UserService;
import com.example.backend.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final ProgressRepository progressRepository;
    private final ClassSectionRepository classSectionRepository;
    private final ClassContentItemRepository classContentItemRepository;
    private final ClassMemberRepository classMemberRepository;
    private final ClassMemberAuthorizationService classMemberAuthorizationService;
    private final RedisCacheInvalidationService cacheInvalidationService;

    @Transactional
    @Override
    public void addStudentsToCourse(Integer courseId, StudentCourseRequest request) {
        throw new UnsupportedOperationException("Legacy course enrollment flow has been removed");
    }

    @Transactional
    @Override
    public void addStudentsToClassSection(Integer classSectionId, StudentCourseRequest request) {
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học!"));

        List<User> users = userRepository.findAllById(request.getStudentIds());
        ensureUsersAreNotClassStaff(classSection, users);
        List<Enrollment> enrollments = users.stream()
                .map(user -> Enrollment.builder()
                        .student(user)
                        .classSection(classSection)
                        .progress(0)
                        .approvalStatus(EnrollmentStatus.APPROVED)
                .build())
                .toList();
        enrollmentRepository.saveAll(enrollments);
        cacheInvalidationService.evictAllRedisReadCaches();

        for (User user : users) {
            String message = "Ban đã được thêm vào lớp học " + classSection.getTitle();
            notificationService.createNotification(user, "Được thêm vào lớp học", message, "ENROLLMENT", null, null);
        }
    }

    @Transactional
    @Override
    public void removeStudentsInCourse(Integer courseId, StudentCourseRequest request) {
        throw new UnsupportedOperationException("Legacy course enrollment flow has been removed");
    }

    @Transactional
    @Override
    public void removeStudentsFromClassSection(Integer classSectionId, StudentCourseRequest request) {
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học!"));
        if (request.getStudentIds() == null || request.getStudentIds().isEmpty()) {
            return;
        }

        List<User> users = userRepository.findAllById(request.getStudentIds());
        List<Enrollment> enrollments = enrollmentRepository.findByClassSection_IdAndStudent_IdIn(classSectionId, request.getStudentIds());
        enrollmentRepository.deleteAll(enrollments);
        cacheInvalidationService.evictAllRedisReadCaches();

        for (User user : users) {
            String message = "Bạn đã bị xóa khỏi danh sách lớp " + classSection.getTitle();
            notificationService.createNotification(user, "Bị xóa khỏi lớp học", message, "ENROLLMENT", null, null);
        }
    }

    @Override
    public EnrollmentResponse enrollPublicCourse(Integer courseId) {
        throw new UnsupportedOperationException("Legacy course enrollment flow has been removed");
    }

    @Override
    public EnrollmentResponse enrollPrivateCourse(String classCode) {
        User currentUser = userService.getCurrentUser();
        if (!userRepository.existsById(currentUser.getId())) {
            throw new ResourceNotFoundException("Không tìm thấy người dùng");
        }
        if (!StringUtils.hasText(classCode)) {
            throw new BusinessException("Mã lớp không hợp lệ");
        }
        return enrollClassSectionByCode(classCode);
    }

    @Override
    public EnrollmentResponse enrollClassSection(Integer classSectionId) {
        User currentUser = userService.getCurrentUser();
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));
        return enrollCurrentUserInClassSection(currentUser, classSection);
    }

    @Override
    public EnrollmentResponse enrollClassSectionByCode(String classCode) {
        User currentUser = userService.getCurrentUser();
        if (!userRepository.existsById(currentUser.getId())) {
            throw new ResourceNotFoundException("Không tìm thấy người dùng");
        }
        if (!StringUtils.hasText(classCode)) {
            throw new BusinessException("Mã lớp không hợp lệ");
        }

        ClassSection classSection = classSectionRepository.findByClassCode(classCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học!"));
        return enrollCurrentUserInClassSection(currentUser, classSection);
    }

    @Transactional
    @Override
    public void completeLesson(Integer chapterItemId) {
        throw new UnsupportedOperationException("Legacy course lesson flow has been removed");
    }

    @Transactional
    @Override
    public void completeClassContentItem(Integer classContentItemId) {
        User currentUser = userService.getCurrentUser();
        ClassContentItem classContentItem = classContentItemRepository.findById(classContentItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Nội dung lớp học không tồn tại"));

        if (classContentItem.getItemType() != ContentItemType.LESSON) {
            throw new BusinessException("Chi bai hoc moi duoc danh dau hoan thanh thu cong");
        }

        var classSection = classContentItem.getClassChapter().getClassSection();
        ClassSectionGuard.ensureInteractive(classSection);
        Integer classSectionId = classSection.getId();
        boolean isEnrolled = enrollmentRepository.existsByStudent_IdAndClassSection_IdAndApprovalStatus(
                currentUser.getId(), classSectionId, EnrollmentStatus.APPROVED);
        if (!isEnrolled) {
            throw new UnauthorizedException("Bạn chưa đăng ký hoặc chưa được duyệt vào lớp học này");
        }

        Progress progress = progressRepository
                .findByStudent_IdAndClassContentItem_Id(currentUser.getId(), classContentItemId)
                .orElse(Progress.builder()
                        .student(currentUser)
                        .classContentItem(classContentItem)
                        .isCompleted(false)
                        .build());

        if (!Boolean.TRUE.equals(progress.getIsCompleted())) {
            progress.setIsCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
            progressRepository.save(progress);
            recalculateAndSaveProgressForClassSection(currentUser.getId(), classSectionId);
            cacheInvalidationService.evictAllRedisReadCaches();
        }
    }

    @Override
    public EnrollmentResponse approveStudentToEnrollment(EnrollmentRequest request) {
        User currentUser = userService.getCurrentUser();
        User student = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng!"));

        validateEnrollmentTarget(request);
        ensureStudentIsNotClassStaff(request, student);
        Enrollment enrollment = findPendingEnrollment(request);
        if (enrollment == null) {
            throw new ResourceNotFoundException("Yêu cầu tham gia không tồn tại hoặc đã được xử lý!");
        }
        if (!isEnrollmentOwner(enrollment, currentUser)) {
            throw new UnauthorizedException("Bạn không có quyền phê duyệt yêu cầu này!");
        }

        enrollment.setApprovalStatus(EnrollmentStatus.APPROVED);
        enrollment.setApprovedAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);
        cacheInvalidationService.evictAllRedisReadCaches();

        String message = "Bạn đã được phê duyệt tham gia: " + resolveEnrollmentTargetTitle(enrollment);
        notificationService.createNotification(student, "Yêu cầu đã được phê duyệt", message, "ENROLLMENT", null, null);
        return convertEnrollmentToDTO(enrollment);
    }

    @Override
    public void rejectStudentEnrollment(EnrollmentRequest request) {
        User currentUser = userService.getCurrentUser();
        User student = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        validateEnrollmentTarget(request);
        Enrollment enrollment = findEnrollment(request);
        if (enrollment == null) {
            throw new ResourceNotFoundException("Không tìm thấy thông tin đăng ký của người dùng này!");
        }
        if (!isEnrollmentOwner(enrollment, currentUser)) {
            throw new UnauthorizedException("Bạn không có quyền từ chối yêu cầu này!");
        }

        String title = resolveEnrollmentTargetTitle(enrollment);
        enrollmentRepository.delete(enrollment);
        cacheInvalidationService.evictAllRedisReadCaches();
        String message = "Yêu cầu tham gia của bạn đã bị từ chối: " + title;
        notificationService.createNotification(student, "Yêu cầu bị từ chối", message, "ENROLLMENT_REJECTED", null, null);
    }

    @Override
    public PageResponse<EnrollmentResponse> getStudentsApprovedInEnrollment(Integer courseId, Pageable pageable) {
        throw new UnsupportedOperationException("Legacy course enrollment flow has been removed");
    }

    public PageResponse<EnrollmentResponse> getStudentsApprovedInClassSection(Integer classSectionId, Pageable pageable) {
        return getStudentsApprovedInClassSection(classSectionId, null, pageable);
    }

    @Cacheable(value = CacheNames.ENROLLMENT_APPROVED_CLASS_SECTION, key = "@cacheKeyBuilder.approvedClassSectionEnrollmentKey(#classSectionId, #keyword, #pageable)", sync = true)
    @Override
    public PageResponse<EnrollmentResponse> getStudentsApprovedInClassSection(Integer classSectionId, String keyword, Pageable pageable) {
        if (!classSectionRepository.existsById(classSectionId)) {
            throw new ResourceNotFoundException("Không tìm thấy lớp học");
        }
        assertCanViewClassSectionRoster(classSectionId);
        User currentUser = userService.getCurrentUser();
        boolean includeProgress = currentUser == null
                || currentUser.getRole() == null
                || currentUser.getRole().getRoleName() != RoleType.STUDENT;
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        Page<Enrollment> enrollmentPage = enrollmentRepository.searchByClassSection_IdAndApprovalStatus(
                classSectionId,
                EnrollmentStatus.APPROVED,
                normalizedKeyword,
                pageable
        );
        Page<EnrollmentResponse> enrollmentResponse = enrollmentPage.map(enrollment -> convertEnrollmentToDTO(enrollment, includeProgress));
        return new PageResponse<>(
                enrollmentResponse.getNumber() + 1,
                enrollmentResponse.getTotalPages(),
                (int) enrollmentResponse.getTotalElements(),
                enrollmentResponse.getContent()
        );
    }

    @Override
    public PageResponse<EnrollmentResponse> getStudentsPendingEnrollment(Integer courseId, Pageable pageable) {
        throw new UnsupportedOperationException("Legacy course enrollment flow has been removed");
    }

    @Override
    @Cacheable(value = CacheNames.ENROLLMENT_PENDING_CLASS_SECTION, key = "@cacheKeyBuilder.pendingClassSectionEnrollmentKey(#classSectionId, #pageable)", sync = true)
    public PageResponse<EnrollmentResponse> getStudentsPendingInClassSection(Integer classSectionId, Pageable pageable) {
        if (!classSectionRepository.existsById(classSectionId)) {
            throw new ResourceNotFoundException("Không tìm thấy lớp học");
        }
        Page<Enrollment> enrollmentPage = enrollmentRepository.findByClassSection_IdAndApprovalStatus(classSectionId, EnrollmentStatus.PENDING, pageable);
        return convertToPageResponse(enrollmentPage);
    }

    private PageResponse<EnrollmentResponse> convertToPageResponse(Page<Enrollment> enrollmentPage) {
        Page<EnrollmentResponse> enrollmentResponse = enrollmentPage.map(this::convertEnrollmentToDTO);
        return new PageResponse<>(
                enrollmentResponse.getNumber() + 1,
                enrollmentResponse.getTotalPages(),
                (int) enrollmentResponse.getTotalElements(),
                enrollmentResponse.getContent()
        );
    }

    @Override
    public EnrollmentResponse getCurrentUserProgressByCourse(Integer courseId) {
        throw new UnsupportedOperationException("Legacy course enrollment flow has been removed");
    }

    @Override
    public EnrollmentResponse getCurrentUserProgressByClassSection(Integer classSectionId) {
        User currentStudent = userService.getCurrentUser();
        Enrollment enrollment = enrollmentRepository.findByStudent_IdAndClassSection_IdAndApprovalStatus(
                currentStudent.getId(),
                classSectionId,
                EnrollmentStatus.APPROVED
        );
        if (enrollment == null) {
            throw new UnauthorizedException("Bạn không nằm trong lớp học này!");
        }
        return convertEnrollmentToDTO(enrollment);
    }

    @Override
    public EnrollmentResponse getEnrollmentById(Integer id) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài nguyên"));
        return convertEnrollmentToDTO(enrollment);
    }

    @Override
    public PageResponse<EnrollmentResponse> getEnrollmentPage(Pageable pageable) {
        Page<Enrollment> enrollmentPage = enrollmentRepository.findAll(pageable);
        Page<EnrollmentResponse> enrollmentResponse = enrollmentPage.map(this::convertEnrollmentToDTO);
        return new PageResponse<>(
                enrollmentResponse.getNumber() + 1,
                enrollmentResponse.getTotalPages(),
                (int) enrollmentResponse.getTotalElements(),
                enrollmentResponse.getContent()
        );
    }

    @Override
    public PageResponse<EnrollmentResponse> getEnrollmentPage(String approvalStatus, Pageable pageable) {
        if (approvalStatus == null || approvalStatus.isBlank()) {
            return getEnrollmentPage(pageable);
        }
        EnrollmentStatus status = EnrollmentStatus.valueOf(approvalStatus.toUpperCase());
        Page<Enrollment> enrollmentPage = enrollmentRepository.findByApprovalStatus(status, pageable);
        Page<EnrollmentResponse> enrollmentResponse = enrollmentPage.map(this::convertEnrollmentToDTO);
        return new PageResponse<>(
                enrollmentResponse.getNumber() + 1,
                enrollmentResponse.getTotalPages(),
                (int) enrollmentResponse.getTotalElements(),
                enrollmentResponse.getContent()
        );
    }

    @Override
    public PageResponse<UserViewResponse> searchStudentsInCourse(Integer courseId, SearchUserRequest request, Pageable pageable) {
        throw new UnsupportedOperationException("Legacy course enrollment flow has been removed");
    }

    @Override
    public PageResponse<UserViewResponse> searchStudentsNotInCourse(Integer courseId, SearchUserRequest request, Pageable pageable) {
        throw new UnsupportedOperationException("Legacy course enrollment flow has been removed");
    }

    @Override
    public PageResponse<UserViewResponse> searchStudentsNotInClassSection(Integer classSectionId, SearchUserRequest request, Pageable pageable) {
        Specification<User> spec = buildBaseUserSearchSpec(request);
        spec = spec.and(UserSpecification.hasRole(RoleType.STUDENT));
        spec = spec.and(UserSpecification.notInClassSection(classSectionId));
        Page<User> userPage = userRepository.findAll(spec, pageable);
        Page<UserViewResponse> response = userPage.map(userService::convertUserViewToDTO);
        return new PageResponse<>(
                response.getNumber() + 1,
                (int) response.getTotalElements(),
                response.getTotalPages(),
                response.getContent()
        );
    }

    @Override
    public void recalculateAndSaveProgress(Integer studentId, Integer courseId) {
        // Legacy course-based progress recalculation has been retired.
        // Keep this method as a no-op for backward compatibility with any
        // remaining internal legacy calls.
    }

    @Override
    public void recalculateAndSaveProgressForClassSection(Integer studentId, Integer classSectionId) {
        Enrollment enrollment = enrollmentRepository.findByStudent_IdAndClassSection_IdAndApprovalStatus(
                studentId,
                classSectionId,
                EnrollmentStatus.APPROVED
        );
        if (enrollment == null) {
            return;
        }

        long totalItems = classContentItemRepository.countLearningItemsByClassSectionId(classSectionId);
        if (totalItems == 0) {
            enrollment.setProgress(0);
            enrollmentRepository.save(enrollment);
            return;
        }

        long completedItems = progressRepository.countCompletedClassItemsByStudentAndClassSection(studentId, classSectionId);
        int percent = (int) Math.round(((double) completedItems / totalItems) * 100);
        if (percent > 100) {
            percent = 100;
        }

        enrollment.setProgress(percent);
        enrollmentRepository.save(enrollment);
    }

    @Override
    @Cacheable(value = CacheNames.ENROLLMENT_TEACHER, key = "@cacheKeyBuilder.teacherEnrollmentsKey(#classSectionId, #approvalStatus, #pageable)", sync = true)
    public PageResponse<EnrollmentResponse> getTeacherEnrollments(Integer classSectionId, String approvalStatus, Pageable pageable) {
        Integer teacherId = userService.getCurrentUser().getId();

        // Resolve all classSectionIds where this user is a TEACHER
        // (via ClassMember table and/or direct teacher_id field)
        List<ClassSection> classSectionsViaMember = classMemberRepository
                .findClassSectionsByMemberRole(teacherId, ClassMemberRole.TEACHER);
        List<Integer> teacherClassSectionIds = classSectionsViaMember.stream()
                .map(ClassSection::getId)
                .collect(java.util.stream.Collectors.toList());

        // Also include class sections where teacher_id matches directly
        List<ClassSection> directTeacherSections = classSectionRepository.findByTeacher_Id(teacherId);
        for (ClassSection cs : directTeacherSections) {
            if (!teacherClassSectionIds.contains(cs.getId())) {
                teacherClassSectionIds.add(cs.getId());
            }
        }

        Page<Enrollment> page;

        if (classSectionId != null) {
            // Filter by specific classSectionId — verify teacher owns it
            if (!teacherClassSectionIds.contains(classSectionId)) {
                // Teacher doesn't own this class section — return empty
                return new PageResponse<>(pageable.getPageNumber() + 1, 0, 0, List.of());
            }
            if (approvalStatus != null) {
                EnrollmentStatus status = EnrollmentStatus.valueOf(approvalStatus.toUpperCase());
                page = enrollmentRepository.findByClassSection_IdAndApprovalStatus(classSectionId, status, pageable);
            } else {
                page = enrollmentRepository.findByClassSection_Id(classSectionId, pageable);
            }
        } else if (!teacherClassSectionIds.isEmpty()) {
            // No specific classSectionId — get enrollments from all teacher's class sections
            if (approvalStatus != null) {
                EnrollmentStatus status = EnrollmentStatus.valueOf(approvalStatus.toUpperCase());
                page = enrollmentRepository.findByClassSection_IdInAndApprovalStatus(teacherClassSectionIds, status, pageable);
            } else {
                page = enrollmentRepository.findByClassSection_IdIn(teacherClassSectionIds, pageable);
            }
        } else {
            // Teacher has no class sections at all — return empty
            return new PageResponse<>(pageable.getPageNumber() + 1, 0, 0, List.of());
        }

        return convertToPageResponse(page);
    }

    private Specification<User> buildBaseUserSearchSpec(SearchUserRequest request) {
        Specification<User> spec = (root, query, cb) -> cb.conjunction();

        if (StringUtils.hasText(request.getUserName())) {
            spec = spec.and(UserSpecification.likeUserName(request.getUserName()));
        }
        if (StringUtils.hasText(request.getFullName())) {
            spec = spec.and(UserSpecification.likeFullName(request.getFullName()));
        }
        if (StringUtils.hasText(request.getStudentNumber())) {
            spec = spec.and(UserSpecification.hasStudentNumber(request.getStudentNumber()));
        }
        if (StringUtils.hasText(request.getGmail())) {
            spec = spec.and(UserSpecification.likeGmail(request.getGmail()));
        }
        return spec;
    }

    private EnrollmentResponse convertEnrollmentToDTO(Enrollment enrollment) {
        return convertEnrollmentToDTO(enrollment, true);
    }

    private EnrollmentResponse enrollCurrentUserInClassSection(User currentUser, ClassSection classSection) {
        if (classSection.getStatus() == ClassSectionStatus.ARCHIVED) {
            throw new BusinessException("Lớp học đã được lưu trữ, bạn không thể tham gia!");
        }
        ensureCurrentUserIsNotClassStaff(currentUser, classSection);

        Enrollment existingEnrollment = enrollmentRepository.findByStudent_IdAndClassSection_Id(
                currentUser.getId(),
                classSection.getId()
        );
        if (existingEnrollment != null) {
            if (existingEnrollment.getApprovalStatus() == EnrollmentStatus.APPROVED) {
                throw new BusinessException("Bạn đã tham gia lớp học này!");
            }
            if (existingEnrollment.getApprovalStatus() == EnrollmentStatus.PENDING) {
                throw new BusinessException("Hãy đợi giảng viên xét duyet yêu cầu của bạn!");
            }
        }

        EnrollmentStatus targetStatus = classSection.getStatus() == ClassSectionStatus.PUBLIC
                ? EnrollmentStatus.APPROVED
                : EnrollmentStatus.PENDING;

        Enrollment newEnrollment = Enrollment.builder()
                .student(currentUser)
                .classSection(classSection)
                .progress(0)
                .approvalStatus(targetStatus)
                .approvedAt(targetStatus == EnrollmentStatus.APPROVED ? LocalDateTime.now() : null)
                .build();
        enrollmentRepository.save(newEnrollment);

        if (targetStatus == EnrollmentStatus.PENDING) {
            User notifyUser = classMemberAuthorizationService.resolvePrimaryTeacher(classSection);
            if (notifyUser != null) {
                String message = "Người học " + currentUser.getFullName()
                        + " đã yêu cầu tham gia lớp học: " + classSection.getTitle();
                notificationService.createNotification(
                        notifyUser,
                        "Yêu cầu tham gia lớp học",
                        message,
                        "CLASS_SECTION_ENROLLMENT_REQUEST",
                        null,
                        null
                );
            }
        }

        cacheInvalidationService.evictAllRedisReadCaches();
        return convertEnrollmentToDTO(newEnrollment);
    }

    private EnrollmentResponse convertEnrollmentToDTO(Enrollment enrollment, boolean includeProgress) {
        ClassSection classSection = enrollment.getClassSection();

        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .studentId(enrollment.getStudent() != null ? enrollment.getStudent().getId() : null)
                .userName(enrollment.getStudent() != null ? enrollment.getStudent().getUserName() : null)
                .fullName(enrollment.getStudent() != null ? enrollment.getStudent().getFullName() : null)
                .studentNumber(enrollment.getStudent() != null ? enrollment.getStudent().getStudentNumber() : null)
                .email(enrollment.getStudent() != null ? enrollment.getStudent().getGmail() : null)
                .studentAvatar(enrollment.getStudent() != null ? enrollment.getStudent().getImageUrl() : null)
                .courseTitle(null)
                .courseCode(null)
                .classSectionTitle(classSection != null ? classSection.getTitle() : null)
                .classSectionCode(classSection != null ? classSection.getClassCode() : null)
                .classSectionId(classSection != null ? classSection.getId() : null)
                .progress(includeProgress ? enrollment.getProgress() : null)
                .approvalStatus(enrollment.getApprovalStatus() != null ? enrollment.getApprovalStatus().toString() : null)
                .createdAt(enrollment.getCreatedDate())
                .approvedAt(enrollment.getApprovedAt())
                .build();
    }

    private void assertCanViewClassSectionRoster(Integer classSectionId) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        if (currentUser.getRole() != null && currentUser.getRole().getRoleName() == RoleType.ADMIN) {
            return;
        }

        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));
        if (classMemberAuthorizationService.isTeacherOrTa(classSection, currentUser)) {
            return;
        }
        if (currentUser.getRole() != null
                && currentUser.getRole().getRoleName() == RoleType.STUDENT
                && enrollmentRepository.existsByStudent_IdAndClassSection_IdAndApprovalStatus(
                        currentUser.getId(),
                        classSectionId,
                        EnrollmentStatus.APPROVED
                )) {
            return;
        }
        throw new UnauthorizedException("Bạn không có quyền xem danh sách học viên của lớp này");
    }

    private void validateEnrollmentTarget(EnrollmentRequest request) {
        if (request.getClassSectionId() == null) {
            throw new BusinessException("Class section id is required");
        }
    }

    private void ensureCurrentUserIsNotClassStaff(User currentUser, ClassSection classSection) {
        if (currentUser == null || classSection == null) {
            return;
        }
        if (classMemberAuthorizationService.isTeacherOrTa(classSection, currentUser)) {
            throw new BusinessException("Không thể đăng ký vào lớp khi bạn đang giữ vai trò giảng dạy trong chính lớp này");
        }
    }

    private void ensureUsersAreNotClassStaff(ClassSection classSection, List<User> users) {
        if (classSection == null || users == null || users.isEmpty()) {
            return;
        }
        for (User user : users) {
            ensureUserIsNotClassStaff(classSection, user);
        }
    }

    private void ensureUserIsNotClassStaff(ClassSection classSection, User user) {
        if (user == null || classSection == null) {
            return;
        }
        if (classMemberAuthorizationService.isTeacherOrTa(classSection, user)) {
            throw new BusinessException("Không thể thêm học viên vào lớp khi người dùng đã là giảng viên hoặc trợ giảng của lớp này");
        }
    }

    private void ensureStudentIsNotClassStaff(EnrollmentRequest request, User student) {
        if (student == null || request == null || request.getClassSectionId() == null) {
            return;
        }
        ClassSection classSection = classSectionRepository.findById(request.getClassSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học!"));
        ensureUserIsNotClassStaff(classSection, student);
    }

    private Enrollment findPendingEnrollment(EnrollmentRequest request) {
        return enrollmentRepository.findByStudent_IdAndClassSection_IdAndApprovalStatus(
                request.getStudentId(),
                request.getClassSectionId(),
                EnrollmentStatus.PENDING
        );
    }

    private Enrollment findEnrollment(EnrollmentRequest request) {
        return enrollmentRepository.findByStudent_IdAndClassSection_Id(
                request.getStudentId(),
                request.getClassSectionId()
        );
    }

    private boolean isEnrollmentOwner(Enrollment enrollment, User currentUser) {
        if (currentUser.getRole().getRoleName().equals(RoleType.ADMIN)) {
            return true;
        }

        ClassSection classSection = enrollment.getClassSection();
        if (classSection == null) {
            return false;
        }
        return classMemberAuthorizationService.isTeacher(classSection, currentUser);
    }

    private String resolveEnrollmentTargetTitle(Enrollment enrollment) {
        if (enrollment.getClassSection() != null) {
            return enrollment.getClassSection().getTitle();
        }
        return "unknown target";
    }
}
