package com.example.backend.service.impl;

import com.example.backend.cache.CacheNames;
import com.example.backend.constant.ClassMemberRole;
import com.example.backend.constant.EnrollmentStatus;
import com.example.backend.constant.RoleType;
import com.example.backend.constant.SubmissionStatus;
import com.example.backend.dto.response.reporting.AdminReportSummaryResponse;
import com.example.backend.dto.response.reporting.AssignmentReportResponse;
import com.example.backend.dto.response.reporting.ClassReportOverviewResponse;
import com.example.backend.dto.response.reporting.QuizReportResponse;
import com.example.backend.dto.response.reporting.TeacherReportSummaryResponse;
import com.example.backend.entity.ClassMember;
import com.example.backend.entity.ClassSection;
import com.example.backend.entity.User;
import com.example.backend.entity.assignment.Assignment;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.repository.AssignmentRepository;
import com.example.backend.repository.ClassContentItemRepository;
import com.example.backend.repository.ClassMemberRepository;
import com.example.backend.repository.ClassSectionRepository;
import com.example.backend.repository.EnrollmentRepository;
import com.example.backend.repository.SubjectRepository;
import com.example.backend.repository.SubmissionRepository;
import com.example.backend.repository.QuizAttemptRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.ClassMemberAuthorizationService;
import com.example.backend.service.ReportingService;
import com.example.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReportingServiceImpl implements ReportingService {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final int TEACHER_LOAD_LIMIT = 6;
    private static final int SUBJECT_LOAD_LIMIT = 6;
    private static final int TOP_CLASSES_LIMIT = 5;
    private static final int ACTIVE_ASSISTANTS_LIMIT = 6;

    private final UserRepository userRepository;
    private final ClassSectionRepository classSectionRepository;
    private final ClassMemberRepository classMemberRepository;
    private final SubjectRepository subjectRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final ClassContentItemRepository classContentItemRepository;
    private final ClassMemberAuthorizationService classMemberAuthorizationService;
    private final UserService userService;


    @Override
    @Transactional(readOnly = true)
   // @Cacheable(value = CacheNames.ADMIN_REPORT_SUMMARY)
    public AdminReportSummaryResponse getAdminReportSummary() {
        requireAdmin();

        AdminReportSummaryResponse response = new AdminReportSummaryResponse();

        response.setTotalUsers(userRepository.count());
        response.setTotalClassSections(classSectionRepository.count());
        response.setTotalSubjects(subjectRepository.count());
        response.setTotalTeachers(userRepository.countByRoleName(RoleType.TEACHER));
        response.setTotalAssistants(classMemberRepository.countDistinctUsersByRole(ClassMemberRole.TA));
        response.setTotalStudents(enrollmentRepository.countTotalApprovedStudents());
        response.setPendingEnrollments(enrollmentRepository
                .findByApprovalStatus(EnrollmentStatus.PENDING, org.springframework.data.domain.Pageable.unpaged())
                .getTotalElements());

        // status breakdown
        List<AdminReportSummaryResponse.StatusBreakdownItem> statusBreakdown = new ArrayList<>();
        Map<String, Long> statusFill = new LinkedHashMap<>();
        statusFill.put("PUBLIC", 0L);
        statusFill.put("PRIVATE", 0L);
        statusFill.put("ARCHIVED", 0L);
        for (Object[] row : classSectionRepository.countGroupByStatus()) {
            String status = row[0] == null ? "PRIVATE" : String.valueOf(row[0]);
            long count = ((Number) row[1]).longValue();
            statusFill.put(status, statusFill.getOrDefault(status, 0L) + count);
        }
        statusFill.forEach((status, count) ->
                statusBreakdown.add(new AdminReportSummaryResponse.StatusBreakdownItem(status, count)));
        response.setClassStatusBreakdown(statusBreakdown);

        // teacher load (top N)
        List<AdminReportSummaryResponse.TeacherLoadItem> teacherLoad = new ArrayList<>();
        int teacherCount = 0;
        for (Object[] row : classSectionRepository.findTeacherLoadProjection()) {
            if (teacherCount >= TEACHER_LOAD_LIMIT) {
                break;
            }
            Integer teacherId = (Integer) row[0];
            String teacherName = (String) row[1];
            long classCount = ((Number) row[2]).longValue();
            long studentCount = ((Number) row[3]).longValue();
            teacherLoad.add(new AdminReportSummaryResponse.TeacherLoadItem(
                    teacherId, teacherName, classCount, studentCount));
            teacherCount++;
        }
        response.setTeacherLoad(teacherLoad);

        // subject load (top N)
        List<AdminReportSummaryResponse.SubjectLoadItem> subjectLoad = new ArrayList<>();
        int subjectCount = 0;
        for (Object[] row : classSectionRepository.findSubjectLoadProjection()) {
            if (subjectCount >= SUBJECT_LOAD_LIMIT) {
                break;
            }
            Integer subjectId = (Integer) row[0];
            String subjectTitle = (String) row[1];
            long count = ((Number) row[2]).longValue();
            subjectLoad.add(new AdminReportSummaryResponse.SubjectLoadItem(
                    subjectId, subjectTitle, count));
            subjectCount++;
        }
        response.setSubjectLoad(subjectLoad);

        // top classes
        List<Object[]> topRows = classSectionRepository.findTopClassesByApprovedEnrollment(
                PageRequest.of(0, TOP_CLASSES_LIMIT));
        List<Integer> topClassIds = new ArrayList<>();
        List<AdminReportSummaryResponse.TopClassItem> topClassItems = new ArrayList<>();
        for (Object[] row : topRows) {
            Integer classSectionId = (Integer) row[0];
            String title = (String) row[1];
            String classCode = (String) row[2];
            String status = row[3] == null ? null : row[3].toString();
            String subjectTitle = (String) row[4];
            Integer teacherId = (Integer) row[5];
            String teacherName = (String) row[6];
            long totalEnrollments = ((Number) row[7]).longValue();

            topClassIds.add(classSectionId);
            AdminReportSummaryResponse.TopClassItem item = new AdminReportSummaryResponse.TopClassItem();
            item.setClassSectionId(classSectionId);
            item.setTitle(title);
            item.setClassCode(classCode);
            item.setStatus(status);
            item.setSubjectTitle(subjectTitle);
            item.setTeacherId(teacherId);
            item.setTeacherName(teacherName);
            item.setTotalEnrollments(totalEnrollments);
            item.setTaCount(0L);
            topClassItems.add(item);
        }

        // attach TA count for top classes (single query)
        if (!topClassIds.isEmpty()) {
            Map<Integer, Long> taCountMap = new HashMap<>();
            for (Object[] row : classMemberRepository.countByRoleGroupedByClassSection(
                    ClassMemberRole.TA, topClassIds)) {
                Integer classSectionId = (Integer) row[0];
                long taCount = ((Number) row[1]).longValue();
                taCountMap.put(classSectionId, taCount);
            }
            for (AdminReportSummaryResponse.TopClassItem item : topClassItems) {
                item.setTaCount(taCountMap.getOrDefault(item.getClassSectionId(), 0L));
            }
        }
        response.setTopClasses(topClassItems);

        // active assistants (TAs with their assisting classes)
        Map<Integer, AdminReportSummaryResponse.AssistantClassesItem> assistantMap = new LinkedHashMap<>();
        for (Object[] row : classMemberRepository.findAssistantsWithClasses(ClassMemberRole.TA)) {
            Integer userId = (Integer) row[0];
            if (userId == null) {
                continue;
            }
            AdminReportSummaryResponse.AssistantClassesItem assistant = assistantMap.computeIfAbsent(
                    userId,
                    key -> new AdminReportSummaryResponse.AssistantClassesItem(
                            userId,
                            (String) row[1],
                            (String) row[2],
                            (String) row[3],
                            new ArrayList<>())
            );
            Integer classSectionId = (Integer) row[4];
            String classTitle = (String) row[5];
            String classCode = (String) row[6];
            assistant.getClasses().add(new AdminReportSummaryResponse.AssistantClassRef(
                    classSectionId, classTitle, classCode));
        }
        List<AdminReportSummaryResponse.AssistantClassesItem> assistantList = new ArrayList<>(assistantMap.values());
        assistantList.sort((a, b) -> Integer.compare(b.getClasses().size(), a.getClasses().size()));
        if (assistantList.size() > ACTIVE_ASSISTANTS_LIMIT) {
            assistantList = assistantList.subList(0, ACTIVE_ASSISTANTS_LIMIT);
        }
        response.setAssistantsActive(assistantList);

        // pending submissions / quiz reviews across whole system (admin scope ⇒ all class sections)
        Set<Integer> allClassSectionIds = new HashSet<>();
        for (ClassSection cs : classSectionRepository.findAll()) {
            allClassSectionIds.add(cs.getId());
        }
        if (!allClassSectionIds.isEmpty()) {
            response.setPendingSubmissions(
                    submissionRepository.countPendingSubmissionsInClassSections(allClassSectionIds));
            response.setPendingQuizReviews(
                    quizAttemptRepository.countPendingQuizReviewsInClassSections(allClassSectionIds));
        }

        return response;
    }

    // ────────────────────────────────────────────────────────────────────
    // Teacher scope
    // ────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public TeacherReportSummaryResponse getTeacherReportSummary() {
        User currentUser = requireCurrentUser();
        Set<Integer> taughtClassIds = new HashSet<>();
        for (ClassSection cs : classSectionRepository.findByTeacher_Id(currentUser.getId())) {
            taughtClassIds.add(cs.getId());
        }
        // Add class-member TEACHER role (some teachers may be assigned via members table only)
        for (ClassSection cs : classMemberRepository.findClassSectionsByMemberRole(
                currentUser.getId(), ClassMemberRole.TEACHER)) {
            taughtClassIds.add(cs.getId());
        }
        Set<Integer> assistedClassIds = new HashSet<>();
        for (ClassSection cs : classMemberRepository.findClassSectionsByMemberRole(
                currentUser.getId(), ClassMemberRole.TA)) {
            assistedClassIds.add(cs.getId());
        }
        Set<Integer> allClassIds = new HashSet<>();
        allClassIds.addAll(taughtClassIds);
        allClassIds.addAll(assistedClassIds);

        TeacherReportSummaryResponse response = new TeacherReportSummaryResponse();
        response.setTotalClasses(allClassIds.size());

        if (allClassIds.isEmpty()) {
            response.setTotalStudents(0);
            response.setPendingSubmissions(0);
            response.setPendingQuizReviews(0);
            response.setAtRiskStudents(0);
            response.setPendingRequests(0);
            response.setTaughtClasses(new ArrayList<>());
            response.setAssistedClasses(new ArrayList<>());
            return response;
        }

        response.setTotalStudents(enrollmentRepository.countTotalApprovedStudentsInClassSections(allClassIds));
        response.setPendingSubmissions(submissionRepository.countPendingSubmissionsInClassSections(allClassIds));
        response.setPendingQuizReviews(quizAttemptRepository.countPendingQuizReviewsInClassSections(allClassIds));
        response.setAtRiskStudents(enrollmentRepository.countAtRiskStudentsInClassSections(allClassIds, 50));
        response.setPendingRequests(enrollmentRepository.countPendingRequestsInClassSections(allClassIds));

        // taught classes details
        List<TeacherReportSummaryResponse.TaughtClassItem> taughtList = new ArrayList<>();
        if (!taughtClassIds.isEmpty()) {
            Map<Integer, Long> taCountByClass = new HashMap<>();
            for (Object[] row : classMemberRepository.countByRoleGroupedByClassSection(
                    ClassMemberRole.TA, taughtClassIds)) {
                taCountByClass.put((Integer) row[0], ((Number) row[1]).longValue());
            }
            for (Integer classId : taughtClassIds) {
                ClassSection cs = classSectionRepository.findById(classId).orElse(null);
                if (cs == null) {
                    continue;
                }
                long studentCount = enrollmentRepository.countByClassSection_IdAndApprovalStatus(
                        classId, EnrollmentStatus.APPROVED);
                TeacherReportSummaryResponse.TaughtClassItem item = new TeacherReportSummaryResponse.TaughtClassItem();
                item.setClassSectionId(cs.getId());
                item.setTitle(cs.getTitle());
                item.setClassCode(cs.getClassCode());
                item.setStatus(cs.getStatus() == null ? null : cs.getStatus().name());
                item.setSubjectTitle(cs.getSubject() == null ? null : cs.getSubject().getTitle());
                item.setTotalEnrollments(studentCount);
                item.setTaCount(taCountByClass.getOrDefault(classId, 0L));
                taughtList.add(item);
            }
            taughtList.sort((a, b) -> Long.compare(b.getTotalEnrollments(), a.getTotalEnrollments()));
        }
        response.setTaughtClasses(taughtList);

        // assisted classes details — show "ai đang trợ giảng lớp nào" của chính user nhìn từ phía teacher
        List<TeacherReportSummaryResponse.AssistedClassItem> assistedList = new ArrayList<>();
        for (Integer classId : assistedClassIds) {
            ClassSection cs = classSectionRepository.findById(classId).orElse(null);
            if (cs == null) {
                continue;
            }
            long studentCount = enrollmentRepository.countByClassSection_IdAndApprovalStatus(
                    classId, EnrollmentStatus.APPROVED);
            String primaryTeacherName = cs.getTeacher() == null ? null : cs.getTeacher().getFullName();
            TeacherReportSummaryResponse.AssistedClassItem item = new TeacherReportSummaryResponse.AssistedClassItem();
            item.setClassSectionId(cs.getId());
            item.setTitle(cs.getTitle());
            item.setClassCode(cs.getClassCode());
            item.setStatus(cs.getStatus() == null ? null : cs.getStatus().name());
            item.setSubjectTitle(cs.getSubject() == null ? null : cs.getSubject().getTitle());
            item.setPrimaryTeacherName(primaryTeacherName);
            item.setTotalEnrollments(studentCount);
            assistedList.add(item);
        }
        assistedList.sort((a, b) -> Long.compare(b.getTotalEnrollments(), a.getTotalEnrollments()));
        response.setAssistedClasses(assistedList);

        return response;
    }


    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.CLASS_REPORT_OVERVIEW, key = "#classSectionId + ':' + #lowThreshold + ':' + #highThreshold")
    public ClassReportOverviewResponse getClassReportOverview(Integer classSectionId,
                                                              int lowThreshold,
                                                              int highThreshold) {
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        ensureClassReportAccess(classSection);

        int safeLow = Math.max(0, Math.min(99, lowThreshold));
        int safeHigh = Math.max(safeLow + 1, Math.min(100, highThreshold));

        ClassReportOverviewResponse response = new ClassReportOverviewResponse();
        response.setClassSectionId(classSection.getId());
        response.setClassTitle(classSection.getTitle());
        response.setClassCode(classSection.getClassCode());
        response.setSubjectTitle(classSection.getSubject() == null ? null : classSection.getSubject().getTitle());
        response.setStatus(classSection.getStatus() == null ? null : classSection.getStatus().name());
        response.setStartDate(classSection.getStartDate() == null ? null : classSection.getStartDate().format(ISO_DATE));
        response.setEndDate(classSection.getEndDate() == null ? null : classSection.getEndDate().format(ISO_DATE));
        if (classSection.getTeacher() != null) {
            response.setPrimaryTeacherId(classSection.getTeacher().getId());
            response.setPrimaryTeacherName(classSection.getTeacher().getFullName());
        }

        long totalStudents = enrollmentRepository.countByClassSection_IdAndApprovalStatus(
                classSectionId, EnrollmentStatus.APPROVED);
        response.setTotalStudents(totalStudents);

        Double avgProgress = enrollmentRepository.averageProgressByClassSection(classSectionId);
        response.setAverageProgress(avgProgress == null ? 0.0 : Math.round(avgProgress * 10.0) / 10.0);

        response.setPendingRequests(enrollmentRepository.countByClassSection_IdAndApprovalStatus(
                classSectionId, EnrollmentStatus.PENDING));

        long taCount = 0;
        List<ClassReportOverviewResponse.TeachingMember> taList = new ArrayList<>();
        for (ClassMember member : classMemberRepository.findByClassSection_Id(classSectionId)) {
            if (member.getRole() != ClassMemberRole.TA) {
                continue;
            }
            taCount++;
            User user = member.getUser();
            ClassReportOverviewResponse.TeachingMember m = new ClassReportOverviewResponse.TeachingMember();
            if (user != null) {
                m.setUserId(user.getId());
                m.setFullName(user.getFullName());
                m.setEmail(user.getGmail());
                m.setAvatarUrl(user.getImageUrl());
            }
            m.setRole(ClassMemberRole.TA.name());
            taList.add(m);
        }
        response.setTaCount(taCount);
        response.setTeachingAssistants(taList);

        // tracked counts
        response.setTrackedQuizzes(classContentItemRepository.countTrackedQuizzesByClassSectionId(classSectionId));
        response.setTrackedAssignments(assignmentRepository.countByClassSectionId(classSectionId));

        // progress buckets (low / medium / high)
        long low = enrollmentRepository.countAtRiskStudents(classSectionId, safeLow);
        long high = enrollmentRepository.countEngagedStudents(classSectionId, safeHigh);
        long medium = enrollmentRepository.countStudentsInProgressRange(classSectionId, safeLow, safeHigh);

        response.setProgressBuckets(new ClassReportOverviewResponse.ProgressBuckets(
                low, medium, high, safeLow, safeHigh));
        response.setAtRiskStudents(low);
        response.setEngagedStudents(high);

        // quiz aggregate
        Object[] quizStats = quizAttemptRepository.aggregateClassSectionQuizStats(classSectionId);
        if (quizStats != null && quizStats.length >= 4) {
            // Hibernate may wrap a single result into Object[][1]. Defensive:
            Object first = quizStats[0];
            if (first instanceof Object[]) {
                quizStats = (Object[]) first;
            }
        }
        if (quizStats != null && quizStats.length >= 4) {
            double avgScore = ((Number) quizStats[1]).doubleValue();
            double topScore = ((Number) quizStats[2]).doubleValue();
            response.setAverageQuizScore(Math.round(avgScore * 10.0) / 10.0);
            response.setTopScore(Math.round(topScore * 10.0) / 10.0);
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.CLASS_ASSIGNMENT_REPORT, key = "#classSectionId")
    public AssignmentReportResponse getAssignmentReport(Integer classSectionId) {
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        ensureClassReportAccess(classSection);

        List<Assignment> assignments = assignmentRepository.findByClassSectionId(classSectionId);
        long totalStudents = enrollmentRepository.countByClassSection_IdAndApprovalStatus(
                classSectionId, EnrollmentStatus.APPROVED);

        // status counts grouped by assignment+status
        Map<Integer, Map<SubmissionStatus, Long>> statusMap = new HashMap<>();
        for (Object[] row : submissionRepository.countByClassSectionGroupedByAssignmentAndStatus(classSectionId)) {
            Integer assignmentId = (Integer) row[0];
            SubmissionStatus status = (SubmissionStatus) row[1];
            long count = ((Number) row[2]).longValue();
            statusMap.computeIfAbsent(assignmentId, k -> new HashMap<>()).put(status, count);
        }
        // distinct submitters per assignment (used for accurate notSubmitted)
        Map<Integer, Long> submittersMap = new HashMap<>();
        for (Object[] row : submissionRepository.countDistinctActualSubmittersByClassSectionGroupedByAssignment(classSectionId)) {
            submittersMap.put((Integer) row[0], ((Number) row[1]).longValue());
        }

        AssignmentReportResponse response = new AssignmentReportResponse();
        response.setTotalAssignments(assignments.size());
        response.setTotalStudents(totalStudents);

        List<AssignmentReportResponse.AssignmentStatusRow> rows = new ArrayList<>();
        long totalGraded = 0;
        long totalPending = 0;
        long totalNotSubmitted = 0;
        long totalLateSubmitted = 0;
        long totalReturned = 0;

        for (Assignment assignment : assignments) {
            Map<SubmissionStatus, Long> byStatus = statusMap.getOrDefault(assignment.getId(), new HashMap<>());
            long submitted = countNonNullStatuses(byStatus,
                    SubmissionStatus.SUBMITTED, SubmissionStatus.LATE_SUBMITTED,
                    SubmissionStatus.GRADED, SubmissionStatus.RETURNED);
            long pending = byStatus.getOrDefault(SubmissionStatus.SUBMITTED, 0L)
                    + byStatus.getOrDefault(SubmissionStatus.LATE_SUBMITTED, 0L);
            long graded = byStatus.getOrDefault(SubmissionStatus.GRADED, 0L);
            long late = byStatus.getOrDefault(SubmissionStatus.LATE_SUBMITTED, 0L);
            long returned = byStatus.getOrDefault(SubmissionStatus.RETURNED, 0L);

            long distinctSubmitters = submittersMap.getOrDefault(assignment.getId(), 0L);
            long notSubmitted = Math.max(0L, totalStudents - distinctSubmitters);

            AssignmentReportResponse.AssignmentStatusRow row = new AssignmentReportResponse.AssignmentStatusRow();
            row.setAssignmentId(assignment.getId());
            row.setAssignmentTitle(assignment.getTitle());
            row.setTotalStudents(totalStudents);
            row.setSubmittedCount(submitted);
            row.setGradedCount(graded);
            row.setPendingReviewCount(pending);
            row.setLateSubmittedCount(late);
            row.setReturnedCount(returned);
            row.setNotSubmitted(notSubmitted);
            row.setMaxScore(assignment.getMaxScore());
            row.setDueAt(assignment.getDueAt() == null ? null : assignment.getDueAt().format(ISO_DATE_TIME));
            row.setCloseAt(assignment.getCloseAt() == null ? null : assignment.getCloseAt().format(ISO_DATE_TIME));
            rows.add(row);

            totalGraded += graded;
            totalPending += pending;
            totalNotSubmitted += notSubmitted;
            totalLateSubmitted += late;
            totalReturned += returned;
        }

        response.setRows(rows);
        response.setTotalGraded(totalGraded);
        response.setTotalPending(totalPending);
        response.setTotalNotSubmitted(totalNotSubmitted);
        response.setTotalLateSubmitted(totalLateSubmitted);
        response.setTotalReturned(totalReturned);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public QuizReportResponse getQuizReport(Integer classSectionId) {
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        ensureClassReportAccess(classSection);

        QuizReportResponse response = new QuizReportResponse();
        long trackedQuizzes = classContentItemRepository.countTrackedQuizzesByClassSectionId(classSectionId);
        response.setTotalQuizzes(trackedQuizzes);

        List<QuizReportResponse.QuizSummaryRow> rows = new ArrayList<>();
        long totalAttempts = 0;
        long totalParticipants = 0;
        double sumGrade = 0;
        long sumGradeCount = 0;
        double topScore = 0;
        long totalPassed = 0;
        long totalNotPassed = 0;
        long totalWaitingReview = 0;

        for (Object[] row : quizAttemptRepository.aggregateQuizSummariesByClassSection(classSectionId)) {
            Integer quizId = (Integer) row[0];
            Integer classContentItemId = (Integer) row[1];
            String quizTitle = (String) row[2];
            long attempts = ((Number) row[3]).longValue();
            long uniqueStudents = ((Number) row[4]).longValue();
            double avgGrade = ((Number) row[5]).doubleValue();
            double maxGrade = ((Number) row[6]).doubleValue();
            long passed = row[7] == null ? 0 : ((Number) row[7]).longValue();
            long notPassed = row[8] == null ? 0 : ((Number) row[8]).longValue();
            long waitingReview = row[9] == null ? 0 : ((Number) row[9]).longValue();
            Integer minPassScore = (Integer) row[10];

            QuizReportResponse.QuizSummaryRow item = new QuizReportResponse.QuizSummaryRow();
            item.setQuizId(quizId);
            item.setClassContentItemId(classContentItemId);
            item.setQuizTitle(quizTitle);
            item.setTotalAttempts(attempts);
            item.setUniqueStudents(uniqueStudents);
            item.setPassedCount(passed);
            item.setNotPassedCount(notPassed);
            item.setWaitingReviewCount(waitingReview);
            item.setAverageScore(Math.round(avgGrade * 10.0) / 10.0);
            item.setTopScore(Math.round(maxGrade * 10.0) / 10.0);
            item.setMinPassScore(minPassScore);
            rows.add(item);

            totalAttempts += attempts;
            totalParticipants += uniqueStudents;
            sumGrade += avgGrade * attempts;
            sumGradeCount += attempts;
            topScore = Math.max(topScore, maxGrade);
            totalPassed += passed;
            totalNotPassed += notPassed;
            totalWaitingReview += waitingReview;
        }

        response.setRows(rows);
        response.setTotalAttempts(totalAttempts);
        response.setTotalParticipants(totalParticipants);
        response.setAverageScore(sumGradeCount == 0 ? 0.0 : Math.round((sumGrade / sumGradeCount) * 10.0) / 10.0);
        response.setTopScore(Math.round(topScore * 10.0) / 10.0);
        response.setTotalPassed(totalPassed);
        response.setTotalNotPassed(totalNotPassed);
        response.setTotalWaitingReview(totalWaitingReview);
        return response;
    }


    private long countNonNullStatuses(Map<SubmissionStatus, Long> map, SubmissionStatus... statuses) {
        long total = 0;
        for (SubmissionStatus status : statuses) {
            total += map.getOrDefault(status, 0L);
        }
        return total;
    }

    private User requireCurrentUser() {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return currentUser;
    }

    private void requireAdmin() {
        User currentUser = requireCurrentUser();
        if (currentUser.getRole() == null || currentUser.getRole().getRoleName() != RoleType.ADMIN) {
            throw new UnauthorizedException("Admin role required");
        }
    }

    private void ensureClassReportAccess(ClassSection classSection) {
        User currentUser = requireCurrentUser();
        if (currentUser.getRole() != null && currentUser.getRole().getRoleName() == RoleType.ADMIN) {
            return;
        }
        if (!classMemberAuthorizationService.canViewTeachingWorkspace(classSection, currentUser)) {
            throw new UnauthorizedException("You do not have permission to view this class report");
        }
    }
}
