package com.example.backend.service;

import com.example.backend.dto.request.course.CourseRequest;
import com.example.backend.dto.request.course.StudentCourseRequest;
import com.example.backend.dto.request.search.SearchUserRequest;
import com.example.backend.dto.response.CloudinaryResponse;
import com.example.backend.dto.response.course.CourseResponse;
import com.example.backend.dto.response.EnrollmentStatusResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.user.UserViewResponse;
import com.example.backend.entity.Course;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CourseService {
    CourseResponse createCourse(CourseRequest courseRequest);

    CourseResponse updateCourse(Integer id, CourseRequest courseRequest);

    void deleteCourseById(Integer id);

    CourseResponse getCourseById(Integer id);

    PageResponse<CourseResponse> getCoursesApprovedByStudent(Pageable pageable);

    PageResponse<CourseResponse> getAllPublicCourses(Pageable pageable);

    PageResponse<CourseResponse> getCoursesPendingByStudent(Pageable pageable);

    PageResponse<CourseResponse> getAllCoursesByTeacher(Pageable pageable);

    PageResponse<CourseResponse> getAllCourses(Pageable pageable);

    CloudinaryResponse uploadImage(final Integer id, final MultipartFile file);

    CourseResponse convertEntityToDto(Course course);

    CourseResponse publishCourse(Integer id);

    EnrollmentStatusResponse checkEnrollmentStatus(Integer courseId);
}
