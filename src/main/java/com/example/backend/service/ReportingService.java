package com.example.backend.service;

import com.example.backend.dto.response.reporting.AdminReportSummaryResponse;
import com.example.backend.dto.response.reporting.AssignmentReportResponse;
import com.example.backend.dto.response.reporting.ClassReportOverviewResponse;
import com.example.backend.dto.response.reporting.QuizReportResponse;
import com.example.backend.dto.response.reporting.TeacherReportSummaryResponse;

public interface ReportingService {
    AdminReportSummaryResponse getAdminReportSummary();

    TeacherReportSummaryResponse getTeacherReportSummary();

    ClassReportOverviewResponse getClassReportOverview(Integer classSectionId, int lowThreshold, int highThreshold);

    AssignmentReportResponse getAssignmentReport(Integer classSectionId);

    QuizReportResponse getQuizReport(Integer classSectionId);
}
