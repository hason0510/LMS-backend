package com.example.backend.controller;

import com.example.backend.dto.request.EnrollmentRequest;
import com.example.backend.dto.request.course.CourseRatingRequest;
import com.example.backend.dto.request.course.StudentCourseRequest;
import com.example.backend.dto.request.search.SearchUserRequest;
import com.example.backend.dto.response.EnrollmentResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.course.CourseRatingResponse;
import com.example.backend.dto.response.course.CourseResponse;
import com.example.backend.dto.response.user.UserViewResponse;
import com.example.backend.entity.User;
import com.example.backend.service.CourseService;
import com.example.backend.service.EnrollmentService;
import com.example.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/lms")
@Tag(name = "Enrollment Management", description = "APIs for managing course enrollments and student registrations")
public class EnrollmentController {
    private final EnrollmentService enrollmentService;
    private final UserService userService;

    public EnrollmentController(EnrollmentService enrollmentService, CourseService courseService, UserService userService) {
        this.enrollmentService = enrollmentService;
        this.userService = userService;
    }

    // ================= STUDENT SELF-ENROLLMENT =================

    @Operation(summary = "Đăng ký tham gia khóa học Public")
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/courses/{courseId}/enroll")
    public ResponseEntity<EnrollmentResponse> enrollPublicCourse(@PathVariable Integer courseId) {
        EnrollmentResponse response = enrollmentService.enrollPublicCourse(courseId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Đăng ký tham gia khóa học Private")
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/courses/enroll/private")
    public ResponseEntity<EnrollmentResponse> enrollPrivateCourse(@RequestBody Map<String, String> requestBody) {
        // Giả sử body gửi lên là { "classCode": "CODE123" }
        String classCode = requestBody.get("classCode");
        EnrollmentResponse response = enrollmentService.enrollPrivateCourse(classCode);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @Operation(summary = "Hiển thị danh sách đánh giá một khóa học cụ thể")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/courses/{courseId}/rating")
    public ResponseEntity<PageResponse<CourseRatingResponse>> getAllCourseRatings(
            @PathVariable Integer courseId,
            @RequestParam(value = "pageNumber", defaultValue = "1") Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "ratingValue", required = false) Integer ratingValue) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        PageResponse<CourseRatingResponse> response = enrollmentService.getAllCourseRatings(courseId, ratingValue, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Sinh viên trong khóa đánh giá khóa hoọc")
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/courses/{courseId}/rating")
    public ResponseEntity<CourseRatingResponse> ratingCourse(@PathVariable Integer courseId, @RequestBody CourseRatingRequest request) {
        CourseRatingResponse response = enrollmentService.ratingCourse(courseId, request);
        return ResponseEntity.ok().body(response);
    }

    @PreAuthorize("hasRole('STUDENT')")
    @DeleteMapping("/courses/{courseId}/rating")
    public ResponseEntity<Void> deleteReview(@PathVariable Integer courseId) {
        enrollmentService.deleteReview(courseId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Đánh dấu hoàn thành LESSON")
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/chapter-items/{chapterItemId}/complete")
    public ResponseEntity<Void> completeLesson(@PathVariable Integer chapterItemId) {
        enrollmentService.completeLesson(chapterItemId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/my-progress/{courseId}")
    public ResponseEntity<EnrollmentResponse> getCurrentUserProgressByCourse(@PathVariable Integer courseId){
        EnrollmentResponse response =  enrollmentService.getCurrentUserProgressByCourse(courseId);
        return ResponseEntity.ok().body(response);
    }


    // ================= TEACHER/ADMIN APPROVAL & MANAGEMENT =================

    @Operation(summary = "Phê duyệt yêu cầu tham gia khóa học")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/enrollments/approve")
    public ResponseEntity<EnrollmentResponse> approveStudentToEnrollment(@RequestBody EnrollmentRequest request) {
        EnrollmentResponse response = enrollmentService.approveStudentToEnrollment(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Từ chối/Xóa học viên khỏi khóa học")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @DeleteMapping("/enrollments/reject")
    public ResponseEntity<Void> rejectStudent(@RequestBody EnrollmentRequest request) {
        enrollmentService.rejectStudentEnrollment(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Lấy danh sách học viên đã được duyệt vào khóa học")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/courses/{courseId}/enrollments/approved")
    public ResponseEntity<PageResponse<EnrollmentResponse>> getStudentsApprovedInEnrollment(
            @PathVariable Integer courseId,
            @RequestParam(value = "pageNumber", defaultValue = "1") Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, Sort.by("id").descending());
        PageResponse<EnrollmentResponse> response = enrollmentService.getStudentsApprovedInEnrollment(courseId, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Lấy danh sách yêu cầu đang CHỜ duyệt")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/courses/{courseId}/enrollments/pending")
    public ResponseEntity<PageResponse<EnrollmentResponse>> getStudentsPendingEnrollment(
            @PathVariable Integer courseId,
            @RequestParam(value = "pageNumber", defaultValue = "1") Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, Sort.by("id").descending());
        PageResponse<EnrollmentResponse> response = enrollmentService.getStudentsPendingEnrollment(courseId, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Lấy tất cả danh sách ghi danh", description = "Get all enrollments in the system")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/enrollments")
    public ResponseEntity<PageResponse<EnrollmentResponse>> getEnrollmentPage(
            @RequestParam(value = "pageNumber", defaultValue = "1") Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        PageResponse<EnrollmentResponse> response = enrollmentService.getEnrollmentPage(pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Lấy danh sách enrollments của giáo viên", description = "Get all enrollments for teacher's courses")
    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/teacher/enrollments")
    public ResponseEntity<PageResponse<EnrollmentResponse>> getTeacherEnrollments(
            @RequestParam(value = "courseId", required = false) Integer courseId,
            @RequestParam(value = "approvalStatus", required = false) String approvalStatus,
            @RequestParam(value = "pageNumber", defaultValue = "1") Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        User currentUser = userService.getCurrentUser();
        Integer teacherId = currentUser.getId();
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        PageResponse<EnrollmentResponse> response = enrollmentService.getTeacherEnrollments(teacherId, courseId, approvalStatus, pageable);
        return ResponseEntity.ok(response);
    }

    // ================= MANUAL ADD/REMOVE STUDENTS (EXISTING) =================

    @Operation(summary = "Lấy danh sách sinh viên có thể thêm vào khóa học (Search)")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/courses/{courseId}/students/available")
    public ResponseEntity<PageResponse<UserViewResponse>> getStudentsInCourse(
            @PathVariable Integer courseId, SearchUserRequest request,
            @RequestParam(value = "pageNumber", defaultValue = "1") Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "5") Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        PageResponse<UserViewResponse> response = enrollmentService.searchStudentsInCourse(courseId, request, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Lấy danh sách sinh viên chưa trong khóa học (Search)")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/courses/{courseId}/students/not-available")
    public ResponseEntity<PageResponse<UserViewResponse>> getStudentsNotInCourse(
            @PathVariable Integer courseId, SearchUserRequest request,
            @RequestParam(value = "pageNumber", defaultValue = "1") Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "5") Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        PageResponse<UserViewResponse> response = enrollmentService.searchStudentsNotInCourse(courseId, request, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Thêm trực tiếp sinh viên vào khóa học")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/courses/{courseId}/students")
    public ResponseEntity<?> addStudentsToCourse(@PathVariable Integer courseId, @RequestBody StudentCourseRequest request) {
        enrollmentService.addStudentsToCourse(courseId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new java.util.HashMap<String, String>() {{
            put("code", "201");
            put("message", "Students has been added successfully");
        }});
    }

    @Operation(summary = "Xóa sinh viên khỏi khóa học")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @DeleteMapping("/courses/{courseId}/students")
    public ResponseEntity<?> removeStudentsInCourse(@PathVariable Integer courseId, @RequestBody StudentCourseRequest request) {
        enrollmentService.removeStudentsInCourse(courseId, request);
        return ResponseEntity.ok(new java.util.HashMap<String, String>() {{
            put("code", "200");
            put("message", "Students has been deleted successfully");
        }});
    }
}