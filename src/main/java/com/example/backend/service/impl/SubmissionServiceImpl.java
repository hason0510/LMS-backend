package com.example.backend.service.impl;

import com.example.backend.utils.ClassSectionGuard;

import com.example.backend.cache.RedisCacheInvalidationService;
import com.example.backend.constant.EnrollmentStatus;
import com.example.backend.constant.ResourceType;
import com.example.backend.constant.ResourceSource;
import com.example.backend.constant.RoleType;
import com.example.backend.constant.SubmissionStatus;
import com.example.backend.dto.request.ResourceRequest;
import com.example.backend.dto.request.SubmissionGradeRequest;
import com.example.backend.dto.request.SubmissionRequest;
import com.example.backend.dto.request.SubmissionReturnRequest;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.SubmissionResponse;
import com.example.backend.entity.assignment.Assignment;
import com.example.backend.entity.ClassSection;
import com.example.backend.entity.Enrollment;
import com.example.backend.entity.resource.Resource;
import com.example.backend.entity.assignment.Submission;
import com.example.backend.entity.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.repository.AssignmentRepository;
import com.example.backend.repository.ClassContentItemRepository;
import com.example.backend.repository.ClassSectionRepository;
import com.example.backend.repository.EnrollmentRepository;
import com.example.backend.repository.SubmissionRepository;
import com.example.backend.service.ClassMemberAuthorizationService;
import com.example.backend.service.ResourceService;
import com.example.backend.service.SubmissionService;
import com.example.backend.service.UserService;
import com.example.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.example.backend.specification.SubmissionSpecification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final ClassSectionRepository classSectionRepository;
    private final ClassContentItemRepository classContentItemRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserService userService;
    private final ClassMemberAuthorizationService classMemberAuthorizationService;
    private final ResourceService resourceService;
    private final RedisCacheInvalidationService cacheInvalidationService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public SubmissionResponse submitAssignment(Integer assignmentId, Integer classSectionId, SubmissionRequest request) {
        User currentUser = requireCurrentUser();
        if (currentUser.getRole().getRoleName() != RoleType.STUDENT) {
            throw new UnauthorizedException("Only students can submit assignments");
        }

        validateSubmissionPayload(request);

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        ClassSection classSection = resolveClassSection(assignment, classSectionId);
        ensureStudentEnrollment(currentUser, classSection.getId());

        LocalDateTime now = LocalDateTime.now();
        if (isSubmissionClosed(assignment, now)) {
            throw new BusinessException("Đã quá thời gian nộp bài!");
        }
        boolean lateSubmission = isLateSubmission(assignment, now);

        Submission submission = submissionRepository.findByAssignment_IdAndClassSection_IdAndStudent_Id(
                        assignment.getId(),
                        classSection.getId(),
                        currentUser.getId()
                )
                .orElseGet(() -> {
                    Submission created = new Submission();
                    created.setAssignment(assignment);
                    created.setClassSection(classSection);
                    created.setStudent(currentUser);
                    created.setStatus(SubmissionStatus.NOT_SUBMITTED);
                    created.setSubmissionCount(0);
                    return created;
                });

        if (submission.getStatus() == SubmissionStatus.GRADED) {
            throw new BusinessException("Bài đã được chấm điểm, không thể nộp lại. Liên hệ giảng viên nếu cần nộp lại.");
        }

        List<ResourceRequest> requestedResources = resolveRequestedResources(request);
        submission.setDescription(request.getDescription());
        applySubmissionResources(submission, requestedResources);
        syncLegacySubmissionFields(submission);
        submission.setSubmissionTime(now);
        submission.setStatus(lateSubmission ? SubmissionStatus.LATE_SUBMITTED : SubmissionStatus.SUBMITTED);
        submission.setSubmissionCount((submission.getSubmissionCount() == null ? 0 : submission.getSubmissionCount()) + 1);

        // Any resubmission resets grading state until teacher grades again.
        submission.setGrade(null);
        submission.setFeedback(null);
        submission.setGradedAt(null);

        Submission saved = submissionRepository.save(submission);
        cacheInvalidationService.evictTeachingAndReportCaches();
        return convertToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionResponse getMySubmission(Integer assignmentId, Integer classSectionId) {
        User currentUser = requireCurrentUser();
        if (currentUser.getRole().getRoleName() != RoleType.STUDENT) {
            throw new UnauthorizedException("Only students can view their submission");
        }

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        ClassSection classSection = resolveClassSection(assignment, classSectionId);
        ensureStudentEnrollment(currentUser, classSection.getId());

        return submissionRepository.findByAssignment_IdAndClassSection_IdAndStudent_Id(
                        assignmentId,
                        classSection.getId(),
                        currentUser.getId()
                )
                .map(this::convertToResponse)
                .orElseGet(() -> buildNotSubmittedResponse(assignment, classSection, currentUser));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionResponse> getMySubmissions(Integer classSectionId, SubmissionStatus status) {
        User currentUser = requireCurrentUser();
        if (currentUser.getRole().getRoleName() != RoleType.STUDENT) {
            throw new UnauthorizedException("Only students can view their submissions");
        }

        List<Submission> submissions;
        if (classSectionId != null) {
            ensureStudentEnrollment(currentUser, classSectionId);
            submissions = status != null
                    ? submissionRepository.findByStudent_IdAndClassSection_IdAndStatusOrderBySubmissionTimeDesc(
                    currentUser.getId(),
                    classSectionId,
                    status
            )
                    : submissionRepository.findByStudent_IdAndClassSection_IdOrderBySubmissionTimeDesc(
                    currentUser.getId(),
                    classSectionId
            );
        } else {
            submissions = submissionRepository.findByStudent_IdOrderBySubmissionTimeDesc(currentUser.getId());
            if (status != null) {
                submissions = submissions.stream()
                        .filter(submission -> submission.getStatus() == status)
                        .toList();
            }
        }

        return submissions.stream().map(this::convertToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SubmissionResponse> getAssignmentSubmissions(
            Integer assignmentId,
            Integer classSectionId,
            boolean includeNotSubmitted,
            String keyword,
            SubmissionStatus status,
            Integer pageNumber,
            Integer pageSize
    ) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        ClassSection classSection = resolveClassSection(assignment, classSectionId);
        requireTeachingPermission(classSection, ClassMemberAuthorizationService.CAP_MANAGE_ASSIGNMENTS);

        boolean onlyNotSubmitted = status == SubmissionStatus.NOT_SUBMITTED;
        Specification<Submission> baseSpec = Specification
                .allOf(SubmissionSpecification.hasAssignmentId(assignmentId))
                .and(SubmissionSpecification.hasClassSectionId(classSection.getId()))
                .and(SubmissionSpecification.studentContains(keyword));
        Specification<Submission> filteredSpec = baseSpec;
        if (status != null && !onlyNotSubmitted) {
            filteredSpec = filteredSpec.and(SubmissionSpecification.hasStatus(status));
        }

        List<Submission> existingSubmissions = onlyNotSubmitted
                ? List.of()
                : submissionRepository.findAll(filteredSpec, Sort.by(Sort.Direction.DESC, "submissionTime"));

        List<SubmissionResponse> response = new ArrayList<>();
        Map<Integer, Submission> submissionByStudentId = new LinkedHashMap<>();
        List<Submission> existingSubmissionsForExclusion = onlyNotSubmitted && includeNotSubmitted
                ? submissionRepository.findAll(baseSpec)
                : existingSubmissions;
        for (Submission submission : existingSubmissionsForExclusion) {
            if (submission.getStudent() == null) {
                continue;
            }
            submissionByStudentId.putIfAbsent(submission.getStudent().getId(), submission);
        }
        if (!onlyNotSubmitted) {
            for (Submission submission : submissionByStudentId.values()) {
                response.add(convertToResponse(submission));
            }
        }

        if (includeNotSubmitted) {
            List<Enrollment> enrollments = enrollmentRepository.findByClassSection_IdAndApprovalStatus(
                    classSection.getId(),
                    EnrollmentStatus.APPROVED
            );
            for (Enrollment enrollment : enrollments) {
                User student = enrollment.getStudent();
                if (student == null || submissionByStudentId.containsKey(student.getId())) {
                    continue;
                }
                SubmissionResponse notSubmitted = buildNotSubmittedResponse(assignment, classSection, student);
                if ((status == null || status == SubmissionStatus.NOT_SUBMITTED)
                        && matchesSubmissionKeyword(notSubmitted, keyword)) {
                    response.add(notSubmitted);
                }
            }
        }

        response.sort(Comparator.comparing(
                SubmissionResponse::getStudentName,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
        ));
        return paginateSubmissions(response, pageNumber, pageSize);
    }

    private boolean matchesSubmissionKeyword(SubmissionResponse submission, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalized = keyword.trim().toLowerCase();
        return containsIgnoreCase(submission.getStudentName(), normalized)
                || containsIgnoreCase(submission.getStudentNumber(), normalized)
                || containsIgnoreCase(String.valueOf(submission.getStudentId()), normalized);
    }

    private boolean containsIgnoreCase(String value, String normalizedKeyword) {
        return value != null && value.toLowerCase().contains(normalizedKeyword);
    }

    private PageResponse<SubmissionResponse> paginateSubmissions(
            List<SubmissionResponse> submissions,
            Integer pageNumber,
            Integer pageSize
    ) {
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int safePageNumber = pageNumber == null || pageNumber < 1 ? 1 : pageNumber;
        int totalElements = submissions.size();
        int totalPage = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / safePageSize);
        int fromIndex = Math.min((safePageNumber - 1) * safePageSize, totalElements);
        int toIndex = Math.min(fromIndex + safePageSize, totalElements);
        return new PageResponse<>(
                safePageNumber,
                totalPage,
                totalElements,
                submissions.subList(fromIndex, toIndex)
        );
    }

    @Override
    @Transactional
    public SubmissionResponse gradeSubmission(Integer submissionId, SubmissionGradeRequest request) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));
        User currentUser = requireTeachingPermission(
                submission.getClassSection(),
                ClassMemberAuthorizationService.CAP_MANAGE_ASSIGNMENTS
        );
        preventSelfReview(submission, currentUser);
        if (submission.getSubmissionTime() == null || submission.getStatus() == SubmissionStatus.NOT_SUBMITTED) {
            throw new BusinessException("Cannot grade a submission that has not been turned in");
        }

        Integer maxScore = submission.getAssignment().getMaxScore();
        if (maxScore != null && (request.getGrade() < 0 || request.getGrade() > maxScore)) {
            throw new BusinessException("Grade must be between 0 and " + maxScore);
        }

        submission.setGrade(request.getGrade());
        submission.setFeedback(request.getFeedback());
        submission.setGradedAt(LocalDateTime.now());
        submission.setStatus(SubmissionStatus.GRADED);

        SubmissionResponse response = convertToResponse(submissionRepository.save(submission));
        cacheInvalidationService.evictTeachingAndReportCaches();
        // Báo cho học sinh khi bài tập đã được chấm, kèm ref-link tới trang bài tập.
        if (submission.getStudent() != null) {
            Integer gradedCsId = submission.getClassSection() != null ? submission.getClassSection().getId() : null;
            Integer gradedAssignmentId = submission.getAssignment() != null ? submission.getAssignment().getId() : null;
            notificationService.createNotification(
                    submission.getStudent(),
                    "Bài tập đã được chấm",
                    "Giảng viên đã chấm bài tập của bạn.",
                    "ASSIGNMENT_GRADED",
                    null,
                    (gradedCsId != null && gradedAssignmentId != null)
                            ? "/class-sections/" + gradedCsId + "/assignments/" + gradedAssignmentId
                            : null,
                    null,
                    gradedCsId,
                    submission.getClassSection() != null ? submission.getClassSection().getTitle() : null,
                    "ASSIGNMENT",
                    gradedAssignmentId,
                    null
            );
        }
        return response;
    }

    @Override
    @Transactional
    public SubmissionResponse returnSubmission(Integer submissionId, SubmissionReturnRequest request) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));
        User currentUser = requireTeachingPermission(
                submission.getClassSection(),
                ClassMemberAuthorizationService.CAP_MANAGE_ASSIGNMENTS
        );
        preventSelfReview(submission, currentUser);
        if (submission.getSubmissionTime() == null || submission.getStatus() == SubmissionStatus.NOT_SUBMITTED) {
            throw new BusinessException("Cannot return a submission that has not been turned in");
        }

        submission.setFeedback(request.getFeedback());
        submission.setStatus(SubmissionStatus.RETURNED);
        submission.setGradedAt(LocalDateTime.now());
        SubmissionResponse response = convertToResponse(submissionRepository.save(submission));
        cacheInvalidationService.evictTeachingAndReportCaches();
        if (submission.getStudent() != null) {
            Integer returnedCsId = submission.getClassSection() != null ? submission.getClassSection().getId() : null;
            Integer returnedAssignmentId = submission.getAssignment() != null ? submission.getAssignment().getId() : null;
            notificationService.createNotification(
                    submission.getStudent(),
                    "Bài tập được trả lại",
                    "Giảng viên đã trả lại bài tập để bạn chỉnh sửa và nộp lại.",
                    "ASSIGNMENT_RETURNED",
                    null,
                    (returnedCsId != null && returnedAssignmentId != null)
                            ? "/class-sections/" + returnedCsId + "/assignments/" + returnedAssignmentId
                            : null,
                    null,
                    returnedCsId,
                    submission.getClassSection() != null ? submission.getClassSection().getTitle() : null,
                    "ASSIGNMENT",
                    returnedAssignmentId,
                    null
            );
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionResponse getSubmissionById(Integer submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));

        User currentUser = requireCurrentUser();
        if (currentUser.getRole().getRoleName() == RoleType.ADMIN) {
            return convertToResponse(submission);
        }

        boolean isStudentOwner = currentUser.getRole().getRoleName() == RoleType.STUDENT
                && submission.getStudent() != null
                && submission.getStudent().getId().equals(currentUser.getId());
        if (isStudentOwner) {
            return convertToResponse(submission);
        }

        requireTeachingPermission(submission.getClassSection(), ClassMemberAuthorizationService.CAP_MANAGE_ASSIGNMENTS);
        return convertToResponse(submission);
    }

    private void validateSubmissionPayload(SubmissionRequest request) {
        boolean hasDescription = StringUtils.hasText(request.getDescription());
        boolean hasResources = !resolveRequestedResources(request).isEmpty();
        if (!hasDescription && !hasResources) {
            throw new BusinessException("Submission must include text or resources");
        }
    }

    private List<ResourceRequest> resolveRequestedResources(SubmissionRequest request) {
        List<ResourceRequest> resources = new ArrayList<>();
        if (request.getResources() != null) {
            resources.addAll(request.getResources());
        }

        if (StringUtils.hasText(request.getFileUrl()) || StringUtils.hasText(request.getCloudinaryId())) {
            ResourceRequest fileResource = new ResourceRequest();
            fileResource.setTitle("Attachment");
            fileResource.setSource(ResourceSource.UPLOAD);
            fileResource.setFileUrl(request.getFileUrl());
            fileResource.setCloudinaryId(request.getCloudinaryId());
            resources.add(fileResource);
        }

        if (StringUtils.hasText(request.getEmbedUrl())) {
            ResourceRequest linkResource = new ResourceRequest();
            linkResource.setTitle("Link");
            linkResource.setType(ResourceType.LINK);
            linkResource.setSource(ResourceSource.LINK);
            linkResource.setFileUrl(request.getEmbedUrl());
            resources.add(linkResource);
        }
        return resources;
    }

    private void applySubmissionResources(Submission submission, List<ResourceRequest> requestedResources) {
        if (submission.getResources() == null) {
            submission.setResources(new ArrayList<>());
        }
        submission.getResources().clear();

        if (requestedResources == null || requestedResources.isEmpty()) {
            return;
        }

        for (ResourceRequest request : requestedResources) {
            Resource resource = resourceService.buildDetachedResource(request);
            resource.setSubmission(submission);
            resource.setAssignment(null);
            resource.setLesson(null);
            submission.getResources().add(resource);
        }
    }

    private void syncLegacySubmissionFields(Submission submission) {
        submission.setFileUrl(null);
        submission.setEmbedUrl(null);
        submission.setCloudinaryId(null);
        if (submission.getResources() == null) {
            return;
        }

        for (Resource resource : submission.getResources()) {
            if ((resource.getSource() == ResourceSource.UPLOAD || resource.getSource() == ResourceSource.LINK)
                    && submission.getFileUrl() == null) {
                submission.setFileUrl(resource.getFileUrl());
                submission.setCloudinaryId(resource.getCloudinaryId());
                continue;
            }
            if (resource.getSource() == ResourceSource.EMBED && submission.getEmbedUrl() == null) {
                submission.setEmbedUrl(resource.getEmbedUrl());
            }
        }
    }

    private ClassSection resolveClassSection(Assignment assignment, Integer classSectionId) {
        if (classSectionId == null) {
            throw new BusinessException("classSectionId is required for assignments");
        }

        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        if (!classContentItemRepository.existsAssignmentInClassSection(classSectionId, assignment.getId())) {
            throw new BusinessException("Assignment is not available in this class section");
        }
        return classSection;
    }

    private void ensureStudentEnrollment(User student, Integer classSectionId) {
        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        ensureClassSectionInteractive(classSection);
        boolean isApproved = enrollmentRepository.existsByStudent_IdAndClassSection_IdAndApprovalStatus(
                student.getId(),
                classSectionId,
                EnrollmentStatus.APPROVED
        );
        if (!isApproved) {
            throw new UnauthorizedException("You are not enrolled in this class section");
        }
    }

    private boolean isLateSubmission(Assignment assignment, LocalDateTime submittedAt) {
        return assignment.getDueAt() != null && submittedAt.isAfter(assignment.getDueAt());
    }

    private boolean isSubmissionClosed(Assignment assignment, LocalDateTime now) {
        return assignment.getCloseAt() != null && now.isAfter(assignment.getCloseAt());
    }

    private boolean canResubmit(Assignment assignment) {
        return !isSubmissionClosed(assignment, LocalDateTime.now());
    }

    private SubmissionResponse convertToResponse(Submission submission) {
        SubmissionResponse response = new SubmissionResponse();
        response.setId(submission.getId());
        response.setAssignmentId(submission.getAssignment() != null ? submission.getAssignment().getId() : null);
        response.setAssignmentTitle(submission.getAssignment() != null ? submission.getAssignment().getTitle() : null);
        response.setClassSectionId(submission.getClassSection() != null ? submission.getClassSection().getId() : null);
        response.setStudentId(submission.getStudent() != null ? submission.getStudent().getId() : null);
        response.setStudentName(submission.getStudent() != null ? submission.getStudent().getFullName() : null);
        response.setStudentNumber(submission.getStudent() != null ? submission.getStudent().getStudentNumber() : null);
        response.setStudentAvatar(submission.getStudent() != null ? submission.getStudent().getImageUrl() : null);
        response.setDescription(submission.getDescription());
        response.setFileUrl(submission.getFileUrl());
        response.setEmbedUrl(submission.getEmbedUrl());
        response.setCloudinaryId(submission.getCloudinaryId());
        response.setSubmissionTime(submission.getSubmissionTime());
        response.setStatus(submission.getStatus());
        response.setSubmissionCount(submission.getSubmissionCount());
        response.setGrade(submission.getGrade());
        response.setFeedback(submission.getFeedback());
        response.setGradedAt(submission.getGradedAt());
        response.setDueAt(submission.getAssignment() != null ? submission.getAssignment().getDueAt() : null);
        response.setCloseAt(submission.getAssignment() != null ? submission.getAssignment().getCloseAt() : null);
        response.setLate(submission.getStatus() == SubmissionStatus.LATE_SUBMITTED);
        response.setCanResubmit(submission.getAssignment() != null && canResubmit(submission.getAssignment()));
        response.setResources(
                submission.getResources() == null
                        ? List.of()
                        : submission.getResources().stream().map(resourceService::convertEntityToDTO).toList()
        );
        return response;
    }

    private SubmissionResponse buildNotSubmittedResponse(Assignment assignment, ClassSection classSection, User student) {
        SubmissionResponse response = new SubmissionResponse();
        response.setAssignmentId(assignment.getId());
        response.setAssignmentTitle(assignment.getTitle());
        response.setClassSectionId(classSection.getId());
        response.setStudentId(student.getId());
        response.setStudentName(student.getFullName());
        response.setStudentNumber(student.getStudentNumber());
        response.setStudentAvatar(student.getImageUrl());
        response.setStatus(SubmissionStatus.NOT_SUBMITTED);
        response.setSubmissionCount(0);
        response.setDueAt(assignment.getDueAt());
        response.setCloseAt(assignment.getCloseAt());
        response.setLate(false);
        response.setCanResubmit(canResubmit(assignment));
        response.setResources(List.of());
        return response;
    }

    private User requireTeachingPermission(ClassSection classSection, String capability) {
        ensureClassSectionInteractive(classSection);
        User currentUser = requireCurrentUser();
        if (classMemberAuthorizationService.hasCapability(classSection, currentUser, capability)) {
            return currentUser;
        }
        throw new UnauthorizedException("You do not have teaching permission in this class section");
    }

    private void ensureClassSectionInteractive(ClassSection classSection) {
        ClassSectionGuard.ensureInteractive(classSection);
    }

    private void preventSelfReview(Submission submission, User currentUser) {
        if (submission.getStudent() != null && submission.getStudent().getId().equals(currentUser.getId())) {
            throw new BusinessException("You cannot review your own submission");
        }
    }

    private User requireCurrentUser() {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        return currentUser;
    }
}
