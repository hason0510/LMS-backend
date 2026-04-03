package com.example.backend.service.impl;

import com.example.backend.constant.ContentItemType;
import com.example.backend.constant.CourseStatus;
import com.example.backend.constant.EnrollmentStatus;
import com.example.backend.constant.ItemType;
import com.example.backend.constant.RoleType;
import com.example.backend.dto.request.EnrollmentRequest;
import com.example.backend.dto.request.course.CourseRatingRequest;
import com.example.backend.dto.request.course.StudentCourseRequest;
import com.example.backend.dto.request.search.SearchUserRequest;
import com.example.backend.dto.response.EnrollmentResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.course.CourseRatingResponse;
import com.example.backend.dto.response.user.UserViewResponse;
import com.example.backend.entity.ChapterItem;
import com.example.backend.entity.ClassContentItem;
import com.example.backend.entity.ClassSection;
import com.example.backend.entity.Course;
import com.example.backend.entity.CourseRating;
import com.example.backend.entity.Enrollment;
import com.example.backend.entity.StudentChapterItemProgress;
import com.example.backend.entity.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.repository.ChapterItemRepository;
import com.example.backend.repository.ClassContentItemRepository;
import com.example.backend.repository.ClassSectionRepository;
import com.example.backend.repository.CourseRatingRepository;
import com.example.backend.repository.CourseRepository;
import com.example.backend.repository.EnrollmentRepository;
import com.example.backend.repository.ProgressRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.CourseService;
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
    private final CourseRatingRepository courseRatingRepository;
    private final CourseService courseService;
    private final ProgressRepository progressRepository;
    private final ChapterItemRepository chapterItemRepository;
    private final ClassSectionRepository classSectionRepository;
    private final ClassContentItemRepository classContentItemRepository;

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

        Course course = courseRepository.findByClassCode(classCode)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay khoa hoc!"));
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
                .approvalStatus(EnrollmentStatus.PENDING)
                .build();
        enrollmentRepository.save(newEnrollment);

        User notifyUser = classSection.getTeacher() != null ? classSection.getTeacher() : classSection.getManager();
        if (notifyUser != null) {
            String message = "Sinh vien " + currentUser.getFullName() + " da yeu cau tham gia lop hoc: " + classSection.getTitle();
            notificationService.createNotification(notifyUser, "Yeu cau tham gia lop hoc", message, "CLASS_SECTION_ENROLLMENT_REQUEST", null, null);
        }

        return convertEnrollmentToDTO(newEnrollment);
    }

    @Override
    @Transactional
    public CourseRatingResponse ratingCourse(Integer courseId, CourseRatingRequest request) {
        if (request.getRatingValue() < 1 || request.getRatingValue() > 5) {
            throw new BusinessException("Diem danh gia phai tu 1 den 5!");
        }

        User currentUser = userService.getCurrentUser();
        boolean isEnrolled = enrollmentRepository.existsByStudent_IdAndCourse_IdAndApprovalStatus(
                currentUser.getId(), courseId, EnrollmentStatus.APPROVED);
        if (!isEnrolled) {
            throw new UnauthorizedException("Ban phai tham gia khoa hoc moi duoc danh gia!");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay khoa hoc"));

        CourseRating review = courseRatingRepository.findByStudent_IdAndCourse_Id(currentUser.getId(), courseId)
                .orElse(CourseRating.builder()
                        .student(currentUser)
                        .course(course)
                        .ratingValue(request.getRatingValue())
                        .description(request.getDescription())
                        .build());

        review.setRatingValue(request.getRatingValue());
        review.setDescription(request.getDescription());
        courseRatingRepository.save(review);

        Double avgRating = courseRatingRepository.getAverageRating(courseId);
        double roundedRating = avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0;
        course.setRating(roundedRating);
        courseRepository.save(course);
        return convertEntityToDTO(review);
    }

    @Override
    public void deleteReview(Integer courseId) {
        User currentUser = userService.getCurrentUser();
        CourseRating review = courseRatingRepository.findByStudent_IdAndCourse_Id(currentUser.getId(), courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay danh gia cua ban cho khoa hoc nay"));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay khoa hoc"));

        courseRatingRepository.delete(review);

        Double avgRating = courseRatingRepository.getAverageRating(courseId);
        double roundedRating = avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0;
        course.setRating(roundedRating);
        courseRepository.save(course);
    }

    @Override
    public PageResponse<CourseRatingResponse> getAllCourseRatings(Integer courseId, Integer ratingValue, Pageable pageable) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Khong tim thay khoa hoc");
        }

        Page<CourseRating> courseRatingPage;
        if (ratingValue != null && ratingValue > 0) {
            courseRatingPage = courseRatingRepository.findAllByCourse_IdAndRatingValue(courseId, ratingValue, pageable);
        } else {
            courseRatingPage = courseRatingRepository.findAllByCourse_Id(courseId, pageable);
        }

        Page<CourseRatingResponse> courseRatingResponsePage = courseRatingPage.map(this::convertEntityToDTO);
        return new PageResponse<>(
                courseRatingResponsePage.getNumber() + 1,
                courseRatingResponsePage.getTotalPages(),
                courseRatingResponsePage.getNumberOfElements(),
                courseRatingResponsePage.getContent()
        );
    }

    private CourseRatingResponse convertEntityToDTO(CourseRating entity) {
        return CourseRatingResponse.builder()
                .id(entity.getId())
                .courseId(entity.getCourse().getId())
                .courseName(entity.getCourse().getTitle())
                .studentId(entity.getStudent().getId())
                .studentCode(entity.getStudent().getStudentNumber())
                .studentUsername(entity.getStudent().getUserName())
                .studentFullname(entity.getStudent().getFullName())
                .ratingValue(entity.getRatingValue())
                .description(entity.getDescription())
                .build();
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

        StudentChapterItemProgress progress = progressRepository
                .findByStudent_IdAndChapterItem_Id(currentUser.getId(), chapterItem.getId())
                .orElse(StudentChapterItemProgress.builder()
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

        StudentChapterItemProgress progress = progressRepository
                .findByStudent_IdAndClassContentItem_Id(currentUser.getId(), classContentItemId)
                .orElse(StudentChapterItemProgress.builder()
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
        Page<EnrollmentResponse> enrollmentResponse = enrollmentPage.map(this::convertEnrollmentToDTO);
        return new PageResponse<>(
                enrollmentResponse.getNumber() + 1,
                enrollmentResponse.getTotalPages(),
                enrollmentResponse.getNumberOfElements(),
                enrollmentResponse.getContent()
        );
    }

    @Override
    public PageResponse<EnrollmentResponse> getStudentsPendingEnrollment(Integer courseId, Pageable pageable) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Khong tim thay khoa hoc");
        }
        Page<Enrollment> enrollmentPage = enrollmentRepository.findByCourse_IdAndApprovalStatus(courseId, EnrollmentStatus.PENDING, pageable);
        Page<EnrollmentResponse> enrollmentResponse = enrollmentPage.map(this::convertEnrollmentToDTO);
        return new PageResponse<>(
                enrollmentResponse.getNumber() + 1,
                enrollmentResponse.getTotalPages(),
                enrollmentResponse.getNumberOfElements(),
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
                enrollmentResponse.getNumberOfElements(),
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
                response.getNumberOfElements(),
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
                response.getNumberOfElements(),
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

    @Override
    public PageResponse<EnrollmentResponse> getTeacherEnrollments(Integer teacherId, Integer courseId, String approvalStatus, Pageable pageable) {
        Page<Enrollment> page;

        if (courseId != null && approvalStatus != null) {
            EnrollmentStatus status = EnrollmentStatus.valueOf(approvalStatus.toUpperCase());
            page = enrollmentRepository.findByTeacherIdAndCourseIdAndApprovalStatus(teacherId, courseId, status, pageable);
        } else if (courseId != null) {
            page = enrollmentRepository.findByTeacherIdAndCourseId(teacherId, courseId, pageable);
        } else if (approvalStatus != null) {
            EnrollmentStatus status = EnrollmentStatus.valueOf(approvalStatus.toUpperCase());
            page = enrollmentRepository.findByTeacherIdAndApprovalStatus(teacherId, status, pageable);
        } else {
            page = enrollmentRepository.findByTeacherId(teacherId, pageable);
        }

        List<EnrollmentResponse> responses = page.getContent().stream()
                .map(this::convertEnrollmentToDTO)
                .toList();

        return new PageResponse<>(
                page.getNumber() + 1,
                page.getTotalPages(),
                page.getTotalElements(),
                responses
        );
    }

    private void validateEnrollmentTarget(EnrollmentRequest request) {
        boolean hasCourse = request.getCourseId() != null;
        boolean hasClassSection = request.getClassSectionId() != null;
        if (hasCourse == hasClassSection) {
            throw new BusinessException("Chi duoc truyen mot trong hai truong: courseId hoac classSectionId");
        }
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

        boolean isTeacher = classSection.getTeacher() != null
                && classSection.getTeacher().getId().equals(currentUser.getId());
        boolean isManager = classSection.getManager() != null
                && classSection.getManager().getId().equals(currentUser.getId());
        return isTeacher || isManager;
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
