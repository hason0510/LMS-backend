package com.example.backend.controller;

import com.example.backend.dto.response.classsection.ClassSectionResponse;
import com.example.backend.dto.response.teaching.ClassPeopleRowResponse;
import com.example.backend.dto.response.teaching.TeachingContextResponse;
import com.example.backend.dto.response.teaching.TeachingReviewQueueItemResponse;
import com.example.backend.dto.response.teaching.TeachingWorkbenchSummaryResponse;
import com.example.backend.service.TeachingWorkbenchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lms")
@RequiredArgsConstructor
@Tag(name = "Teaching Workbench")
public class TeachingWorkbenchController {
    private final TeachingWorkbenchService teachingWorkbenchService;

    @Operation(summary = "Get teaching workspace context for current user")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/teaching/context")
    public ResponseEntity<TeachingContextResponse> getTeachingContext() {
        return ResponseEntity.ok(teachingWorkbenchService.getTeachingContext());
    }

    @Operation(summary = "Get classes where current user is teacher or TA")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/teaching/my-classes")
    public ResponseEntity<List<ClassSectionResponse>> getMyTeachingClasses() {
        return ResponseEntity.ok(teachingWorkbenchService.getMyTeachingClasses());
    }

    @Operation(summary = "Get teaching workbench summary")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/teaching/workbench/summary")
    public ResponseEntity<TeachingWorkbenchSummaryResponse> getGlobalSummary() {
        return ResponseEntity.ok(teachingWorkbenchService.getSummary(null));
    }

    @Operation(summary = "Get class workbench summary")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/class-sections/{id}/workbench/summary")
    public ResponseEntity<TeachingWorkbenchSummaryResponse> getClassSummary(@PathVariable Integer id) {
        return ResponseEntity.ok(teachingWorkbenchService.getSummary(id));
    }

    @Operation(summary = "Get global review queue for teaching workspace")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/teaching/workbench/review-queue")
    public ResponseEntity<List<TeachingReviewQueueItemResponse>> getGlobalReviewQueue() {
        return ResponseEntity.ok(teachingWorkbenchService.getReviewQueue());
    }

    @Operation(summary = "Get review queue")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/class-sections/{id}/workbench/review-queue")
    public ResponseEntity<List<TeachingReviewQueueItemResponse>> getReviewQueue(@PathVariable Integer id) {
        return ResponseEntity.ok(teachingWorkbenchService.getReviewQueue(id));
    }

    @Operation(summary = "Get class people rows for teaching workspace")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/class-sections/{id}/people")
    public ResponseEntity<List<ClassPeopleRowResponse>> getClassPeople(
            @PathVariable Integer id,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(teachingWorkbenchService.getClassPeople(id, status));
    }
}
