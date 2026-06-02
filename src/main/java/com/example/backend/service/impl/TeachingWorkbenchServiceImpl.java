package com.example.backend.service.impl;

import com.example.backend.constant.AttemptStatus;
import com.example.backend.constant.ClassMemberRole;
import com.example.backend.constant.EnrollmentStatus;
import com.example.backend.constant.GradingStatus;
import com.example.backend.constant.RoleType;
import com.example.backend.constant.SubmissionStatus;
import com.example.backend.dto.response.classsection.ClassSectionResponse;
import com.example.backend.dto.response.teaching.ClassPeopleRowResponse;
import com.example.backend.dto.response.teaching.TeachingContextResponse;
import com.example.backend.dto.response.teaching.TeachingReviewQueueItemResponse;
import com.example.backend.dto.response.teaching.TeachingWorkbenchSummaryResponse;
import com.example.backend.entity.ClassSection;
import com.example.backend.entity.Enrollment;
import com.example.backend.entity.User;
import com.example.backend.entity.assignment.Assignment;
import com.example.backend.entity.assignment.Submission;
import com.example.backend.entity.quiz.QuizAttempt;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.repository.AssignmentRepository;
import com.example.backend.repository.ClassMemberRepository;
import com.example.backend.repository.ClassSectionRepository;
import com.example.backend.repository.EnrollmentRepository;
import com.example.backend.repository.QuizAttemptRepository;
import com.example.backend.repository.SubmissionRepository;
import com.example.backend.service.ClassMemberAuthorizationService;
import com.example.backend.service.ClassSectionService;
import com.example.backend.service.TeachingWorkbenchService;
import com.example.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TeachingWorkbenchServiceImpl implements TeachingWorkbenchService {
    private final ClassSectionRepository classSectionRepository;
    private final ClassMemberRepository classMemberRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final ClassSectionService classSectionService;
    private final ClassMemberAuthorizationService classMemberAuthorizationService;
    private final UserService userService;

    @Override
    @Transactional(readOnly = true)
    public TeachingContextResponse getTeachingContext() {
        Set<Integer> classIds = resolveTeachingClassSectionIds(requireCurrentUser());
        return new TeachingContextResponse(!classIds.isEmpty(), classIds.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassSectionResponse> getMyTeachingClasses() {
        return resolveTeachingClassSectionIds(requireCurrentUser()).stream()
                .map(classSectionService::getClassSectionById)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TeachingWorkbenchSummaryResponse getSummary(Integer classSectionId) {
        User currentUser = requireCurrentUser();
        Set<Integer> classIds = resolveTeachingClassSectionIds(currentUser);
        if (classSectionId != null) {
            requireManagedClass(classSectionId, classIds);
            classIds = new LinkedHashSet<>(List.of(classSectionId));
        }

        Set<Integer> peopleClassIds = filterClassIdsByCapability(
                currentUser,
                classIds,
                ClassMemberAuthorizationService.CAP_VIEW_PEOPLE
        );
        Set<Integer> progressClassIds = filterClassIdsByCapability(
                currentUser,
                classIds,
                ClassMemberAuthorizationService.CAP_VIEW_PROGRESS
        );
        Set<Integer> reviewClassIds = filterReviewClassIds(currentUser, classIds);

        long totalStudents = peopleClassIds.stream()
                .mapToLong(enrollmentRepository::countApprovedEnrollmentsByClassSectionId)
                .sum();
        List<TeachingReviewQueueItemResponse> queue = buildReviewQueue(currentUser, reviewClassIds);
        long pendingSubmissions = queue.stream().filter(item -> "ASSIGNMENT".equals(item.getType())).count();
        long pendingQuizReviews = queue.stream().filter(item -> "QUIZ".equals(item.getType())).count();
        long upcomingAssignments = countUpcomingAssignments(progressClassIds);
        long atRiskStudents = progressClassIds.stream()
                .flatMap(id -> buildPeopleRows(currentUser, id, null).stream())
                .filter(row -> row.getMissingAssignments() > 0 || row.getProgress() == null || row.getProgress() < 50)
                .count();

        return new TeachingWorkbenchSummaryResponse(
                classIds.size(),
                totalStudents,
                pendingSubmissions,
                pendingQuizReviews,
                atRiskStudents,
                upcomingAssignments
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeachingReviewQueueItemResponse> getReviewQueue() {
        User currentUser = requireCurrentUser();
        Set<Integer> reviewClassIds = filterReviewClassIds(
                currentUser,
                resolveTeachingClassSectionIds(currentUser)
        );
        return buildReviewQueue(currentUser, reviewClassIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeachingReviewQueueItemResponse> getReviewQueue(Integer classSectionId) {
        User currentUser = requireCurrentUser();
        Set<Integer> classIds = resolveTeachingClassSectionIds(currentUser);
        requireManagedClass(classSectionId, classIds);
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        if (!canReviewClassSection(classSection, currentUser)) {
            throw new UnauthorizedException("You do not have review permission in this class section");
        }
        return buildReviewQueue(currentUser, new LinkedHashSet<>(List.of(classSectionId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassPeopleRowResponse> getClassPeople(Integer classSectionId, String status) {
        User currentUser = requireCurrentUser();
        Set<Integer> classIds = resolveTeachingClassSectionIds(currentUser);
        requireManagedClass(classSectionId, classIds);
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        if (!classMemberAuthorizationService.canViewPeople(classSection, currentUser)) {
            throw new UnauthorizedException("You do not have permission to view people in this class section");
        }
        return buildPeopleRows(currentUser, classSectionId, status);
    }

    private List<TeachingReviewQueueItemResponse> buildReviewQueue(User currentUser, Set<Integer> classIds) {
        List<TeachingReviewQueueItemResponse> items = new ArrayList<>();
        for (Integer classId : classIds) {
            ClassSection classSection = classSectionRepository.findById(classId)
                    .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
            if (classMemberAuthorizationService.canManageAssignments(classSection, currentUser)) {
                addAssignmentReviewItems(items, currentUser, classSection);
            }
            if (classMemberAuthorizationService.canReviewQuizzes(classSection, currentUser)) {
                addQuizReviewItems(items, currentUser, classSection);
            }
        }
        items.sort(Comparator.comparing(
                TeachingReviewQueueItemResponse::getSubmittedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        return items;
    }

    private void addAssignmentReviewItems(
            List<TeachingReviewQueueItemResponse> items,
            User currentUser,
            ClassSection classSection
    ) {
        for (Assignment assignment : assignmentRepository.findByClassSection_IdOrderByDueAtAsc(classSection.getId())) {
            Map<Integer, Submission> latestByStudent = new LinkedHashMap<>();
            for (Submission submission : submissionRepository.findByAssignment_IdAndClassSection_IdOrderBySubmissionTimeDesc(
                    assignment.getId(),
                    classSection.getId()
            )) {
                if (submission.getStudent() == null || submission.getStudent().getId() == null) {
                    continue;
                }
                latestByStudent.putIfAbsent(submission.getStudent().getId(), submission);
            }

            for (Submission submission : latestByStudent.values()) {
                if (!isCompletedSubmission(submission) || isGradedSubmission(submission)) {
                    continue;
                }
                TeachingReviewQueueItemResponse item = new TeachingReviewQueueItemResponse();
                item.setType("ASSIGNMENT");
                item.setSubmissionId(submission.getId());
                item.setAssignmentId(assignment.getId());
                item.setClassSectionId(classSection.getId());
                item.setClassSectionTitle(classSection.getTitle());
                item.setTitle(assignment.getTitle());
                item.setStudentId(submission.getStudent().getId());
                item.setStudentName(submission.getStudent().getFullName());
                item.setSubmittedAt(submission.getSubmissionTime());
                item.setDueAt(assignment.getDueAt());
                item.setStatus(submission.getStatus() != null ? submission.getStatus().name() : null);
                item.setLate(submission.getStatus() == SubmissionStatus.LATE_SUBMITTED);
                item.setScore(submission.getGrade());
                item.setMaxScore(assignment.getMaxScore());
                item.setSelfOwned(submission.getStudent().getId().equals(currentUser.getId()));
                items.add(item);
            }
        }
    }

    private void addQuizReviewItems(
            List<TeachingReviewQueueItemResponse> items,
            User currentUser,
            ClassSection classSection
    ) {
        Integer teacherId = currentUser.getRole().getRoleName() == RoleType.ADMIN ? null : currentUser.getId();
        List<QuizAttempt> attempts = quizAttemptRepository.searchManagedAttempts(
                List.of(AttemptStatus.COMPLETED, AttemptStatus.EXPIRED),
                classSection.getId(),
                teacherId,
                List.of(ClassMemberRole.TEACHER, ClassMemberRole.TA),
                null,
                "PENDING",
                GradingStatus.NEEDS_REVIEW,
                Pageable.unpaged()
        ).getContent();

        for (QuizAttempt attempt : attempts) {
            if (attempt.getStudent() == null) {
                continue;
            }
            TeachingReviewQueueItemResponse item = new TeachingReviewQueueItemResponse();
            item.setType("QUIZ");
            item.setAttemptId(attempt.getId());
            item.setQuizId(attempt.getQuiz() != null ? attempt.getQuiz().getId() : null);
            item.setClassSectionId(classSection.getId());
            item.setClassSectionTitle(classSection.getTitle());
            item.setTitle(attempt.getQuiz() != null ? attempt.getQuiz().getTitle() : "Quiz");
            item.setStudentId(attempt.getStudent().getId());
            item.setStudentName(attempt.getStudent().getFullName());
            item.setSubmittedAt(attempt.getCompletedTime());
            item.setStatus(attempt.getGradingStatus() != null ? attempt.getGradingStatus().name() : null);
            item.setScore(attempt.getEarnedPoints());
            item.setMaxScore(attempt.getTotalPoints());
            item.setSelfOwned(attempt.getStudent().getId().equals(currentUser.getId()));
            items.add(item);
        }
    }

    private List<ClassPeopleRowResponse> buildPeopleRows(User currentUser, Integer classSectionId, String status) {
        EnrollmentStatus requestedStatus = parseEnrollmentStatus(status);
        List<Enrollment> enrollments = requestedStatus == null
                ? enrollmentRepository.findByClassSection_Id(classSectionId, Pageable.unpaged()).getContent()
                : enrollmentRepository.findByClassSection_IdAndApprovalStatus(classSectionId, requestedStatus);

        List<Assignment> assignments = assignmentRepository.findByClassSection_IdOrderByDueAtAsc(classSectionId);
        LocalDateTime now = LocalDateTime.now();
        List<ClassPeopleRowResponse> rows = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            User student = enrollment.getStudent();
            if (student == null) {
                continue;
            }
            List<Submission> studentSubmissions = submissionRepository.findByStudent_IdAndClassSection_IdOrderBySubmissionTimeDesc(
                    student.getId(),
                    classSectionId
            );
            Map<Integer, Submission> latestByAssignment = new LinkedHashMap<>();
            for (Submission submission : studentSubmissions) {
                if (submission.getAssignment() == null || submission.getAssignment().getId() == null) {
                    continue;
                }
                latestByAssignment.putIfAbsent(submission.getAssignment().getId(), submission);
            }

            long missing = assignments.stream()
                    .filter(assignment -> assignment.getDueAt() != null && now.isAfter(assignment.getDueAt()))
                    .filter(assignment -> !isCompletedSubmission(latestByAssignment.get(assignment.getId())))
                    .count();
            long pendingReviews = latestByAssignment.values().stream()
                    .filter(this::isCompletedSubmission)
                    .filter(submission -> !isGradedSubmission(submission))
                    .count();
            Number latestScore = studentSubmissions.stream()
                    .filter(submission -> submission.getGrade() != null)
                    .findFirst()
                    .map(Submission::getGrade)
                    .orElse(null);
            String lastActivity = studentSubmissions.stream()
                    .filter(submission -> submission.getSubmissionTime() != null)
                    .findFirst()
                    .map(submission -> submission.getSubmissionTime().toString())
                    .orElse(null);

            ClassPeopleRowResponse row = new ClassPeopleRowResponse();
            row.setEnrollmentId(enrollment.getId());
            row.setStudentId(student.getId());
            row.setStudentName(student.getFullName());
            row.setStudentNumber(student.getStudentNumber());
            row.setEmail(student.getGmail());
            row.setAvatarUrl(student.getImageUrl());
            row.setEnrollmentStatus(enrollment.getApprovalStatus());
            row.setProgress(enrollment.getProgress());
            row.setMissingAssignments(missing);
            row.setPendingReviews(pendingReviews);
            row.setLatestScore(latestScore);
            row.setLastActivity(lastActivity);
            row.setSelf(student.getId().equals(currentUser.getId()));
            rows.add(row);
        }
        rows.sort(Comparator.comparing(ClassPeopleRowResponse::getStudentName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        return rows;
    }

    private long countUpcomingAssignments(Set<Integer> classIds) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextWeek = now.plusDays(7);
        return classIds.stream()
                .flatMap(classId -> assignmentRepository.findByClassSection_IdOrderByDueAtAsc(classId).stream())
                .filter(assignment -> assignment.getDueAt() != null)
                .filter(assignment -> !assignment.getDueAt().isBefore(now) && !assignment.getDueAt().isAfter(nextWeek))
                .count();
    }

    private EnrollmentStatus parseEnrollmentStatus(String status) {
        if (!StringUtils.hasText(status) || "ALL".equalsIgnoreCase(status)) {
            return null;
        }
        return EnrollmentStatus.valueOf(status.trim().toUpperCase());
    }

    private boolean isCompletedSubmission(Submission submission) {
        return submission != null
                && submission.getSubmissionTime() != null
                && submission.getStatus() != null
                && submission.getStatus() != SubmissionStatus.NOT_SUBMITTED;
    }

    private boolean isGradedSubmission(Submission submission) {
        return submission != null
                && (submission.getStatus() == SubmissionStatus.GRADED || submission.getGrade() != null);
    }

    private void requireManagedClass(Integer classSectionId, Set<Integer> managedClassIds) {
        if (classSectionId == null || !managedClassIds.contains(classSectionId)) {
            throw new UnauthorizedException("You do not have teaching permission in this class section");
        }
    }

    private Set<Integer> filterClassIdsByCapability(User currentUser, Set<Integer> classIds, String capability) {
        Set<Integer> filtered = new LinkedHashSet<>();
        for (Integer classId : classIds) {
            ClassSection classSection = classSectionRepository.findById(classId)
                    .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
            if (classMemberAuthorizationService.hasCapability(classSection, currentUser, capability)) {
                filtered.add(classId);
            }
        }
        return filtered;
    }

    private Set<Integer> filterReviewClassIds(User currentUser, Set<Integer> classIds) {
        Set<Integer> filtered = new LinkedHashSet<>();
        for (Integer classId : classIds) {
            ClassSection classSection = classSectionRepository.findById(classId)
                    .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
            if (canReviewClassSection(classSection, currentUser)) {
                filtered.add(classId);
            }
        }
        return filtered;
    }

    private boolean canReviewClassSection(ClassSection classSection, User currentUser) {
        return classMemberAuthorizationService.canManageAssignments(classSection, currentUser)
                || classMemberAuthorizationService.canReviewQuizzes(classSection, currentUser);
    }

    private Set<Integer> resolveTeachingClassSectionIds(User currentUser) {
        if (currentUser.getRole().getRoleName() == RoleType.ADMIN) {
            Set<Integer> ids = new LinkedHashSet<>();
            for (ClassSection classSection : classSectionRepository.findAll()) {
                if (classSection.getId() != null) {
                    ids.add(classSection.getId());
                }
            }
            return ids;
        }

        Set<Integer> ids = new LinkedHashSet<>();
        for (ClassSection classSection : classSectionRepository.findByTeacher_Id(currentUser.getId())) {
            ids.add(classSection.getId());
        }
        for (ClassSection classSection : classMemberRepository.findClassSectionsByMemberRole(currentUser.getId(), ClassMemberRole.TEACHER)) {
            ids.add(classSection.getId());
        }
        for (ClassSection classSection : classMemberRepository.findClassSectionsByMemberRole(currentUser.getId(), ClassMemberRole.TA)) {
            ids.add(classSection.getId());
        }
        return ids;
    }

    private User requireCurrentUser() {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        return currentUser;
    }
}
