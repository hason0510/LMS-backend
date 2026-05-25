package com.example.backend.service.impl;

import com.example.backend.constant.ClassMemberRole;
import com.example.backend.constant.ContentItemType;
import com.example.backend.constant.ClassSectionStatus;
import com.example.backend.constant.CourseStatus;
import com.example.backend.constant.EnrollmentStatus;
import com.example.backend.constant.ItemType;
import com.example.backend.constant.RoleType;
import com.example.backend.dto.request.EnrollmentRequest;
import com.example.backend.dto.request.course.StudentCourseRequest;
import com.example.backend.dto.request.search.SearchUserRequest;
import com.example.backend.dto.response.EnrollmentResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.user.UserViewResponse;
import com.example.backend.entity.old.ChapterItem;
import com.example.backend.entity.ClassContentItem;
import com.example.backend.entity.ClassSection;
import com.example.backend.entity.old.Course;
import com.example.backend.entity.Enrollment;
import com.example.backend.entity.Progress;
import com.example.backend.entity.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.repository.old.ChapterItemRepository;
import com.example.backend.repository.ClassContentItemRepository;
import com.example.backend.repository.ClassMemberRepository;
import com.example.backend.repository.ClassSectionRepository;
import com.example.backend.repository.old.CourseRepository;
import com.example.backend.repository.EnrollmentRepository;
import com.example.backend.repository.ProgressRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.ClassMemberAuthorizationService;
import com.example.backend.service.old.CourseService;
import com.example.backend.service.EnrollmentService;
import com.example.backend.service.NotificationService;
import com.example.backend.service.UserService;
import com.example.backend.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
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
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final CourseService courseService;
    private final ProgressRepository progressRepository;
    private final ChapterItemRepository chapterItemRepository;
    private final ClassSectionRepository classSectionRepository;
    private final ClassContentItemRepository classContentItemRepository;
    private final ClassMemberRepository classMemberRepository;
    private final ClassMemberAuthorizationService classMemberAuthorizationService;

    @Transactional
    @Override
    public void addStudentsToCourse(Integer courseId, StudentCourseRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay khoa hoc!"));

        List<User> users = userRepository.findAllById(request.getStudentIds());
        List<Enrollment> progresses = users.stream()
                .map(user -> Enrollment.builder()
                        .student(user)
                        .course(course)
                        .progress(0)
                        .approvalStatus(EnrollmentStatus.APPROVED)
                        .build())
                .toList();
        enrollmentRepository.saveAll(progresses);

        for (User user : users) {
            String message = "Ban da duoc them vao khoa hoc " + course.getTitle();
            notificationService.createNotification(user, "Duoc them vao khoa hoc", message, "ENROLLMENT", null, null);
        }
    }

    @Transactional
    @Override
    public void addStudentsToClassSection(Integer classSectionId, StudentCourseRequest request) {
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay lop hoc!"));

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

        for (User user : users) {
            String message = "Ban da duoc them vao lop hoc " + classSection.getTitle();
            notificationService.createNotification(user, "Duoc them vao lop hoc", message, "ENROLLMENT", null, null);
        }
    }

    @Transactional
    @Override
    public void removeStudentsInCourse(Integer courseId, StudentCourseRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay khoa hoc!"));
        if (request.getStudentIds() == null || request.getStudentIds().isEmpty()) {
            return;
        }

        List<User> users = userRepository.findAllById(request.getStudentIds());
        List<Enrollment> progresses = enrollmentRepository.findByCourse_IdAndStudent_IdIn(courseId, request.getStudentIds());
        enrollmentRepository.deleteAll(progresses);

        for (User user : users) {
            String message = "Ban da bi xoa khoi danh sach lop " + course.getTitle();
            notificationService.createNotification(user, "Bi xoa khoi khoa hoc", message, "ENROLLMENT", null, null);
        }
    }

    @Transactional
    @Override
    public void removeStudentsFromClassSection(Integer classSectionId, StudentCourseRequest request) {
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay lop hoc!"));
        if (request.getStudentIds() == null || request.getStudentIds().isEmpty()) {
            return;
        }

        List<User> users = userRepository.findAllById(request.getStudentIds());
        List<Enrollment> enrollments = enrollmentRepository.findByClassSection_IdAndStudent_IdIn(classSectionId, request.getStudentIds());
        enrollmentRepository.deleteAll(enrollments);

        for (User user : users) {
            String message = "Ban da bi xoa khoi danh sach lop " + classSection.getTitle();
            notificationService.createNotification(user, "Bi xoa khoi lop hoc", message, "ENROLLMENT", null, null);
        }
    }

    @Override
    public EnrollmentResponse enrollPublicCourse(Integer courseId) {
        User currentUser = userService.getCurrentUser();
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay khoa hoc!"));

        if (course.getStatus() == CourseStatus.PRIVATE) {
            throw new UnauthorizedException("Ban khong duoc truy cap vao tai nguyen nay!");
        }

        Enrollment existingEnrollment = enrollmentRepository.findByStudent_IdAndCourse_Id(currentUser.getId(), courseId);
        if (existingEnrollment != null) {
            if (existingEnrollment.getApprovalStatus() == EnrollmentStatus.APPROVED) {
                throw new BusinessException("Ban da tham gia khoa hoc nay!");
            }
            if (existingEnrollment.getApprovalStatus() == EnrollmentStatus.PENDING) {
                throw new BusinessException("Hay doi giao vien xet duyet yeu cau cua ban!");
            }
        }

        Enrollment newEnrollment = Enrollment.builder()
                .student(currentUser)
                .course(course)
                .progress(0)
                .approvalStatus(EnrollmentStatus.PENDING)
                .build();
        enrollmentRepository.save(newEnrollment);

        String message = "Sinh vien " + currentUser.getFullName() + " da yeu cau tham gia khoa hoc: " + course.getTitle();
        notificationService.createNotification(course.getTeacher(), "Yeu cau tham gia khoa hoc", message, "ENROLLMENT_REQUEST", null, null);
        return convertEnrollmentToDTO(newEnrollment);
    }

    @Override
    public EnrollmentResponse enrollPrivateCourse(String classCode) {
        User currentUser = userService.getCurrentUser();
        if (!userRepository.existsById(currentUser.getId())) {
            throw new ResourceNotFoundException("Khong tim thay nguoi dung");
        }

        if (!StringUtils.hasText(classCode)) {
            throw new BusinessException("Ma lop khong hop le");
        }

        String normalizedClassCode = classCode.trim();

        // Prefer class section flow (new domain model), fallback to legacy course flow for old data.
        ClassSection classSection = classSectionRepository.findByClassCode(normalizedClassCode).orElse(null);
        if (classSection != null) {
            if (classSection.getStatus() == ClassSectionStatus.ARCHIVED) {
                throw new BusinessException("Lop hoc da luu tru, khong the tham gia");
            }
            ensureCurrentUserIsNotClassStaff(currentUser, classSection);

            Enrollment existingEnrollment = enrollmentRepository.findByStudent_IdAndClassSection_Id(
                    currentUser.getId(),
                    classSection.getId()
            );
            if (existingEnrollment != null) {
                if (existingEnrollment.getApprovalStatus() == EnrollmentStatus.APPROVED) {
                    throw new BusinessException("Ban da tham gia lop hoc nay!");
                }
                if (existingEnrollment.getApprovalStatus() == EnrollmentStatus.PENDING) {
                    throw new BusinessException("Hay doi giao vien xet duyet yeu cau cua ban!");
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
                    .build();
            enrollmentRepository.save(newEnrollment);

            if (targetStatus == EnrollmentStatus.PENDING) {
                User notifyUser = classMemberAuthorizationService.resolvePrimaryTeacher(classSection);
                if (notifyUser != null) {
                    String message = "Sinh vien " + currentUser.getFullName()
                            + " da yeu cau tham gia lop hoc: " + classSection.getTitle();
                    notificationService.createNotification(
                            notifyUser,
                            "Yeu cau tham gia lop hoc",
                            message,
                            "CLASS_SECTION_ENROLLMENT_REQUEST",
                            null,
                            null
                    );
                }
            }
            return convertEnrollmentToDTO(newEnrollment);
        }

        Course course = courseRepository.findByClassCode(normalizedClassCode)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay lop hoc!"));
        if (enrollmentRepository.findByStudent_IdAndCourse_IdAndApprovalStatus(
                currentUser.getId(), course.getId(), EnrollmentStatus.APPROVED) != null) {
            throw new BusinessException("Ban da tham gia khoa hoc nay!");
        }
        if (enrollmentRepository.findByStudent_IdAndCourse_IdAndApprovalStatus(
                currentUser.getId(), course.getId(), EnrollmentStatus.PENDING) != null) {
            throw new BusinessException("Hay doi giao vien xet duyet yeu cau cua ban");
        }

        Enrollment newEnrollment = Enrollment.builder()
                .student(currentUser)
                .course(course)
                .progress(0)
                .approvalStatus(EnrollmentStatus.PENDING)
                .build();
        enrollmentRepository.save(newEnrollment);

        String message = "Sinh vien " + currentUser.getFullName() + " da yeu cau tham gia khoa hoc: " + course.getTitle();
        notificationService.createNotification(course.getTeacher(), "Yeu cau tham gia khoa hoc", message, "ENROLLMENT_REQUEST", null, null);
        return convertEnrollmentToDTO(newEnrollment);
    }

    @Override
    public EnrollmentResponse enrollClassSection(Integer classSectionId) {
        User currentUser = userService.getCurrentUser();
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay lop hoc"));
        ensureCurrentUserIsNotClassStaff(currentUser, classSection);

        Enrollment existingEnrollment = enrollmentRepository.findByStudent_IdAndClassSection_Id(currentUser.getId(), classSectionId);
        if (existingEnrollment != null) {
            if (existingEnrollment.getApprovalStatus() == EnrollmentStatus.APPROVED) {
                throw new BusinessException("Ban da tham gia lop hoc nay!");
            }
            if (existingEnrollment.getApprovalStatus() == EnrollmentStatus.PENDING) {
                throw new BusinessException("Hay doi giao vien xet duyet yeu cau cua ban!");
            }
        }

        Enrollment newEnrollment = Enrollment.builder()
                .student(currentUser)
                .classSection(classSection)
                .progress(0)
                .approvalStatus(classSection.getStatus() == ClassSectionStatus.PUBLIC
                        ? EnrollmentStatus.APPROVED
                        : EnrollmentStatus.PENDING)
                .build();
        enrollmentRepository.save(newEnrollment);

        if (newEnrollment.getApprovalStatus() == EnrollmentStatus.PENDING) {
            User notifyUser = classMemberAuthorizationService.resolvePrimaryTeacher(classSection);
            if (notifyUser != null) {
                String message = "Sinh vien " + currentUser.getFullName() + " da yeu cau tham gia lop hoc: " + classSection.getTitle();
                notificationService.createNotification(notifyUser, "Yeu cau tham gia lop hoc", message, "CLASS_SECTION_ENROLLMENT_REQUEST", null, null);
            }
        }

        return convertEnrollmentToDTO(newEnrollment);
    }

    @Transactional
    @Override
    public void completeLesson(Integer chapterItemId) {
        User currentUser = userService.getCurrentUser();
        ChapterItem chapterItem = chapterItemRepository.findById(chapterItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Bai giang khong ton tai"));
        Integer courseId = chapterItem.getChapter().getCourse().getId();

        if (chapterItem.getType() != ItemType.LESSON) {
            throw new BusinessException("Day khong phai la bai giang!");
        }

        boolean isEnrolled = enrollmentRepository.existsByStudent_IdAndCourse_IdAndApprovalStatus(
                currentUser.getId(), courseId, EnrollmentStatus.APPROVED);
        if (!isEnrolled) {
            throw new UnauthorizedException("Ban chua dang ky hoac chua duoc duyet vao khoa hoc nay");
        }

        Progress progress = progressRepository
                .findByStudent_IdAndChapterItem_Id(currentUser.getId(), chapterItem.getId())
                .orElse(Progress.builder()
                        .student(currentUser)
                        .chapterItem(chapterItem)
                        .isCompleted(false)
                        .build());

        if (!Boolean.TRUE.equals(progress.getIsCompleted())) {
            progress.setIsCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
            progressRepository.save(progress);

            if (chapterItem.getChapter() != null && chapterItem.getChapter().getCourse() != null) {
                recalculateAndSaveProgress(currentUser.getId(), chapterItem.getChapter().getCourse().getId());
            }
        }
    }

    @Transactional
    @Override
    public void completeClassContentItem(Integer classContentItemId) {
        User currentUser = userService.getCurrentUser();
        ClassContentItem classContentItem = classContentItemRepository.findById(classContentItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Noi dung lop hoc khong ton tai"));

        if (classContentItem.getItemType() != ContentItemType.LESSON) {
            throw new BusinessException("Chi bai hoc moi duoc danh dau hoan thanh thu cong");
        }

        Integer classSectionId = classContentItem.getClassChapter().getClassSection().getId();
        boolean isEnrolled = enrollmentRepository.existsByStudent_IdAndClassSection_IdAndApprovalStatus(
                currentUser.getId(), classSectionId, EnrollmentStatus.APPROVED);
        if (!isEnrolled) {
            throw new UnauthorizedException("Ban chua dang ky hoac chua duoc duyet vao lop hoc nay");
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
        }
    }

    @Override
    public EnrollmentResponse approveStudentToEnrollment(EnrollmentRequest request) {
        User currentUser = userService.getCurrentUser();
        User student = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay nguoi dung!"));

        validateEnrollmentTarget(request);
        ensureStudentIsNotClassStaff(request, student);
        Enrollment enrollment = findPendingEnrollment(request);
        if (enrollment == null) {
            throw new ResourceNotFoundException("Yeu cau tham gia khong ton tai hoac da duoc xu ly!");
        }
        if (!isEnrollmentOwner(enrollment, currentUser)) {
            throw new UnauthorizedException("Ban khong co quyen phe duyet yeu cau nay!");
        }

        enrollment.setApprovalStatus(EnrollmentStatus.APPROVED);
        enrollmentRepository.save(enrollment);

        String message = "Ban da duoc phe duyet tham gia: " + resolveEnrollmentTargetTitle(enrollment);
        notificationService.createNotification(student, "Yeu cau da duoc phe duyet", message, "ENROLLMENT", null, null);
        return convertEnrollmentToDTO(enrollment);
    }

    @Override
    public void rejectStudentEnrollment(EnrollmentRequest request) {
        User currentUser = userService.getCurrentUser();
        User student = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay nguoi dung!"));

        validateEnrollmentTarget(request);
        Enrollment enrollment = findEnrollment(request);
        if (enrollment == null) {
            throw new ResourceNotFoundException("Khong tim thay thong tin dang ky cua nguoi dung nay!");
        }
        if (!isEnrollmentOwner(enrollment, currentUser)) {
            throw new UnauthorizedException("Ban khong co quyen tu choi yeu cau nay!");
        }

        String title = resolveEnrollmentTargetTitle(enrollment);
        enrollmentRepository.delete(enrollment);
        String message = "Yeu cau tham gia cua ban da bi tu choi: " + title;
        notificationService.createNotification(student, "Yeu cau bi tu choi", message, "ENROLLMENT_REJECTED", null, null);
    }

    @Override
    public PageResponse<EnrollmentResponse> getStudentsApprovedInEnrollment(Integer courseId, Pageable pageable) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Khong tim thay khoa hoc");
        }
        Page<Enrollment> enrollmentPage = enrollmentRepository.findByCourse_IdAndApprovalStatus(courseId, EnrollmentStatus.APPROVED, pageable);
        return convertToPageResponse(enrollmentPage);
    }

    @Override
    public PageResponse<EnrollmentResponse> getStudentsApprovedInClassSection(Integer classSectionId, Pageable pageable) {
        if (!classSectionRepository.existsById(classSectionId)) {
            throw new ResourceNotFoundException("Khong tim thay lop hoc");
        }
        Page<Enrollment> enrollmentPage = enrollmentRepository.findByClassSection_IdAndApprovalStatus(classSectionId, EnrollmentStatus.APPROVED, pageable);
        return convertToPageResponse(enrollmentPage);
    }

    @Override
    public PageResponse<EnrollmentResponse> getStudentsPendingEnrollment(Integer courseId, Pageable pageable) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Khong tim thay khoa hoc");
        }
        Page<Enrollment> enrollmentPage = enrollmentRepository.findByCourse_IdAndApprovalStatus(courseId, EnrollmentStatus.PENDING, pageable);
        return convertToPageResponse(enrollmentPage);
    }

    @Override
    public PageResponse<EnrollmentResponse> getStudentsPendingInClassSection(Integer classSectionId, Pageable pageable) {
        if (!classSectionRepository.existsById(classSectionId)) {
            throw new ResourceNotFoundException("Khong tim thay lop hoc");
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
        User currentStudent = userService.getCurrentUser();
        Enrollment enrollment = enrollmentRepository.findByStudent_IdAndCourse_IdAndApprovalStatus(
                currentStudent.getId(),
                courseId,
                EnrollmentStatus.APPROVED
        );
        if (enrollment == null) {
            throw new UnauthorizedException("Ban khong nam trong khoa hoc nay!");
        }
        return convertEnrollmentToDTO(enrollment);
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
            throw new UnauthorizedException("Ban khong nam trong lop hoc nay!");
        }
        return convertEnrollmentToDTO(enrollment);
    }

    @Override
    public EnrollmentResponse getEnrollmentById(Integer id) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay tai nguyen"));
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
        Specification<User> spec = buildBaseUserSearchSpec(request);
        spec = spec.and(UserSpecification.hasRole(RoleType.STUDENT));
        spec = spec.and(UserSpecification.inCourse(courseId));
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
    public PageResponse<UserViewResponse> searchStudentsNotInCourse(Integer courseId, SearchUserRequest request, Pageable pageable) {
        Specification<User> spec = buildBaseUserSearchSpec(request);
        spec = spec.and(UserSpecification.hasRole(RoleType.STUDENT));
        spec = spec.and(UserSpecification.notInCourse(courseId));
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
        Enrollment enrollment = enrollmentRepository.findByStudent_IdAndCourse_IdAndApprovalStatus(
                studentId,
                courseId,
                EnrollmentStatus.APPROVED
        );
        if (enrollment == null) {
            return;
        }

        long totalItems = chapterItemRepository.countTotalItemsByCourseId(courseId);
        if (totalItems == 0) {
            enrollment.setProgress(0);
            enrollmentRepository.save(enrollment);
            return;
        }

        long completedItems = progressRepository.countCompletedItemsByStudentAndCourse(studentId, courseId);
        int percent = (int) Math.round(((double) completedItems / totalItems) * 100);
        if (percent > 100) {
            percent = 100;
        }

        enrollment.setProgress(percent);
        enrollmentRepository.save(enrollment);
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

        long totalItems = classContentItemRepository.countTotalItemsByClassSectionId(classSectionId);
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
        Course course = enrollment.getCourse();
        ClassSection classSection = enrollment.getClassSection();

        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .studentId(enrollment.getStudent() != null ? enrollment.getStudent().getId() : null)
                .userName(enrollment.getStudent() != null ? enrollment.getStudent().getUserName() : null)
                .fullName(enrollment.getStudent() != null ? enrollment.getStudent().getFullName() : null)
                .studentNumber(enrollment.getStudent() != null ? enrollment.getStudent().getStudentNumber() : null)
                .studentAvatar(enrollment.getStudent() != null ? enrollment.getStudent().getImageUrl() : null)
                .courseTitle(course != null ? course.getTitle() : null)
                .courseCode(course != null ? course.getClassCode() : null)
                .courseId(course != null ? course.getId() : null)
                .classSectionTitle(classSection != null ? classSection.getTitle() : null)
                .classSectionCode(classSection != null ? classSection.getClassCode() : null)
                .classSectionId(classSection != null ? classSection.getId() : null)
                .progress(enrollment.getProgress())
                .approvalStatus(enrollment.getApprovalStatus() != null ? enrollment.getApprovalStatus().toString() : null)
                .build();
    }

    private void validateEnrollmentTarget(EnrollmentRequest request) {
        boolean hasCourse = request.getCourseId() != null;
        boolean hasClassSection = request.getClassSectionId() != null;
        if (hasCourse == hasClassSection) {
            throw new BusinessException("Chi duoc truyen mot trong hai truong: courseId hoac classSectionId");
        }
    }

    private void ensureCurrentUserIsNotClassStaff(User currentUser, ClassSection classSection) {
        if (currentUser == null || classSection == null) {
            return;
        }
        if (classMemberAuthorizationService.isTeacherOrTa(classSection, currentUser)) {
            throw new BusinessException("Khong the dang ky vao lop khi ban dang giu vai tro giang day trong chinh lop nay");
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
            throw new BusinessException("Khong the them hoc vien vao lop khi nguoi dung da la giang vien hoac tro giang cua lop nay");
        }
    }

    private void ensureStudentIsNotClassStaff(EnrollmentRequest request, User student) {
        if (student == null || request == null || request.getClassSectionId() == null) {
            return;
        }
        ClassSection classSection = classSectionRepository.findById(request.getClassSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay lop hoc!"));
        ensureUserIsNotClassStaff(classSection, student);
    }

    private Enrollment findPendingEnrollment(EnrollmentRequest request) {
        if (request.getCourseId() != null) {
            return enrollmentRepository.findByStudent_IdAndCourse_IdAndApprovalStatus(
                    request.getStudentId(),
                    request.getCourseId(),
                    EnrollmentStatus.PENDING
            );
        }
        return enrollmentRepository.findByStudent_IdAndClassSection_IdAndApprovalStatus(
                request.getStudentId(),
                request.getClassSectionId(),
                EnrollmentStatus.PENDING
        );
    }

    private Enrollment findEnrollment(EnrollmentRequest request) {
        if (request.getCourseId() != null) {
            return enrollmentRepository.findByStudent_IdAndCourse_Id(
                    request.getStudentId(),
                    request.getCourseId()
            );
        }
        return enrollmentRepository.findByStudent_IdAndClassSection_Id(
                request.getStudentId(),
                request.getClassSectionId()
        );
    }

    private boolean isEnrollmentOwner(Enrollment enrollment, User currentUser) {
        if (currentUser.getRole().getRoleName().equals(RoleType.ADMIN)) {
            return true;
        }

        Course course = enrollment.getCourse();
        if (course != null && course.getTeacher() != null && course.getTeacher().getId().equals(currentUser.getId())) {
            return true;
        }

        ClassSection classSection = enrollment.getClassSection();
        if (classSection == null) {
            return false;
        }
        return classMemberAuthorizationService.isTeacher(classSection, currentUser);
    }

    private String resolveEnrollmentTargetTitle(Enrollment enrollment) {
        if (enrollment.getCourse() != null) {
            return enrollment.getCourse().getTitle();
        }
        if (enrollment.getClassSection() != null) {
            return enrollment.getClassSection().getTitle();
        }
        return "unknown target";
    }
}
