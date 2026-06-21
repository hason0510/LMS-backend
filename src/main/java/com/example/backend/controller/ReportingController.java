package com.example.backend.controller;

import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.reporting.AdminReportSummaryResponse;
import com.example.backend.dto.response.reporting.AssignmentReportResponse;
import com.example.backend.dto.response.reporting.ClassReportOverviewResponse;
import com.example.backend.dto.response.reporting.QuizReportResponse;
import com.example.backend.dto.response.reporting.TeacherReportSummaryResponse;
import com.example.backend.service.ReportingService;
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

@RestController
@RequestMapping("/api/v1/lms")
@RequiredArgsConstructor
@Tag(name = "Reporting Aggregate")
public class ReportingController {

    private final ReportingService reportingService;

    @Operation(summary = "Get admin system-wide report summary")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/reports/summary")
    public ResponseEntity<AdminReportSummaryResponse> getAdminReportSummary() {
        return ResponseEntity.ok(reportingService.getAdminReportSummary());
    }

    @Operation(summary = "Paginated teacher load (classes per teacher) with search + sort")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/reports/teacher-load")
    public ResponseEntity<PageResponse<AdminReportSummaryResponse.TeacherLoadItem>> getTeacherLoad(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "desc") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "8") int size) {
        return ResponseEntity.ok(reportingService.getTeacherLoad(search, sort, page, size));
    }

    @Operation(summary = "Paginated subject load (classes per subject) with search + sort")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/reports/subject-load")
    public ResponseEntity<PageResponse<AdminReportSummaryResponse.SubjectLoadItem>> getSubjectLoad(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "desc") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "8") int size) {
        return ResponseEntity.ok(reportingService.getSubjectLoad(search, sort, page, size));
    }

    @Operation(summary = "Paginated assistant (TA) list with the classes they support; search by name/email/class")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/reports/assistants")
    public ResponseEntity<PageResponse<AdminReportSummaryResponse.AssistantClassesItem>> getAssistants(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(reportingService.getAssistantList(search, page, size));
    }

    @Operation(summary = "Get teacher-scope report summary across all classes the user teaches or assists")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/teacher/reports/summary")
    public ResponseEntity<TeacherReportSummaryResponse> getTeacherReportSummary() {
        return ResponseEntity.ok(reportingService.getTeacherReportSummary());
    }

    @Operation(summary = "Get class report overview")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/class-sections/{id}/reports/overview")
    public ResponseEntity<ClassReportOverviewResponse> getClassReportOverview(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "40") int lowThreshold,
            @RequestParam(defaultValue = "80") int highThreshold) {
        return ResponseEntity.ok(reportingService.getClassReportOverview(id, lowThreshold, highThreshold));
    }

    @Operation(summary = "Get class assignment report")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/class-sections/{id}/reports/assignments")
    public ResponseEntity<AssignmentReportResponse> getClassAssignmentReport(@PathVariable Integer id) {
        return ResponseEntity.ok(reportingService.getAssignmentReport(id));
    }

    @Operation(summary = "Get class quiz report")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/class-sections/{id}/reports/quizzes")
    public ResponseEntity<QuizReportResponse> getClassQuizReport(@PathVariable Integer id) {
        return ResponseEntity.ok(reportingService.getQuizReport(id));
    }
}
