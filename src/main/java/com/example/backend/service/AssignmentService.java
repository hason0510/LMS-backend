package com.example.backend.service;

import com.example.backend.dto.request.AssignmentRequest;
import com.example.backend.dto.response.AssignmentResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.assignment.StudentAssignmentFeedResponse;
import com.example.backend.dto.response.assignment.TeacherAssignmentOverviewResponse;

public interface AssignmentService {
    AssignmentResponse createAssignment(AssignmentRequest request);

    AssignmentResponse updateAssignment(Integer id, AssignmentRequest request);

    AssignmentResponse getAssignmentById(Integer id);
    AssignmentResponse getAssignmentById(Integer id, Integer classContentItemId);

    PageResponse<AssignmentResponse> getAssignmentsByClassSection(Integer classSectionId);

    PageResponse<StudentAssignmentFeedResponse> getStudentAssignmentFeed(String tab, String keyword, Integer classSectionId);

    PageResponse<TeacherAssignmentOverviewResponse> getTeachingAssignments(String tab, String keyword, Integer classSectionId);

    void deleteAssignment(Integer id);
}
