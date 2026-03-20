package com.example.backend.service;

import com.example.backend.dto.request.EnrollmentRequest;
import com.example.backend.dto.request.course.CourseRatingRequest;
import com.example.backend.dto.request.course.StudentCourseRequest;
import com.example.backend.dto.request.search.SearchUserRequest;
import com.example.backend.dto.response.EnrollmentResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.course.CourseRatingResponse;
import com.example.backend.dto.response.course.CourseResponse;
import com.example.backend.dto.response.user.UserViewResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public interface EnrollmentService {
    @Transactional
    void addStudentsToCourse(Integer courseId, StudentCourseRequest request);

    @Transactional
    void removeStudentsInCourse(Integer courseId, StudentCourseRequest request);

    EnrollmentResponse enrollPublicCourse(Integer courseId);

    EnrollmentResponse enrollPrivateCourse(String classCode);

    CourseRatingResponse ratingCourse(Integer courseId, CourseRatingRequest request);

    void deleteReview(Integer courseId);

    PageResponse<CourseRatingResponse> getAllCourseRatings(Integer courseId, Integer ratingValue, Pageable pageable);

    @Transactional
    void completeLesson(Integer lessonId);

    EnrollmentResponse approveStudentToEnrollment(EnrollmentRequest request);

    void rejectStudentEnrollment(EnrollmentRequest request);

    PageResponse<EnrollmentResponse> getStudentsApprovedInEnrollment(Integer courseId, Pageable pageable);

    PageResponse<EnrollmentResponse> getStudentsPendingEnrollment(Integer courseId, Pageable pageable);

    EnrollmentResponse getCurrentUserProgressByCourse(Integer courseId);

    EnrollmentResponse getEnrollmentById(Integer id);

    PageResponse<EnrollmentResponse> getEnrollmentPage(Pageable pageable);

    PageResponse<UserViewResponse> searchStudentsInCourse(Integer courseId, SearchUserRequest request, Pageable pageable);

    PageResponse<UserViewResponse> searchStudentsNotInCourse(Integer courseId, SearchUserRequest request, Pageable pageable);

    void recalculateAndSaveProgress(Integer studentId, Integer courseId);

    PageResponse<EnrollmentResponse> getTeacherEnrollments(Integer teacherId, Integer courseId, String approvalStatus, Pageable pageable);
}
