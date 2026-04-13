package com.example.backend.controller;

import com.example.backend.constant.SubmissionStatus;
import com.example.backend.dto.request.SubmissionGradeRequest;
import com.example.backend.dto.request.SubmissionRequest;
import com.example.backend.dto.request.SubmissionReturnRequest;
import com.example.backend.dto.response.SubmissionResponse;
import com.example.backend.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lms/submissions")
@RequiredArgsConstructor
@Tag(name = "Assignment Submission Management", description = "APIs for student submission and teacher grading workflows")
public class SubmissionController {

    private final SubmissionService submissionService;

    @Operation(summary = "Student submit or resubmit assignment")
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/assignments/{assignmentId}/me")
    public ResponseEntity<SubmissionResponse> submitAssignment(
            @PathVariable Integer assignmentId,
            @RequestParam(required = false) Integer classSectionId,
            @Valid @RequestBody SubmissionRequest request
    ) {
        return ResponseEntity.ok(submissionService.submitAssignment(assignmentId, classSectionId, request));
    }

    @Operation(summary = "Student get own submission for an assignment")
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/assignments/{assignmentId}/me")
    public ResponseEntity<SubmissionResponse> getMySubmission(
            @PathVariable Integer assignmentId,
            @RequestParam(required = false) Integer classSectionId
    ) {
        return ResponseEntity.ok(submissionService.getMySubmission(assignmentId, classSectionId));
    }

    @Operation(summary = "Student get own submission list")
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/me")
    public ResponseEntity<List<SubmissionResponse>> getMySubmissions(
            @RequestParam(required = false) Integer classSectionId,
            @RequestParam(required = false) SubmissionStatus status
    ) {
        return ResponseEntity.ok(submissionService.getMySubmissions(classSectionId, status));
    }

    @Operation(summary = "Teacher get assignment submission list")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/assignments/{assignmentId}")
    public ResponseEntity<List<SubmissionResponse>> getAssignmentSubmissions(
            @PathVariable Integer assignmentId,
            @RequestParam(required = false) Integer classSectionId,
            @RequestParam(defaultValue = "true") boolean includeNotSubmitted
    ) {
        return ResponseEntity.ok(
                submissionService.getAssignmentSubmissions(assignmentId, classSectionId, includeNotSubmitted)
        );
    }

    @Operation(summary = "Teacher grade submission")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{submissionId}/grade")
    public ResponseEntity<SubmissionResponse> gradeSubmission(
            @PathVariable Integer submissionId,
            @Valid @RequestBody SubmissionGradeRequest request
    ) {
        return ResponseEntity.ok(submissionService.gradeSubmission(submissionId, request));
    }

    @Operation(summary = "Teacher return submission for revision")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{submissionId}/return")
    public ResponseEntity<SubmissionResponse> returnSubmission(
            @PathVariable Integer submissionId,
            @Valid @RequestBody SubmissionReturnRequest request
    ) {
        return ResponseEntity.ok(submissionService.returnSubmission(submissionId, request));
    }

    @Operation(summary = "Get submission by id")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{submissionId}")
    public ResponseEntity<SubmissionResponse> getSubmissionById(@PathVariable Integer submissionId) {
        return ResponseEntity.ok(submissionService.getSubmissionById(submissionId));
    }
}
