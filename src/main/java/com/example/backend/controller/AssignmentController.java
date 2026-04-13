package com.example.backend.controller;

import com.example.backend.dto.request.AssignmentRequest;
import com.example.backend.dto.response.AssignmentResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.service.AssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/lms/assignments")
@RequiredArgsConstructor
@Tag(name = "Assignment Management", description = "APIs for assignment setup and retrieval")
public class AssignmentController {
    private final AssignmentService assignmentService;

    @Operation(summary = "Create assignment")
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<AssignmentResponse> createAssignment(@Valid @RequestBody AssignmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assignmentService.createAssignment(request));
    }

    @Operation(summary = "Update assignment")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}")
    public ResponseEntity<AssignmentResponse> updateAssignment(
            @PathVariable Integer id,
            @Valid @RequestBody AssignmentRequest request
    ) {
        return ResponseEntity.ok(assignmentService.updateAssignment(id, request));
    }

    @Operation(summary = "Get assignment by id")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<AssignmentResponse> getAssignmentById(@PathVariable Integer id) {
        return ResponseEntity.ok(assignmentService.getAssignmentById(id));
    }

    @Operation(summary = "Get assignments in class section")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/class-sections/{classSectionId}")
    public ResponseEntity<PageResponse<AssignmentResponse>> getAssignmentsByClassSection(@PathVariable Integer classSectionId) {
        return ResponseEntity.ok(assignmentService.getAssignmentsByClassSection(classSectionId));
    }

    @Operation(summary = "Delete assignment")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAssignment(@PathVariable Integer id) {
        assignmentService.deleteAssignment(id);
        return ResponseEntity.noContent().build();
    }
}
