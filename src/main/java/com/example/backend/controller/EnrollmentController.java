package com.example.backend.controller;

import com.example.backend.dto.request.EnrollmentRequest;
import com.example.backend.dto.request.course.StudentCourseRequest;
import com.example.backend.dto.request.search.SearchUserRequest;
import com.example.backend.dto.response.EnrollmentResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.user.UserViewResponse;
import com.example.backend.entity.User;
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

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/lms")
@Tag(name = "Enrollment Management", description = "APIs for managing enrollments and student registrations")
public class EnrollmentController {
    private final EnrollmentService enrollmentService;
    private final UserService userService;

    public EnrollmentController(EnrollmentService enrollmentService, UserService userService) {
        this.enrollmentService = enrollmentService;
        this.userService = userService;
    }

    @Operation(summary = "Student enroll in public course")
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/courses/{courseId}/enroll")
    public ResponseEntity<EnrollmentResponse> enrollPublicCourse(@PathVariable Integer courseId) {
        EnrollmentResponse response = enrollmentService.enrollPublicCourse(courseId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Student enroll in private course by class code")
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/courses/enroll/private")
    public ResponseEntity<EnrollmentResponse> enrollPrivateCourse(@RequestBody Map<String, String> requestBody) {
        String classCode = requestBody.get("classCode");
        EnrollmentResponse response = enrollmentService.enrollPrivateCourse(classCode);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Student enroll in class section")
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/class-sections/{classSectionId}/enroll")
    public ResponseEntity<EnrollmentResponse> enrollClassSection(@PathVariable Integer classSectionId) {
        EnrollmentResponse response = enrollmentService.enrollClassSection(classSectionId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Mark legacy lesson complete")
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/chapter-items/{chapterItemId}/complete")
    public ResponseEntity<Void> completeLesson(@PathVariable Integer chapterItemId) {
        enrollmentService.completeLesson(chapterItemId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Mark class content item complete")
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/class-content-items/{classContentItemId}/complete")
    public ResponseEntity<Void> completeClassContentItem(@PathVariable Integer classContentItemId) {
        enrollmentService.completeClassContentItem(classContentItemId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/my-progress/{courseId}")
    public ResponseEntity<EnrollmentResponse> getCurrentUserProgressByCourse(@PathVariable Integer courseId) {
        EnrollmentResponse response = enrollmentService.getCurrentUserProgressByCourse(courseId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/my-progress/class-sections/{classSectionId}")
    public ResponseEntity<EnrollmentResponse> getCurrentUserProgressByClassSection(@PathVariable Integer classSectionId) {
        EnrollmentResponse response = enrollmentService.getCurrentUserProgressByClassSection(classSectionId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Approve enrollment request")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/enrollments/approve")
    public ResponseEntity<EnrollmentResponse> approveStudentToEnrollment(@RequestBody EnrollmentRequest request) {
        EnrollmentResponse response = enrollmentService.approveStudentToEnrollment(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reject enrollment request")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @DeleteMapping("/enrollments/reject")
    public ResponseEntity<Void> rejectStudent(@RequestBody EnrollmentRequest request) {
        enrollmentService.rejectStudentEnrollment(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get approved enrollments by course")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/courses/{courseId}/enrollments/approved")
    public ResponseEntity<PageResponse<EnrollmentResponse>> getStudentsApprovedInEnrollment(
            @PathVariable Integer courseId,
            @RequestParam(value = "pageNumber", defaultValue = "1") Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize
    ) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, Sort.by("id").descending());
        PageResponse<EnrollmentResponse> response = enrollmentService.getStudentsApprovedInEnrollment(courseId, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get approved enrollments by class section")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/class-sections/{classSectionId}/enrollments/approved")
    public ResponseEntity<PageResponse<EnrollmentResponse>> getStudentsApprovedInClassSection(
            @PathVariable Integer classSectionId,
            @RequestParam(value = "pageNumber", defaultValue = "1") Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize
    ) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, Sort.by("id").descending());
        PageResponse<EnrollmentResponse> response = enrollmentService.getStudentsApprovedInClassSection(classSectionId, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get pending enrollments by course")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/courses/{courseId}/enrollments/pending")
    public ResponseEntity<PageResponse<EnrollmentResponse>> getStudentsPendingEnrollment(
            @PathVariable Integer courseId,
            @RequestParam(value = "pageNumber", defaultValue = "1") Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize
    ) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, Sort.by("id").descending());
        PageResponse<EnrollmentResponse> response = enrollmentService.getStudentsPendingEnrollment(courseId, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get pending enrollments by class section")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/class-sections/{classSectionId}/enrollments/pending")
    public ResponseEntity<PageResponse<EnrollmentResponse>> getStudentsPendingInClassSection(
            @PathVariable Integer classSectionId,
            @RequestParam(value = "pageNumber", defaultValue = "1") Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize
    ) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, Sort.by("id").descending());
        PageResponse<EnrollmentResponse> response = enrollmentService.getStudentsPendingInClassSection(classSectionId, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all enrollments (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/enrollments")
    public ResponseEntity<PageResponse<EnrollmentResponse>> getEnrollmentPage(
            @RequestParam(value = "pageNumber", defaultValue = "1") Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize
    ) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        PageResponse<EnrollmentResponse> response = enrollmentService.getEnrollmentPage(pageable);
        return ResponseEntity.ok(response);
    }

    //      todo: fix this API trash as hell
    @Operation(summary = "Get teacher enrollments")
    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/teacher/enrollments")
    public ResponseEntity<PageResponse<EnrollmentResponse>> getTeacherEnrollments(
            @RequestParam(value = "classSectionId", required = false) Integer classSectionId,
            @RequestParam(value = "approvalStatus", required = false) String approvalStatus,
            @RequestParam(value = "pageNumber", defaultValue = "1") Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize
    ) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        PageResponse<EnrollmentResponse> response = enrollmentService.getTeacherEnrollments(
                classSectionId,
                approvalStatus,
                pageable
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Search students in course")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/courses/{courseId}/students/available")
    public ResponseEntity<PageResponse<UserViewResponse>> getStudentsInCourse(
            @PathVariable Integer courseId,
            SearchUserRequest request,
            @RequestParam(value = "pageNumber", defaultValue = "1") Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "5") Integer pageSize
    ) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        PageResponse<UserViewResponse> response = enrollmentService.searchStudentsInCourse(courseId, request, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Search students not in course")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/courses/{courseId}/students/not-available")
    public ResponseEntity<PageResponse<UserViewResponse>> getStudentsNotInCourse(
            @PathVariable Integer courseId,
            SearchUserRequest request,
            @RequestParam(value = "pageNumber", defaultValue = "1") Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "5") Integer pageSize
    ) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        PageResponse<UserViewResponse> response = enrollmentService.searchStudentsNotInCourse(courseId, request, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Search students not in class section")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/class-sections/{classSectionId}/students/not-available")
    public ResponseEntity<PageResponse<UserViewResponse>> getStudentsNotInClassSection(
            @PathVariable Integer classSectionId,
            SearchUserRequest request,
            @RequestParam(value = "pageNumber", defaultValue = "1") Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "5") Integer pageSize
    ) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        PageResponse<UserViewResponse> response = enrollmentService.searchStudentsNotInClassSection(classSectionId, request, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Add students to course")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/courses/{courseId}/students")
    public ResponseEntity<?> addStudentsToCourse(@PathVariable Integer courseId, @RequestBody StudentCourseRequest request) {
        enrollmentService.addStudentsToCourse(courseId, request);
        Map<String, String> payload = new HashMap<>();
        payload.put("code", "201");
        payload.put("message", "Students has been added successfully");
        return ResponseEntity.status(HttpStatus.CREATED).body(payload);
    }

    @Operation(summary = "Add students to class section")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/class-sections/{classSectionId}/students")
    public ResponseEntity<?> addStudentsToClassSection(@PathVariable Integer classSectionId, @RequestBody StudentCourseRequest request) {
        enrollmentService.addStudentsToClassSection(classSectionId, request);
        Map<String, String> payload = new HashMap<>();
        payload.put("code", "201");
        payload.put("message", "Students has been added successfully");
        return ResponseEntity.status(HttpStatus.CREATED).body(payload);
    }

    @Operation(summary = "Remove students from course")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @DeleteMapping("/courses/{courseId}/students")
    public ResponseEntity<?> removeStudentsInCourse(@PathVariable Integer courseId, @RequestBody StudentCourseRequest request) {
        enrollmentService.removeStudentsInCourse(courseId, request);
        Map<String, String> payload = new HashMap<>();
        payload.put("code", "200");
        payload.put("message", "Students has been deleted successfully");
        return ResponseEntity.ok(payload);
    }

    @Operation(summary = "Remove students from class section")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @DeleteMapping("/class-sections/{classSectionId}/students")
    public ResponseEntity<?> removeStudentsFromClassSection(@PathVariable Integer classSectionId, @RequestBody StudentCourseRequest request) {
        enrollmentService.removeStudentsFromClassSection(classSectionId, request);
        Map<String, String> payload = new HashMap<>();
        payload.put("code", "200");
        payload.put("message", "Students has been deleted successfully");
        return ResponseEntity.ok(payload);
    }
}
