package com.example.backend.service;

import com.example.backend.constant.SubmissionStatus;
import com.example.backend.dto.request.SubmissionGradeRequest;
import com.example.backend.dto.request.SubmissionRequest;
import com.example.backend.dto.request.SubmissionReturnRequest;
import com.example.backend.dto.response.SubmissionResponse;

import java.util.List;

public interface SubmissionService {
    SubmissionResponse submitAssignment(Integer assignmentId, Integer classSectionId, SubmissionRequest request);

    SubmissionResponse getMySubmission(Integer assignmentId, Integer classSectionId);

    List<SubmissionResponse> getMySubmissions(Integer classSectionId, SubmissionStatus status);

    List<SubmissionResponse> getAssignmentSubmissions(Integer assignmentId, Integer classSectionId, boolean includeNotSubmitted);

    SubmissionResponse gradeSubmission(Integer submissionId, SubmissionGradeRequest request);

    SubmissionResponse returnSubmission(Integer submissionId, SubmissionReturnRequest request);

    SubmissionResponse getSubmissionById(Integer submissionId);
}
