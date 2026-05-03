package com.example.backend.service.impl;

import com.example.backend.constant.EnrollmentStatus;
import com.example.backend.constant.ResourceSource;
import com.example.backend.constant.RoleType;
import com.example.backend.constant.SubmissionStatus;
import com.example.backend.dto.request.ResourceRequest;
import com.example.backend.dto.request.SubmissionGradeRequest;
import com.example.backend.dto.request.SubmissionRequest;
import com.example.backend.dto.request.SubmissionReturnRequest;
import com.example.backend.dto.response.SubmissionResponse;
import com.example.backend.entity.assignment.Assignment;
import com.example.backend.entity.ClassSection;
import com.example.backend.entity.Enrollment;
import com.example.backend.entity.Resource;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
        boolean lateSubmission = isLateSubmission(assignment, now);
        if (lateSubmission && !assignment.isAllowLateSubmission()) {
            throw new BusinessException("Late submission is not allowed for this assignment");
        }

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
    public List<SubmissionResponse> getAssignmentSubmissions(Integer assignmentId, Integer classSectionId, boolean includeNotSubmitted) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        ClassSection classSection = resolveClassSection(assignment, classSectionId);
        requireTeachingPermission(classSection);

        List<Submission> existingSubmissions = submissionRepository.findByAssignment_IdAndClassSection_IdOrderBySubmissionTimeDesc(
                assignmentId,
                classSection.getId()
        );

        List<SubmissionResponse> response = new ArrayList<>();
        Map<Integer, Submission> submissionByStudentId = new HashMap<>();
        for (Submission submission : existingSubmissions) {
            if (submission.getStudent() == null) {
                continue;
            }
            submissionByStudentId.putIfAbsent(submission.getStudent().getId(), submission);
        }
        for (Submission submission : submissionByStudentId.values()) {
            response.add(convertToResponse(submission));
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
                response.add(buildNotSubmittedResponse(assignment, classSection, student));
            }
        }

        response.sort(Comparator.comparing(
                SubmissionResponse::getStudentName,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
        ));
        return response;
    }

    @Override
    @Transactional
    public SubmissionResponse gradeSubmission(Integer submissionId, SubmissionGradeRequest request) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));
        requireTeachingPermission(submission.getClassSection());
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
        submission.setStatus(Boolean.TRUE.equals(request.getReturnToStudent())
                ? SubmissionStatus.RETURNED
                : SubmissionStatus.GRADED);

        return convertToResponse(submissionRepository.save(submission));
    }

    @Override
    @Transactional
    public SubmissionResponse returnSubmission(Integer submissionId, SubmissionReturnRequest request) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));
        requireTeachingPermission(submission.getClassSection());
        if (submission.getSubmissionTime() == null || submission.getStatus() == SubmissionStatus.NOT_SUBMITTED) {
            throw new BusinessException("Cannot return a submission that has not been turned in");
        }

        submission.setFeedback(request.getFeedback());
        submission.setStatus(SubmissionStatus.RETURNED);
        submission.setGradedAt(LocalDateTime.now());
        return convertToResponse(submissionRepository.save(submission));
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

        requireTeachingPermission(submission.getClassSection());
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
            linkResource.setSource(ResourceSource.EMBED);
            linkResource.setEmbedUrl(request.getEmbedUrl());
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
            if (resource.getSource() == ResourceSource.UPLOAD && submission.getFileUrl() == null) {
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
        if (assignment.getClassSection() != null) {
            if (classSectionId != null && !assignment.getClassSection().getId().equals(classSectionId)) {
                throw new BusinessException("assignment does not belong to the provided classSectionId");
            }
            return assignment.getClassSection();
        }

        if (classSectionId == null) {
            throw new BusinessException("classSectionId is required for template-shared assignments");
        }

        ClassSection classSection = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        if (!classContentItemRepository.existsAssignmentInClassSection(classSectionId, assignment.getId())) {
            throw new BusinessException("Assignment is not available in this class section");
        }
        return classSection;
    }

    private void ensureStudentEnrollment(User student, Integer classSectionId) {
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

    private boolean canResubmit(Assignment assignment) {
        LocalDateTime now = LocalDateTime.now();
        if (assignment.getCloseAt() != null && now.isAfter(assignment.getCloseAt())) {
            return false;
        }
        if (assignment.getDueAt() == null) {
            return true;
        }
        return !now.isAfter(assignment.getDueAt()) || assignment.isAllowLateSubmission();
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
        response.setStatus(SubmissionStatus.NOT_SUBMITTED);
        response.setSubmissionCount(0);
        response.setDueAt(assignment.getDueAt());
        response.setCloseAt(assignment.getCloseAt());
        response.setLate(false);
        response.setCanResubmit(canResubmit(assignment));
        response.setResources(List.of());
        return response;
    }

    private void requireTeachingPermission(ClassSection classSection) {
        User currentUser = requireCurrentUser();
        if (classMemberAuthorizationService.isTeacherOrTa(classSection, currentUser)) {
            return;
        }
        throw new UnauthorizedException("You do not have teaching permission in this class section");
    }

    private User requireCurrentUser() {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        return currentUser;
    }
}
