package com.example.backend.service.impl;

import com.example.backend.constant.EnrollmentStatus;
import com.example.backend.constant.RoleType;
import com.example.backend.dto.request.AssignmentRequest;
import com.example.backend.dto.response.AssignmentResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.entity.assignment.Assignment;
import com.example.backend.entity.ClassContentItem;
import com.example.backend.entity.ClassSection;
import com.example.backend.entity.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.repository.AssignmentRepository;
import com.example.backend.repository.ClassContentItemRepository;
import com.example.backend.repository.ClassSectionRepository;
import com.example.backend.repository.EnrollmentRepository;
import com.example.backend.service.AssignmentService;
import com.example.backend.service.ClassMemberAuthorizationService;
import com.example.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final ClassSectionRepository classSectionRepository;
    private final ClassContentItemRepository classContentItemRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserService userService;
    private final ClassMemberAuthorizationService classMemberAuthorizationService;

    @Override
    @Transactional
    public AssignmentResponse createAssignment(AssignmentRequest request) {
        ClassSection classSection = classSectionRepository.findById(request.getClassSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        requireTeacherOrTaPermission(classSection);

        Assignment assignment = new Assignment();
        applyRequest(assignment, request, classSection);
        return convertToResponse(assignmentRepository.save(assignment));
    }

    @Override
    @Transactional
    public AssignmentResponse updateAssignment(Integer id, AssignmentRequest request) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));

        ClassSection classSection = classSectionRepository.findById(request.getClassSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));

        if (assignment.getClassSection() != null
                && !assignment.getClassSection().getId().equals(classSection.getId())) {
            requireTeacherPermission(assignment.getClassSection());
        }
        requireTeacherPermission(classSection);

        applyRequest(assignment, request, classSection);
        return convertToResponse(assignmentRepository.save(assignment));
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentResponse getAssignmentById(Integer id) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));

        if (assignment.getClassSection() != null) {
            requireViewPermission(assignment.getClassSection());
        } else {
            User currentUser = requireCurrentUser();
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

    private void applyRequest(Assignment assignment, AssignmentRequest request, ClassSection classSection) {
        assignment.setTitle(request.getTitle().trim());
        assignment.setDescription(request.getDescription());
        assignment.setInstruction(request.getInstruction());
        assignment.setMaxScore(request.getMaxScore());
        assignment.setDueAt(request.getDueAt());
        assignment.setAllowLateSubmission(Boolean.TRUE.equals(request.getAllowLateSubmission()));
        assignment.setClassSection(classSection);
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
        response.setAllowLateSubmission(assignment.isAllowLateSubmission());
        response.setClassSectionId(assignment.getClassSection() != null ? assignment.getClassSection().getId() : null);
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
}
