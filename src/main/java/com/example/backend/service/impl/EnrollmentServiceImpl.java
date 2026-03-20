package com.example.backend.service.impl;

import com.example.backend.constant.CourseStatus;
import com.example.backend.constant.EnrollmentStatus;
import com.example.backend.constant.ItemType;
import com.example.backend.constant.RoleType;
import com.example.backend.dto.request.EnrollmentRequest;
import com.example.backend.dto.request.course.CourseRatingRequest;
import com.example.backend.dto.request.course.StudentCourseRequest;
import com.example.backend.dto.request.search.SearchUserRequest;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.EnrollmentResponse;
import com.example.backend.dto.response.course.CourseRatingResponse;
import com.example.backend.dto.response.course.CourseResponse;
import com.example.backend.dto.response.user.UserViewResponse;
import com.example.backend.entity.*;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.repository.*;
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

    @Transactional
    @Override
    public void addStudentsToCourse(Integer courseId, StudentCourseRequest request){
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học!"));

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
        for(User u : users){
            String message = "Bạn đã được thêm vào khóa học " + course.getTitle();
            notificationService.createNotification(u, "Được thêm vào khóa học", message, "ENROLLMENT", null, null);
        }
    }

    @Transactional
    @Override
    public void removeStudentsInCourse(Integer courseId, StudentCourseRequest request){
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        if (request.getStudentIds() == null || request.getStudentIds().isEmpty()) {
            return;
        }
        List<User> users = userRepository.findAllById(request.getStudentIds());
        List<Enrollment> progresses = enrollmentRepository.findByCourse_IdAndStudent_IdIn(courseId, request.getStudentIds());
        enrollmentRepository.deleteAll(progresses);
        for(User u : users){
            String message = "Bạn đã bị xóa tên khỏi danh sách lớp " + course.getTitle();
            notificationService.createNotification(u, "Bị xóa khỏi khóa học", message, "ENROLLMENT", null, null);
        }
    }

    @Override
    public EnrollmentResponse enrollPublicCourse(Integer courseId) {
        User currentUser = userService.getCurrentUser();

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học!"));

        if (course.getStatus() == CourseStatus.PRIVATE) {
            throw new UnauthorizedException("Bạn không được truy cập vào tài nguyên này!");
        }

        Enrollment existingEnrollment = enrollmentRepository.findByStudent_IdAndCourse_Id(currentUser.getId(), courseId);

        if (existingEnrollment != null) {
            EnrollmentStatus status = existingEnrollment.getApprovalStatus();

            if (status == EnrollmentStatus.APPROVED) {
                throw new BusinessException("Bạn đã tham gia khóa học này!");
            }

            if (status == EnrollmentStatus.PENDING) {
                throw new BusinessException("Hãy đợi giáo viên xét duyệt yêu cầu của bạn!");
            }
        }

        Enrollment newEnrollment = Enrollment.builder()
                .student(currentUser)
                .course(course)
                .progress(0)
                .approvalStatus(EnrollmentStatus.PENDING)
                .build();
        enrollmentRepository.save(newEnrollment);

        String message = "Sinh viên " + currentUser.getFullName() + " đã yêu cầu tham gia khóa học: " + course.getTitle();
        notificationService.createNotification(course.getTeacher(), "Yêu cầu tham gia khóa học", message, "ENROLLMENT_REQUEST", null, null);

        return convertEnrollmentToDTO(newEnrollment);
    }

    @Override
    public EnrollmentResponse enrollPrivateCourse(String classCode){
        User currentUser = userService.getCurrentUser();
        if(!userRepository.existsById(currentUser.getId())){
            throw new ResourceNotFoundException("Không tìm thấy người dùng");
        }
        Course course = courseRepository.findByClassCode(classCode).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học!"));
        if(enrollmentRepository.findByStudent_IdAndCourse_IdAndApprovalStatus(
                currentUser.getId(), course.getId(), EnrollmentStatus.APPROVED) != null){
            throw new BusinessException("Bạn đã tham gia khóa học này!");
        }
        if(enrollmentRepository.findByStudent_IdAndCourse_IdAndApprovalStatus(
                currentUser.getId(), course.getId(), EnrollmentStatus.PENDING) != null){
            throw new BusinessException("Hãy đợi giáo viên xét duyệt yêu cầu của bạn");
        }
        Enrollment newEnrollment = Enrollment.builder()
                .student(currentUser)
                .course(course)
                .progress(0)
                .approvalStatus(EnrollmentStatus.PENDING)
                .build();
        enrollmentRepository.save(newEnrollment);

        String message = "Sinh viên " + currentUser.getFullName() + " đã yêu cầu tham gia khóa học: " + course.getTitle();
        notificationService.createNotification(course.getTeacher(), "Yêu cầu tham gia khóa học", message, "ENROLLMENT_REQUEST", null, null);

        return convertEnrollmentToDTO(newEnrollment);
    }

    @Override
    @Transactional
    public CourseRatingResponse ratingCourse(Integer courseId, CourseRatingRequest request) {
        if (request.getRatingValue() < 1 || request.getRatingValue() > 5) {
            throw new BusinessException("Điểm đánh giá phải từ 1 đến 5!");
        }
        User currentUser = userService.getCurrentUser();
        boolean isEnrolled = enrollmentRepository.existsByStudent_IdAndCourse_IdAndApprovalStatus(
                currentUser.getId(), courseId, EnrollmentStatus.APPROVED);
        if (!isEnrolled) {
            throw new UnauthorizedException("Bạn phải tham gia khóa học mới được đánh giá!");
        }
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học"));

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
        // 5. Làm tròn (nếu muốn, ví dụ 1 số thập phân)
        // Ví dụ: 4.5666 -> 4.6
        double roundedRating = (avgRating != null) ? Math.round(avgRating * 10.0) / 10.0 : 0.0;
        course.setRating(roundedRating);
        courseRepository.save(course);
        return convertEntityToDTO(review);
    }

    @Override
    public void deleteReview(Integer courseId) {
        User currentUser = userService.getCurrentUser();
        CourseRating review = courseRatingRepository.findByStudent_IdAndCourse_Id(currentUser.getId(), courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá của bạn cho khóa học này"));
        
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học"));
        
        courseRatingRepository.delete(review);
        
        // Update course average rating
        Double avgRating = courseRatingRepository.getAverageRating(courseId);
        double roundedRating = (avgRating != null) ? Math.round(avgRating * 10.0) / 10.0 : 0.0;
        course.setRating(roundedRating);
        courseRepository.save(course);
    }

    @Override
    public PageResponse<CourseRatingResponse> getAllCourseRatings(Integer courseId, Integer ratingValue, Pageable pageable) {
        if(!courseRepository.existsById(courseId)){
            throw new ResourceNotFoundException("Không tìm thấy khóa học");
        }
        
        Page<CourseRating> courseRatingPage;
        if (ratingValue != null && ratingValue > 0) {
            // Filter by rating value if provided
            courseRatingPage = courseRatingRepository.findAllByCourse_IdAndRatingValue(courseId, ratingValue, pageable);
        } else {
            // Get all ratings if no filter
            courseRatingPage = courseRatingRepository.findAllByCourse_Id(courseId, pageable);
        }
        
        Page<CourseRatingResponse> courseRatingResponsePage = courseRatingPage.map(this::convertEntityToDTO);
        PageResponse<CourseRatingResponse> response = new PageResponse<>(
                courseRatingResponsePage.getNumber() + 1,
                courseRatingResponsePage.getTotalPages(),
                courseRatingResponsePage.getNumberOfElements(),
                courseRatingResponsePage.getContent()
        );
        return response;
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

        // 1. Lấy thông tin ChapterItem
        ChapterItem chapterItem = chapterItemRepository.findById(chapterItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài giảng không tồn tại"));
        Integer courseId = chapterItem.getChapter().getCourse().getId();

        if(chapterItem.getType() != ItemType.LESSON){
            throw new BusinessException("Đây không phải là bài giảng!");
        }
        boolean isEnrolled = enrollmentRepository.existsByStudent_IdAndCourse_IdAndApprovalStatus(
                currentUser.getId(), courseId, EnrollmentStatus.APPROVED);

        if (!isEnrolled) {
            throw new UnauthorizedException("Bạn chưa đăng ký hoặc chưa được duyệt vào khóa học này");
        }

        // 3. Cập nhật hoặc tạo mới StudentChapterItemProgress
        StudentChapterItemProgress progress = progressRepository
                .findByStudent_IdAndChapterItem_Id(currentUser.getId(), chapterItem.getId())
                .orElse(StudentChapterItemProgress.builder()
                        .student(currentUser)
                        .chapterItem(chapterItem)
                        .isCompleted(false)
                        .build());

        // 4. Nếu chưa hoàn thành thì mới cập nhật và tính lại %
        if (!Boolean.TRUE.equals(progress.getIsCompleted())) {
            progress.setIsCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
            progressRepository.save(progress);

            // 5. Tính lại tổng % tiến độ cho Enrollment
            if (chapterItem.getChapter() != null && chapterItem.getChapter().getCourse() != null) {
                recalculateAndSaveProgress(
                        currentUser.getId(),
                        chapterItem.getChapter().getCourse().getId()
                );
            }
        }
    }

    @Override
    public EnrollmentResponse approveStudentToEnrollment(EnrollmentRequest request) {
        User currentUser = userService.getCurrentUser();
        User student = userRepository.findById(request.getStudentId()).orElseThrow(() ->
                new ResourceNotFoundException("Không tìm tháy người dùng!"));
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học!"));
        Enrollment enrollment = enrollmentRepository.findByStudent_IdAndCourse_IdAndApprovalStatus(
                request.getStudentId(), request.getCourseId(), EnrollmentStatus.PENDING);
        if (enrollment == null) {
            throw new ResourceNotFoundException("Yêu cầu tham gia khóa học không tồn tại hoặc đã được xử lý!");
        }
        boolean isCourseOwner = enrollment.getCourse().getTeacher().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole().getRoleName().equals(RoleType.ADMIN);
        if (!isCourseOwner && !isAdmin) {
            throw new UnauthorizedException("Bạn không có quyền phê duyệt cho khóa học này!");
        }
        enrollment.setApprovalStatus(EnrollmentStatus.APPROVED);
        enrollmentRepository.save(enrollment);

        String message = "Bạn đã được thêm vào khóa học " + course.getTitle();
        notificationService.createNotification(student, "Được thêm vào khóa học", message, "ENROLLMENT", null, null);
        return convertEnrollmentToDTO(enrollment);
    }

    @Override
    public void rejectStudentEnrollment(EnrollmentRequest request) {
        User currentUser = userService.getCurrentUser();
        User student = userRepository.findById(request.getStudentId()).orElseThrow(() ->
                new ResourceNotFoundException("Không tìm tháy người dùng!"));
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học!"));
        Enrollment enrollment = enrollmentRepository.findByStudent_IdAndCourse_Id(
                request.getStudentId(), request.getCourseId());
        if (enrollment == null) {
            throw new ResourceNotFoundException("Không tìm thấy thông tin đăng ký của người dùng này!");
        }
        boolean isCourseOwner = enrollment.getCourse().getTeacher().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole().getRoleName().equals(RoleType.ADMIN);
        if (!isCourseOwner && !isAdmin) {
            throw new UnauthorizedException("Bạn không có quyền từ chối yêu cầu này!");
        }
        enrollmentRepository.delete(enrollment);
        String message = "Yêu cầu tham gia khóa học " + course.getTitle() + " của bạn đã bị từ chối";
        notificationService.createNotification(student, "Yêu cầu bị từ chối", message, "ENROLLMENT_REJECTED", null, null);
    }

    @Override
    public PageResponse<EnrollmentResponse> getStudentsApprovedInEnrollment(Integer courseId, Pageable pageable) {
        if(!courseRepository.existsById(courseId)){
            throw new ResourceNotFoundException("Không tìm thấy khóa học");
        }
        Page<Enrollment> enrollmentPage = enrollmentRepository.findByCourse_IdAndApprovalStatus(courseId, EnrollmentStatus.APPROVED, pageable);
        Page<EnrollmentResponse> enrollmentResponse = enrollmentPage.map(this::convertEnrollmentToDTO);
        PageResponse<EnrollmentResponse> response = new PageResponse<>(
                enrollmentResponse.getNumber() + 1,
                enrollmentResponse.getTotalPages(),
                enrollmentResponse.getNumberOfElements(),
                enrollmentResponse.getContent()
        );
        return response;
    }

    @Override
    public PageResponse<EnrollmentResponse> getStudentsPendingEnrollment(Integer courseId, Pageable pageable){
        if(!courseRepository.existsById(courseId)){
            throw new ResourceNotFoundException("Không tìm thấy khóa học");
        }
        Page<Enrollment> enrollmentPage = enrollmentRepository.findByCourse_IdAndApprovalStatus(courseId, EnrollmentStatus.PENDING, pageable);
        Page<EnrollmentResponse> enrollmentResponse = enrollmentPage.map(this::convertEnrollmentToDTO);
        PageResponse<EnrollmentResponse> response = new PageResponse<>(
                enrollmentResponse.getNumber() + 1,
                enrollmentResponse.getTotalPages(),
                enrollmentResponse.getNumberOfElements(),
                enrollmentResponse.getContent()
        );
        return response;
    }

    @Override
    public EnrollmentResponse getCurrentUserProgressByCourse(Integer courseId){
        User currentStudent = userService.getCurrentUser();
        Enrollment enrollment = enrollmentRepository.findByStudent_IdAndCourse_IdAndApprovalStatus(currentStudent.getId(), courseId, EnrollmentStatus.APPROVED);
        if(enrollment == null){
            throw new UnauthorizedException("Bạn không nằm trong khóa học này!");
        }
        return convertEnrollmentToDTO(enrollment);
    }

    @Override
    public EnrollmentResponse getEnrollmentById(Integer id){
        Enrollment enrollment = enrollmentRepository.findById(id).orElseThrow(()
                -> new ResourceNotFoundException("Không tìm thấy tài nguyên"));
        return convertEnrollmentToDTO(enrollment);
    }



    @Override
    public PageResponse<EnrollmentResponse> getEnrollmentPage(Pageable pageable){
        Page<Enrollment> enrollmentPage = enrollmentRepository.findAll(pageable);
        Page<EnrollmentResponse> enrollmentResponse = enrollmentPage.map(this::convertEnrollmentToDTO);
        PageResponse<EnrollmentResponse> response = new PageResponse<>(
                enrollmentResponse.getNumber() + 1,
                enrollmentResponse.getTotalPages(),
                enrollmentResponse.getNumberOfElements(),
                enrollmentResponse.getContent()
        );
        return response;

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
        long totalItems = chapterItemRepository.countTotalItemsByCourseId(courseId);
        if (totalItems == 0) return;
        long completedItems = progressRepository.countCompletedItemsByStudentAndCourse(studentId, courseId);
        int percent = (int) Math.round(((double) completedItems / totalItems) * 100);
        if (percent > 100) percent = 100;
        // 4. Update vào Enrollment
        Enrollment enrollment = enrollmentRepository.findByStudent_IdAndCourse_IdAndApprovalStatus(studentId, courseId, EnrollmentStatus.APPROVED); // Cần viết hàm này trong repo nếu chưa có hoặc dùng hàm có sẵn
        if (enrollment != null) {
            enrollment.setProgress(percent);
            enrollmentRepository.save(enrollment);
        }
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
        EnrollmentResponse response = new EnrollmentResponse();
        response.setId(enrollment.getId());
        response.setStudentId(enrollment.getStudent().getId());
        response.setUserName(enrollment.getStudent().getUserName());
        response.setFullName(enrollment.getStudent().getFullName());
        response.setStudentNumber(enrollment.getStudent().getStudentNumber());
        response.setStudentAvatar(enrollment.getStudent().getImageUrl());
        response.setCourseTitle(enrollment.getCourse().getTitle());
        response.setCourseCode(enrollment.getCourse().getClassCode());
        response.setCourseId(enrollment.getCourse().getId());
        response.setProgress(enrollment.getProgress());
        response.setApprovalStatus(enrollment.getApprovalStatus().toString());
        return response;
    }

    @Override
    public PageResponse<EnrollmentResponse> getTeacherEnrollments(Integer teacherId, Integer courseId, String approvalStatus, Pageable pageable) {
        Page<Enrollment> page;

        if (courseId != null && approvalStatus != null) {
            // Filter by both courseId and approvalStatus
            EnrollmentStatus status = EnrollmentStatus.valueOf(approvalStatus.toUpperCase());
            page = enrollmentRepository.findByTeacherIdAndCourseIdAndApprovalStatus(teacherId, courseId, status, pageable);
        } else if (courseId != null) {
            // Filter by courseId only
            page = enrollmentRepository.findByTeacherIdAndCourseId(teacherId, courseId, pageable);
        } else if (approvalStatus != null) {
            // Filter by approvalStatus only
            EnrollmentStatus status = EnrollmentStatus.valueOf(approvalStatus.toUpperCase());
            page = enrollmentRepository.findByTeacherIdAndApprovalStatus(teacherId, status, pageable);
        } else {
            // No filters, get all enrollments for teacher
            page = enrollmentRepository.findByTeacherId(teacherId, pageable);
        }

        List<EnrollmentResponse> responses = page.getContent().stream()
                .map(enrollment -> new EnrollmentResponse(
                        enrollment.getId(),
                        enrollment.getStudent().getId(),
                        enrollment.getStudent().getUserName(),
                        enrollment.getStudent().getFullName(),
                        enrollment.getStudent().getStudentNumber(),
                        enrollment.getStudent().getImageUrl(),
                        enrollment.getCourse().getTitle(),
                        enrollment.getCourse().getClassCode(),
                        enrollment.getCourse().getId(),
                        enrollment.getProgress(),
                        enrollment.getApprovalStatus().toString()
                ))
                .toList();

        return new PageResponse<>(
                page.getNumber() + 1,
                page.getTotalPages(),
                page.getTotalElements(),
                responses
        );
    }
}
