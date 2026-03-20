package com.example.backend.service.impl;


import com.example.backend.constant.CourseStatus;
import com.example.backend.constant.EnrollmentStatus;
import com.example.backend.constant.ResourceType;
import com.example.backend.constant.RoleType;
import com.example.backend.dto.request.course.CourseRequest;
import com.example.backend.dto.response.CloudinaryResponse;
import com.example.backend.dto.response.course.CourseResponse;
import com.example.backend.dto.response.EnrollmentStatusResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.entity.*;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.repository.CategoryRepository;
import com.example.backend.repository.CourseRepository;
import com.example.backend.repository.CourseRatingRepository;
import com.example.backend.repository.EnrollmentRepository;
import com.example.backend.service.CloudinaryService;
import com.example.backend.service.CourseService;
import com.example.backend.service.UserService;
import com.example.backend.utils.FileUploadUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.security.SecureRandom;

@Service
public class CourseServiceImpl implements CourseService {
    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final UserService userService;
    private final CloudinaryService cloudinaryService;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRatingRepository courseRatingRepository;

    private static final String ALPHANUMERIC_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final SecureRandom secureRandom = new SecureRandom();

    public CourseServiceImpl(CourseRepository courseRepository, CategoryRepository categoryRepository, UserService userService, CloudinaryService cloudinaryService, EnrollmentRepository enrollmentRepository, CourseRatingRepository courseRatingRepository) {
        this.courseRepository = courseRepository;
        this.categoryRepository = categoryRepository;
        this.userService = userService;
        this.cloudinaryService = cloudinaryService;
        this.enrollmentRepository = enrollmentRepository;
        this.courseRatingRepository = courseRatingRepository;
    }

    @Override
    public CourseResponse createCourse(CourseRequest request) {
        Course newCourse = new Course();
        newCourse.setDescription(request.getDescription());
        newCourse.setTitle(request.getTitle());
        newCourse.setTeacher(userService.getCurrentUser());
        newCourse.setStatus(CourseStatus.PRIVATE);
        newCourse.setClassCode(generateUniqueClassCode());
        newCourse.setRating(0.0);
        Category newCategory = categoryRepository.findById(request.getCategoryId()).orElseThrow(()
                -> new ResourceNotFoundException("Không tìm thấy danh mục!"));
        newCourse.setCategory(newCategory);
        courseRepository.save(newCourse);
        return convertEntityToDto(newCourse);
    }

