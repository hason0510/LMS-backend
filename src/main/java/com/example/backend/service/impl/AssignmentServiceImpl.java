package com.example.backend.service.impl;

import com.example.backend.constant.EnrollmentStatus;
import com.example.backend.constant.ClassMemberRole;
import com.example.backend.constant.ClassContentAvailabilityStatus;
import com.example.backend.constant.ContentItemType;
import com.example.backend.constant.RoleType;
import com.example.backend.constant.SubmissionStatus;
import com.example.backend.dto.request.AssignmentRequest;
import com.example.backend.dto.request.ResourceRequest;
import com.example.backend.dto.response.AssignmentResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.assignment.StudentAssignmentFeedResponse;
import com.example.backend.dto.response.assignment.TeacherAssignmentOverviewResponse;
import com.example.backend.entity.assignment.Assignment;
import com.example.backend.entity.ClassContentItem;
import com.example.backend.entity.ClassSection;
import com.example.backend.entity.Enrollment;
import com.example.backend.entity.Resource;
import com.example.backend.entity.User;
import com.example.backend.entity.assignment.Submission;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.repository.AssignmentRepository;
import com.example.backend.repository.ClassMemberRepository;
import com.example.backend.repository.ClassContentItemRepository;
import com.example.backend.repository.ClassSectionRepository;
import com.example.backend.repository.EnrollmentRepository;
import com.example.backend.repository.SubmissionRepository;
import com.example.backend.service.AssignmentService;
import com.example.backend.service.ClassContentAccessResult;
import com.example.backend.service.ClassContentAccessService;
import com.example.backend.service.ClassMemberAuthorizationService;
import com.example.backend.service.ClassNotificationService;
import com.example.backend.service.ResourceService;
import com.example.backend.service.UserService;
import com.example.backend.specification.AssignmentSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
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
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final ClassSectionRepository classSectionRepository;
    private final ClassMemberRepository classMemberRepository;
    private final ClassContentItemRepository classContentItemRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SubmissionRepository submissionRepository;
    private final UserService userService;
    private final ClassMemberAuthorizationService classMemberAuthorizationService;
    private final ClassContentAccessService classContentAccessService;
    private final ResourceService resourceService;
    private final ClassNotificationService classNotificationService;

    @Override
    @Transactional
    public AssignmentResponse createAssignment(AssignmentRequest request) {
        ClassSection classSection = classSectionRepository.findById(request.getClassSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        requireTeacherOrTaPermission(classSection);

        Assignment assignment = new Assignment();
        applyRequest(assignment, request, classSection, true);
        Assignment saved = assignmentRepository.save(assignment);
        classNotificationService.notifyApprovedStudents(
                classSection,
                "Bài tập mới: " + saved.getTitle(),
                "Lớp " + classSection.getTitle() + " vừa có bài tập mới.",
                "ASSIGNMENT_CREATED",
                saved.getDescription(),
                "/class-sections/" + classSection.getId() + "/assignments/" + saved.getId(),
                "ASSIGNMENT",
                saved.getId(),
                "assignment-created"
        );
        return convertToResponse(saved);
    }

    @Override
    @Transactional
    public AssignmentResponse updateAssignment(Integer id, AssignmentRequest request) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        String previousTitle = assignment.getTitle();

        ClassSection classSection = classSectionRepository.findById(request.getClassSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));

        if (assignment.getClassSection() != null
                && !assignment.getClassSection().getId().equals(classSection.getId())) {
            requireTeacherPermission(assignment.getClassSection());
        }
        requireTeacherPermission(classSection);

        applyRequest(assignment, request, classSection, false);
        Assignment savedAssignment = assignmentRepository.save(assignment);
        syncLinkedClassContentItemTitle(savedAssignment, previousTitle);
        return convertToResponse(savedAssignment);
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentResponse getAssignmentById(Integer id) {
        return getAssignmentById(id, null);
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentResponse getAssignmentById(Integer id, Integer classContentItemId) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        User currentUser = requireCurrentUser();

        if (classContentItemId != null) {
            validateAssignmentAccessByClassContentItem(assignment, classContentItemId, currentUser);
            return convertToResponse(assignment);
        }

        if (assignment.getClassSection() != null) {
            requireViewPermission(assignment.getClassSection());
        } else {
            List<ClassSection> classSections = classContentItemRepository.findClassSectionsByAssignmentId(assignment.getId());
            if (classSections.isEmpty()) {
                if (currentUser.getRole().getRoleName() == RoleType.STUDENT) {
                    throw new UnauthorizedException("You do not have permission to access this assignment");
                }
            } else {
                boolean canViewAnySection = classSections.stream()
                        .anyMatch(classSection -> canViewClassSection(classSection, currentUser));
                if (!canViewAnySection) {
                    throw new UnauthorizedException("You do not have permission to access this assignment");
                }
            }
        }
        return convertToResponse(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AssignmentResponse> getAssignmentsByClassSection(Integer classSectionId) {
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        requireViewPermission(classSection);

        Map<Integer, Assignment> assignmentMap = new LinkedHashMap<>();
        for (Assignment assignment : assignmentRepository.findByClassSection_IdOrderByDueAtAsc(classSectionId)) {
            assignmentMap.put(assignment.getId(), assignment);
        }

        for (ClassContentItem item : classContentItemRepository.findAssignmentItemsByClassSectionId(classSectionId)) {
            Assignment assignment = resolveAssignment(item);
            if (assignment != null) {
                assignmentMap.putIfAbsent(assignment.getId(), assignment);
            }
        }

        List<Assignment> assignments = new ArrayList<>(assignmentMap.values());
        assignments.sort(Comparator.comparing(Assignment::getDueAt, Comparator.nullsLast(Comparator.naturalOrder())));
        List<AssignmentResponse> responses = assignments.stream().map(this::convertToResponse).toList();
        return new PageResponse<>(1, responses.isEmpty() ? 0 : 1, responses.size(), responses);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StudentAssignmentFeedResponse> getStudentAssignmentFeed(String tab, String keyword, Integer classSectionId) {
        User currentUser = requireCurrentUser();
        if (currentUser.getRole().getRoleName() != RoleType.STUDENT) {
            throw new UnauthorizedException("Only students can access assignment feed");
        }

        List<Enrollment> approvedEnrollments = enrollmentRepository.findByStudent_IdAndApprovalStatus(
                currentUser.getId(),
                EnrollmentStatus.APPROVED
        );
        if (approvedEnrollments.isEmpty()) {
            return new PageResponse<>(1, 0, 0, List.of());
        }

        Set<Integer> classSectionIds = approvedEnrollments.stream()
                .map(Enrollment::getClassSection)
                .filter(Objects::nonNull)
                .map(ClassSection::getId)
                .filter(Objects::nonNull)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
        if (classSectionIds.isEmpty()) {
            return new PageResponse<>(1, 0, 0, List.of());
        }
        if (classSectionId != null && !classSectionIds.contains(classSectionId)) {
            throw new UnauthorizedException("You do not have permission to access this class section");
        }

        Specification<Assignment> spec = Specification.where(AssignmentSpecification.inClassSections(classSectionIds))
                .and(AssignmentSpecification.titleContains(keyword));
        if (classSectionId != null) {
            spec = spec.and(AssignmentSpecification.hasClassSectionId(classSectionId));
        }

        List<Assignment> assignments = assignmentRepository.findAll(spec);
        if (assignments.isEmpty()) {
            return new PageResponse<>(1, 0, 0, List.of());
        }

        Map<Integer, Submission> latestSubmissions = new LinkedHashMap<>();
        List<Integer> assignmentIds = assignments.stream().map(Assignment::getId).toList();
        for (Submission submission : submissionRepository.findByStudent_IdAndAssignment_IdIn(currentUser.getId(), assignmentIds)) {
            if (submission.getAssignment() == null || submission.getAssignment().getId() == null) {
                continue;
            }
            Integer assignmentId = submission.getAssignment().getId();
            Submission currentLatest = latestSubmissions.get(assignmentId);
            if (currentLatest == null || isNewerSubmission(submission, currentLatest)) {
                latestSubmissions.put(assignmentId, submission);
            }
        }

        String normalizedTab = normalizeTab(tab, "UPCOMING");
        LocalDateTime now = LocalDateTime.now();
        List<StudentAssignmentFeedResponse> items = new ArrayList<>();
        for (Assignment assignment : assignments) {
            ClassSection assignmentSection = resolvePrimaryClassSection(assignment);
            if (assignmentSection == null) {
                continue;
            }
            Submission submission = latestSubmissions.get(assignment.getId());
            boolean completed = isCompletedSubmission(submission);
            boolean pastDue = !completed && assignment.getDueAt() != null && now.isAfter(assignment.getDueAt());
            boolean upcoming = !completed && !pastDue;

            StudentAssignmentFeedResponse response = new StudentAssignmentFeedResponse();
            response.setAssignmentId(assignment.getId());
            response.setAssignmentTitle(assignment.getTitle());
            response.setClassSectionId(assignmentSection.getId());
            response.setClassSectionTitle(assignmentSection.getTitle());
            response.setDueAt(assignment.getDueAt());
            response.setCloseAt(assignment.getCloseAt());
            response.setMaxScore(assignment.getMaxScore());
            response.setAllowLateSubmission(assignment.isAllowLateSubmission());
            response.setSubmissionStatus(submission != null ? submission.getStatus() : SubmissionStatus.NOT_SUBMITTED);
            response.setSubmissionTime(submission != null ? submission.getSubmissionTime() : null);
            response.setGrade(submission != null ? submission.getGrade() : null);
            response.setCompleted(completed);
            response.setPastDue(pastDue);
            response.setUpcoming(upcoming);
            if (matchesStudentTab(normalizedTab, response)) {
                items.add(response);
            }
        }

        items.sort(resolveStudentSort(normalizedTab));
        return new PageResponse<>(1, items.isEmpty() ? 0 : 1, items.size(), items);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TeacherAssignmentOverviewResponse> getTeachingAssignments(String tab, String keyword, Integer classSectionId) {
        User currentUser = requireCurrentUser();
        if (currentUser.getRole().getRoleName() != RoleType.TEACHER && currentUser.getRole().getRoleName() != RoleType.ADMIN) {
            throw new UnauthorizedException("Only teacher/admin can access teaching assignments");
        }

        Set<Integer> classSectionIds = resolveTeachingClassSectionIds(currentUser);
        if (classSectionIds.isEmpty()) {
            return new PageResponse<>(1, 0, 0, List.of());
        }
        if (classSectionId != null && !classSectionIds.contains(classSectionId)) {
            throw new UnauthorizedException("You do not manage this class section");
        }

        Specification<Assignment> spec = Specification.where(AssignmentSpecification.inClassSections(classSectionIds))
                .and(AssignmentSpecification.titleContains(keyword));
        if (classSectionId != null) {
            spec = spec.and(AssignmentSpecification.hasClassSectionId(classSectionId));
        }

        List<Assignment> assignments = assignmentRepository.findAll(spec);
        if (assignments.isEmpty()) {
            return new PageResponse<>(1, 0, 0, List.of());
        }

        LocalDateTime now = LocalDateTime.now();
        String normalizedTab = normalizeTab(tab, "ALL");
        List<TeacherAssignmentOverviewResponse> items = new ArrayList<>();
        for (Assignment assignment : assignments) {
            ClassSection assignmentSection = resolvePrimaryClassSection(assignment);
            if (assignmentSection == null) {
                continue;
            }

            long totalStudents = enrollmentRepository.countApprovedEnrollmentsByClassSectionId(assignmentSection.getId());
            Map<Integer, Submission> latestByStudentId = new LinkedHashMap<>();
            for (Submission submission : submissionRepository.findByAssignment_IdAndClassSection_IdOrderBySubmissionTimeDesc(
                    assignment.getId(),
                    assignmentSection.getId()
            )) {
                if (submission.getStudent() == null || submission.getStudent().getId() == null) {
                    continue;
                }
                latestByStudentId.putIfAbsent(submission.getStudent().getId(), submission);
            }

            long turnedInCount = latestByStudentId.values().stream().filter(this::isCompletedSubmission).count();
            long gradedCount = latestByStudentId.values().stream().filter(this::isGradedSubmission).count();
            long pendingReviewCount = Math.max(0, turnedInCount - gradedCount);
            boolean completed = totalStudents > 0 && turnedInCount >= totalStudents;
            boolean pastDue = assignment.getDueAt() != null && now.isAfter(assignment.getDueAt());
            boolean upcoming = !pastDue;

            TeacherAssignmentOverviewResponse response = new TeacherAssignmentOverviewResponse();
            response.setAssignmentId(assignment.getId());
            response.setAssignmentTitle(assignment.getTitle());
            response.setClassSectionId(assignmentSection.getId());
            response.setClassSectionTitle(assignmentSection.getTitle());
            response.setDueAt(assignment.getDueAt());
            response.setCloseAt(assignment.getCloseAt());
            response.setMaxScore(assignment.getMaxScore());
            response.setTotalStudents(totalStudents);
            response.setTurnedInCount(turnedInCount);
            response.setGradedCount(gradedCount);
            response.setPendingReviewCount(pendingReviewCount);
            response.setPastDue(pastDue);
            response.setUpcoming(upcoming);
            response.setCompleted(completed);

            if (matchesTeachingTab(normalizedTab, response)) {
                items.add(response);
            }
        }

        items.sort(Comparator.comparing(TeacherAssignmentOverviewResponse::getDueAt, Comparator.nullsLast(Comparator.naturalOrder())));
        return new PageResponse<>(1, items.isEmpty() ? 0 : 1, items.size(), items);
    }

    @Override
    @Transactional
    public void deleteAssignment(Integer id) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));

        ClassSection classSection = resolvePrimaryClassSection(assignment);
        if (classSection == null) {
            throw new BusinessException("Assignment is not linked to any class section");
        }
        requireTeacherPermission(classSection);
        assignmentRepository.delete(assignment);
    }

    private void applyRequest(Assignment assignment, AssignmentRequest request, ClassSection classSection, boolean createMode) {
        if (request.getDueAt() != null && request.getCloseAt() != null && request.getCloseAt().isBefore(request.getDueAt())) {
            throw new BusinessException("closeAt must be after dueAt");
        }
        assignment.setTitle(request.getTitle().trim());
        assignment.setDescription(request.getDescription());
        assignment.setInstruction(request.getInstruction());
        assignment.setMaxScore(request.getMaxScore());
        assignment.setDueAt(request.getDueAt());
        assignment.setCloseAt(request.getCloseAt());
        assignment.setAllowLateSubmission(Boolean.TRUE.equals(request.getAllowLateSubmission()));
        assignment.setClassSection(classSection);

        if (createMode || request.getResources() != null) {
            replaceAssignmentResources(assignment, request.getResources());
        }
    }

    private void replaceAssignmentResources(Assignment assignment, List<ResourceRequest> resourceRequests) {
        if (assignment.getResources() == null) {
            assignment.setResources(new ArrayList<>());
        }
        assignment.getResources().clear();

        if (resourceRequests == null || resourceRequests.isEmpty()) {
            return;
        }

        for (ResourceRequest resourceRequest : resourceRequests) {
            Resource resource = resourceService.buildDetachedResource(resourceRequest);
            resource.setAssignment(assignment);
            resource.setLesson(null);
            resource.setSubmission(null);
            assignment.getResources().add(resource);
        }
    }

    private Assignment resolveAssignment(ClassContentItem item) {
        if (item.getAssignment() != null) {
            return item.getAssignment();
        }
       /* if (item.getContentItemTemplate() != null) {
            return null;
        }*/
        return null;
    }

    private void syncLinkedClassContentItemTitle(Assignment assignment, String previousAssignmentTitle) {
        classContentItemRepository.findByAssignment_Id(assignment.getId()).ifPresent(classContentItem -> {
            String itemTitle = classContentItem.getTitle();
            if (!StringUtils.hasText(itemTitle) || Objects.equals(itemTitle, previousAssignmentTitle)) {
                classContentItem.setTitle(assignment.getTitle());
                classContentItemRepository.save(classContentItem);
            }
        });
    }

    private boolean isNewerSubmission(Submission candidate, Submission current) {
        LocalDateTime candidateTime = candidate.getSubmissionTime();
        LocalDateTime currentTime = current.getSubmissionTime();
        if (candidateTime != null && currentTime != null) {
            return candidateTime.isAfter(currentTime);
        }
        if (candidateTime != null) {
            return true;
        }
        return candidate.getId() != null && current.getId() != null && candidate.getId() > current.getId();
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

    private String normalizeTab(String tab, String fallback) {
        if (!StringUtils.hasText(tab)) {
            return fallback;
        }
        return tab.trim().toUpperCase();
    }

    private boolean matchesStudentTab(String tab, StudentAssignmentFeedResponse item) {
        return switch (tab) {
            case "PAST_DUE" -> item.isPastDue();
            case "COMPLETED" -> item.isCompleted();
            default -> item.isUpcoming();
        };
    }

    private Comparator<StudentAssignmentFeedResponse> resolveStudentSort(String tab) {
        if ("COMPLETED".equals(tab)) {
            return Comparator.comparing(
                    StudentAssignmentFeedResponse::getSubmissionTime,
                    Comparator.nullsLast(Comparator.reverseOrder())
            );
        }
        if ("PAST_DUE".equals(tab)) {
            return Comparator.comparing(
                    StudentAssignmentFeedResponse::getDueAt,
                    Comparator.nullsLast(Comparator.reverseOrder())
            );
        }
        return Comparator.comparing(
                StudentAssignmentFeedResponse::getDueAt,
                Comparator.nullsLast(Comparator.naturalOrder())
        );
    }

    private boolean matchesTeachingTab(String tab, TeacherAssignmentOverviewResponse item) {
        return switch (tab) {
            case "UPCOMING" -> item.isUpcoming();
            case "PAST_DUE" -> item.isPastDue();
            case "COMPLETED" -> item.isCompleted();
            default -> true;
        };
    }

    private Set<Integer> resolveTeachingClassSectionIds(User currentUser) {
        if (currentUser.getRole().getRoleName() == RoleType.ADMIN) {
            Set<Integer> adminIds = new LinkedHashSet<>();
            for (ClassSection classSection : classSectionRepository.findAll()) {
                if (classSection.getId() != null) {
                    adminIds.add(classSection.getId());
                }
            }
            return adminIds;
        }

        Set<Integer> classSectionIds = new LinkedHashSet<>();
        for (ClassSection classSection : classSectionRepository.findByTeacher_Id(currentUser.getId())) {
            if (classSection.getId() != null) {
                classSectionIds.add(classSection.getId());
            }
        }
        for (ClassSection classSection : classMemberRepository.findClassSectionsByMemberRole(
                currentUser.getId(),
                ClassMemberRole.TEACHER
        )) {
            if (classSection.getId() != null) {
                classSectionIds.add(classSection.getId());
            }
        }
        for (ClassSection classSection : classMemberRepository.findClassSectionsByMemberRole(
                currentUser.getId(),
                ClassMemberRole.TA
        )) {
            if (classSection.getId() != null) {
                classSectionIds.add(classSection.getId());
            }
        }
        return classSectionIds;
    }

    private ClassSection resolvePrimaryClassSection(Assignment assignment) {
        if (assignment.getClassSection() != null) {
            return assignment.getClassSection();
        }
        return classContentItemRepository.findClassSectionsByAssignmentId(assignment.getId()).stream()
                .findFirst()
                .orElse(null);
    }

    private AssignmentResponse convertToResponse(Assignment assignment) {
        AssignmentResponse response = new AssignmentResponse();
        response.setId(assignment.getId());
        response.setTitle(StringUtils.hasText(assignment.getTitle()) ? assignment.getTitle().trim() : assignment.getTitle());
        response.setDescription(assignment.getDescription());
        response.setInstruction(assignment.getInstruction());
        response.setMaxScore(assignment.getMaxScore());
        response.setDueAt(assignment.getDueAt());
        response.setCloseAt(assignment.getCloseAt());
        response.setAllowLateSubmission(assignment.isAllowLateSubmission());
        response.setClassSectionId(assignment.getClassSection() != null ? assignment.getClassSection().getId() : null);
        response.setResources(
                assignment.getResources() == null
                        ? List.of()
                        : assignment.getResources().stream().map(resourceService::convertEntityToDTO).toList()
        );
        return response;
    }

    private void requireTeacherPermission(ClassSection classSection) {
        User currentUser = requireCurrentUser();
        if (classMemberAuthorizationService.isTeacher(classSection, currentUser)) {
            return;
        }
        throw new UnauthorizedException("You do not manage this class section");
    }

    private void requireTeacherOrTaPermission(ClassSection classSection) {
        User currentUser = requireCurrentUser();
        if (classMemberAuthorizationService.isTeacherOrTa(classSection, currentUser)) {
            return;
        }
        throw new UnauthorizedException("You do not have teaching permission in this class section");
    }

    private void requireViewPermission(ClassSection classSection) {
        User currentUser = requireCurrentUser();
        if (canViewClassSection(classSection, currentUser)) {
            return;
        }
        throw new UnauthorizedException("You do not have permission to access this class section");
    }

    private boolean canViewClassSection(ClassSection classSection, User currentUser) {
        if (classMemberAuthorizationService.isTeacherOrTa(classSection, currentUser)) {
            return true;
        }
        if (currentUser.getRole().getRoleName() != RoleType.STUDENT) {
            return false;
        }
        return enrollmentRepository.existsByStudent_IdAndClassSection_IdAndApprovalStatus(
                currentUser.getId(),
                classSection.getId(),
                EnrollmentStatus.APPROVED
        );
    }

    private User requireCurrentUser() {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        return currentUser;
    }

    private void validateAssignmentAccessByClassContentItem(
            Assignment assignment,
            Integer classContentItemId,
            User currentUser
    ) {
        ClassContentItem classContentItem = classContentItemRepository.findById(classContentItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Class content item not found"));
        if (classContentItem.getItemType() != ContentItemType.ASSIGNMENT
                || classContentItem.getAssignment() == null
                || !classContentItem.getAssignment().getId().equals(assignment.getId())) {
            throw new UnauthorizedException("Noi dung bai tap khong hop le");
        }

        ClassContentAccessResult accessResult = classContentAccessService.evaluateForUser(classContentItem, currentUser);
        if (accessResult.accessible()) {
            return;
        }
        if (accessResult.availabilityStatus() == ClassContentAvailabilityStatus.NO_ENROLLMENT) {
            throw new UnauthorizedException(accessResult.message());
        }
        throw new BusinessException(accessResult.message() != null
                ? accessResult.message()
                : "Ban khong co quyen truy cap noi dung nay");
    }
}
