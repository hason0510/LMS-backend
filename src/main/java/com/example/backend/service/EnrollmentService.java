package com.example.backend.service;

import com.example.backend.dto.request.EnrollmentRequest;
import com.example.backend.dto.request.course.StudentCourseRequest;
import com.example.backend.dto.request.search.SearchUserRequest;
import com.example.backend.dto.response.EnrollmentResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.user.UserViewResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public interface EnrollmentService {
    @Transactional
    void addStudentsToCourse(Integer courseId, StudentCourseRequest request);

    @Transactional
    void addStudentsToClassSection(Integer classSectionId, StudentCourseRequest request);

    @Transactional
    void removeStudentsInCourse(Integer courseId, StudentCourseRequest request);

    @Transactional
    void removeStudentsFromClassSection(Integer classSectionId, StudentCourseRequest request);

    EnrollmentResponse enrollPublicCourse(Integer courseId);

    EnrollmentResponse enrollPrivateCourse(String classCode);

    EnrollmentResponse enrollClassSection(Integer classSectionId);

    EnrollmentResponse enrollClassSectionByCode(String classCode);

    @Transactional
    void completeLesson(Integer lessonId);

    @Transactional
    void completeClassContentItem(Integer classContentItemId);

    EnrollmentResponse approveStudentToEnrollment(EnrollmentRequest request);

    void rejectStudentEnrollment(EnrollmentRequest request);

    PageResponse<EnrollmentResponse> getStudentsApprovedInEnrollment(Integer courseId, Pageable pageable);

    PageResponse<EnrollmentResponse> getStudentsApprovedInClassSection(Integer classSectionId, String keyword, Pageable pageable);

    PageResponse<EnrollmentResponse> getStudentsPendingEnrollment(Integer courseId, Pageable pageable);

    PageResponse<EnrollmentResponse> getStudentsPendingInClassSection(Integer classSectionId, Pageable pageable);

    EnrollmentResponse getCurrentUserProgressByCourse(Integer courseId);

    EnrollmentResponse getCurrentUserProgressByClassSection(Integer classSectionId);

    EnrollmentResponse getEnrollmentById(Integer id);

    PageResponse<EnrollmentResponse> getEnrollmentPage(Pageable pageable);

    PageResponse<EnrollmentResponse> getEnrollmentPage(String approvalStatus, Pageable pageable);

    PageResponse<UserViewResponse> searchStudentsInCourse(Integer courseId, SearchUserRequest request, Pageable pageable);

    PageResponse<UserViewResponse> searchStudentsNotInCourse(Integer courseId, SearchUserRequest request, Pageable pageable);

    PageResponse<UserViewResponse> searchStudentsNotInClassSection(Integer classSectionId, SearchUserRequest request, Pageable pageable);

    void recalculateAndSaveProgress(Integer studentId, Integer courseId);

    void recalculateAndSaveProgressForClassSection(Integer studentId, Integer classSectionId);

    PageResponse<EnrollmentResponse> getTeacherEnrollments(Integer classSectionId, String approvalStatus, Pageable pageable);
}