    @Override
    public CourseResponse updateCourse(Integer id, CourseRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học!"));
        if (request.getTitle() != null) {
            course.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            course.setDescription(request.getDescription());
        }
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục!"));
            course.setCategory(category);
        }
        if(request.getStatus() != null){
            course.setStatus(request.getStatus());
        }
        if(request.getRating() != null){
            course.setRating(request.getRating());
        }
        courseRepository.save(course);
        return convertEntityToDto(course);
    }

    @Override
    public void deleteCourseById(Integer id) {
        Course deletedCourse = courseRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khoá học!"));
        User currentUser = userService.getCurrentUser();
        if(currentUser.getRole().getRoleName().equals(RoleType.ADMIN) || deletedCourse.getTeacher().getId().equals(currentUser.getId())) {
            for (Chapter chapter : deletedCourse.getChapters()) {
                chapter.set_deleted(true);
            }
            courseRepository.deleteById(id);
        }
    }

    @Override
    public CourseResponse getCourseById(Integer id) {
        User currentUser = userService.getCurrentUser();
        boolean isAdmin = currentUser.getRole().getRoleName().equals(RoleType.ADMIN);
        Course course = courseRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học!"));
        boolean isCourseOwner = course.getTeacher().getId().equals(currentUser.getId());
        
        // Teacher có thể xem khóa học mình tạo
        if(isCourseOwner || isAdmin) {
            return convertEntityToDto(course);
        }
        
        // Nếu khóa học là PRIVATE, chỉ admin và teacher owner được xem
        if(course.getStatus() == CourseStatus.PRIVATE){
            throw new UnauthorizedException("Bạn không được truy cập vào tài nguyên này!");
        }
        
        // PUBLIC courses: ai cũng được xem
        return convertEntityToDto(course);
    }

    @Override
    public PageResponse<CourseResponse> getAllPublicCourses(Pageable pageable) {
        Page<Course> coursePublicPage = courseRepository.findByStatus(CourseStatus.PUBLIC, pageable);
        Page<CourseResponse> courseResponsePage = coursePublicPage.map(this::convertEntityToDto);
        PageResponse<CourseResponse> response = new PageResponse<>(
                courseResponsePage.getNumber() + 1,
                courseResponsePage.getTotalPages(),
                courseResponsePage.getNumberOfElements(),
                courseResponsePage.getContent()
        );
        return response;
    }

    @Override
    public PageResponse<CourseResponse> getCoursesApprovedByStudent(Pageable pageable) {
        User currentStudent = userService.getCurrentUser();
        Page<Enrollment> enrollmentPage = enrollmentRepository.findByStudent_IdAndApprovalStatus(
                currentStudent.getId(),
                EnrollmentStatus.APPROVED,
                pageable
        );
        Page<CourseResponse> courseResponsePage = enrollmentPage.map(enrollment -> {
            Course course = enrollment.getCourse();
            CourseResponse courseResponse = convertEntityToDto(course);
            // Add progress from enrollment
            courseResponse.setProgress(enrollment.getProgress() != null ? enrollment.getProgress() : 0);
            return courseResponse;
        });
        return new PageResponse<>(
                courseResponsePage.getNumber() + 1,
                courseResponsePage.getTotalPages(),
                courseResponsePage.getTotalElements(),
                courseResponsePage.getContent()
        );
    }

    @Override
    public PageResponse<CourseResponse> getCoursesPendingByStudent(Pageable pageable) {
        User currentStudent = userService.getCurrentUser();
        Page<Enrollment> enrollmentPage = enrollmentRepository.findByStudent_IdAndApprovalStatus(
                currentStudent.getId(),
                EnrollmentStatus.PENDING,
                pageable
        );
        Page<CourseResponse> courseResponsePage = enrollmentPage.map(enrollment -> {
            Course course = enrollment.getCourse();
            CourseResponse courseResponse = convertEntityToDto(course);
            // Add progress from enrollment
            courseResponse.setProgress(enrollment.getProgress() != null ? enrollment.getProgress() : 0);
            return courseResponse;
        });
        return new PageResponse<>(
                courseResponsePage.getNumber() + 1,
                courseResponsePage.getTotalPages(),
                courseResponsePage.getTotalElements(),
                courseResponsePage.getContent()
        );
    }

    @Override
    public PageResponse<CourseResponse> getAllCoursesByTeacher(Pageable pageable) {
        User currentUser = userService.getCurrentUser();
        Page<Course> coursePublicPage = courseRepository.findByTeacher_Id(currentUser.getId(), pageable);
        Page<CourseResponse> courseResponsePage = coursePublicPage.map(this::convertEntityToDto);
        PageResponse<CourseResponse> response = new PageResponse<>(
                courseResponsePage.getNumber() + 1,
                courseResponsePage.getTotalPages(),
                courseResponsePage.getNumberOfElements(),
                courseResponsePage.getContent()
        );
        return response;
    }


    @Override
    public PageResponse<CourseResponse> getAllCourses(Pageable pageable) {
        Page<Course> coursePage = courseRepository.findAll(pageable);
        Page<CourseResponse> courseResponsePage = coursePage.map(this::convertEntityToDto);
        PageResponse<CourseResponse> response = new PageResponse<>(
                courseResponsePage.getNumber() + 1,
                courseResponsePage.getTotalPages(),
                courseResponsePage.getNumberOfElements(),
                courseResponsePage.getContent()
        );
        return response;
    }

    @Override
    public CloudinaryResponse uploadImage(Integer id, MultipartFile file) {
        final Course uploadCourse = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        FileUploadUtil.assertAllowed(file, "image");
        final String cloudinaryImageId = uploadCourse.getCloudinaryImageId();
        if(StringUtils.hasText(cloudinaryImageId)) {
            cloudinaryService.deleteFile(cloudinaryImageId, ResourceType.IMAGE);
        }
        final String fileName = FileUploadUtil.getFileName(file.getOriginalFilename());
        final CloudinaryResponse response = this.cloudinaryService.uploadFile(file, fileName, "image");
        uploadCourse.setImageUrl(response.getUrl());
        uploadCourse.setCloudinaryImageId(response.getPublicId());
        courseRepository.save(uploadCourse);
        return response;
    }

    @Override
    public CourseResponse convertEntityToDto(Course course){
        CourseResponse response = new CourseResponse();
        response.setId(course.getId());
        response.setTitle(course.getTitle());
        response.setDescription(course.getDescription());
        response.setCategoryId(course.getCategory().getId());
        response.setCategoryName(course.getCategory().getTitle());
        response.setTeacherId(course.getTeacher().getId());
        response.setTeacherName(course.getTeacher().getFullName());
        response.setImageUrl(course.getImageUrl());
        response.setCloudinaryImageId(course.getCloudinaryImageId());
        response.setStatus(course.getStatus());
        response.setClassCode(course.getClassCode());
        response.setRating(course.getRating());
        response.setReviewCounts(courseRatingRepository.countReviewsByCourse(course.getId()));
        response.setTotalEnrollments(enrollmentRepository.countApprovedEnrollmentsByCourseId(course.getId()));
        return response;
    }

    @Override
    public CourseResponse publishCourse(Integer id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học!"));
        User currentUser = userService.getCurrentUser();
        boolean isAdmin = currentUser.getRole().getRoleName().equals(RoleType.ADMIN);
        boolean isCourseOwner = course.getTeacher().getId().equals(currentUser.getId());

        if (!isAdmin && !isCourseOwner) {
            throw new UnauthorizedException("Bạn không có quyền xuất bản khóa học này!");
        }

        if (course.getStatus() == CourseStatus.PUBLIC) {
            throw new IllegalArgumentException("Khóa học đã được xuất bản!");
        }

        course.setStatus(CourseStatus.PUBLIC);
        courseRepository.save(course);
        return convertEntityToDto(course);
    }

    @Override
    public EnrollmentStatusResponse checkEnrollmentStatus(Integer courseId) {
        User currentUser = userService.getCurrentUser();
        
        // Check if user is enrolled with APPROVED status
        boolean isApproved = enrollmentRepository.existsByStudent_IdAndCourse_IdAndApprovalStatus(
                currentUser.getId(),
                courseId,
                EnrollmentStatus.APPROVED
        );
        
        // Check if user has any enrollment (PENDING or APPROVED)
        Enrollment enrollment = enrollmentRepository.findByStudent_IdAndCourse_Id(
                currentUser.getId(),
                courseId
        );
        
        String enrollmentStatus = null;
        if (enrollment != null) {
            enrollmentStatus = enrollment.getApprovalStatus().toString();
        }
        
        return EnrollmentStatusResponse.builder()
                .enrolled(isApproved)
                .enrollmentStatus(enrollmentStatus)
                .build();
    }

    private String generateUniqueClassCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(5);
            for (int i = 0; i < 5; i++) {
                int randomIndex = secureRandom.nextInt(ALPHANUMERIC_CHARS.length());
                sb.append(ALPHANUMERIC_CHARS.charAt(randomIndex));
            }
            code = sb.toString();
        } while (courseRepository.existsByClassCode(code)); // Nếu trùng thì random lại
        return code;
    }


}
