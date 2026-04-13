package com.example.backend.service;

import com.example.backend.dto.request.AssignmentRequest;
import com.example.backend.dto.response.AssignmentResponse;
import com.example.backend.dto.response.PageResponse;

public interface AssignmentService {
    AssignmentResponse createAssignment(AssignmentRequest request);

    AssignmentResponse updateAssignment(Integer id, AssignmentRequest request);

    AssignmentResponse getAssignmentById(Integer id);

    PageResponse<AssignmentResponse> getAssignmentsByClassSection(Integer classSectionId);

    void deleteAssignment(Integer id);
}
