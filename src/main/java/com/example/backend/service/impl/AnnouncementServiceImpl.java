package com.example.backend.service.impl;

import com.example.backend.constant.ClassMemberRole;
import com.example.backend.constant.ClassSectionStatus;
import com.example.backend.constant.EnrollmentStatus;
import com.example.backend.constant.RoleType;
import com.example.backend.dto.request.AnnouncementRequest;
import com.example.backend.dto.response.AnnouncementResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.entity.Announcement;
import com.example.backend.entity.ClassSection;
import com.example.backend.entity.Subject;
import com.example.backend.entity.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.repository.AnnouncementRepository;
import com.example.backend.repository.ClassMemberRepository;
import com.example.backend.repository.ClassSectionRepository;
import com.example.backend.repository.EnrollmentRepository;
import com.example.backend.service.AnnouncementService;
import com.example.backend.service.ClassMemberAuthorizationService;
import com.example.backend.service.ClassNotificationService;
import com.example.backend.service.UserService;
import com.example.backend.specification.AnnouncementSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {
    private final AnnouncementRepository announcementRepository;
    private final ClassSectionRepository classSectionRepository;
    private final ClassMemberRepository classMemberRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserService userService;
    private final ClassMemberAuthorizationService classMemberAuthorizationService;
    private final ClassNotificationService classNotificationService;

    @Override
    @Transactional
    public AnnouncementResponse createAnnouncement(AnnouncementRequest request) {
        ClassSection classSection = classSectionRepository.findById(request.getClassSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        requireManagePermission(classSection);

        User currentUser = requireCurrentUser();
        Announcement announcement = new Announcement();
        announcement.setClassSection(classSection);
        announcement.setTitle(request.getTitle().trim());
        announcement.setSummary(request.getSummary());
        announcement.setCreatedByUser(currentUser);
        Announcement saved = announcementRepository.save(announcement);

        classNotificationService.notifyApprovedStudents(
                classSection,
                saved.getTitle(),
                StringUtils.hasText(saved.getSummary()) ? saved.getSummary() : "Có thông báo mới trong lớp học.",
                "ANNOUNCEMENT",
                saved.getSummary(),
                null,
                "/class-sections/" + classSection.getId() + "?announcementId=" + saved.getId(),
                "ANNOUNCEMENT",
                saved.getId(),
                "announcement-created"
        );

        return convertToResponse(saved);
    }

    @Override
    @Transactional
    public AnnouncementResponse updateAnnouncement(Integer id, AnnouncementRequest request) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));
        requireManagePermission(announcement.getClassSection());

        ClassSection classSection = classSectionRepository.findById(request.getClassSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
        requireManagePermission(classSection);

        announcement.setClassSection(classSection);
        announcement.setTitle(request.getTitle().trim());
        announcement.setSummary(request.getSummary());
        return convertToResponse(announcementRepository.save(announcement));
    }

    @Override
    @Transactional(readOnly = true)
    public AnnouncementResponse getAnnouncementById(Integer id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));
        requireViewPermission(announcement.getClassSection());
        return convertToResponse(announcement);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AnnouncementResponse> getAnnouncements(
            Integer classSectionId,
            Integer subjectId,
            String subjectKeyword,
            String classTitle,
            String contentKeyword,
            String sort,
            LocalDate date,
            LocalDate dateFrom,
            LocalDate dateTo,
            String search,
            Integer pageNumber,
            Integer pageSize
    ) {
        Set<Integer> accessibleIds;
        if (classSectionId != null) {
            ClassSection classSection = classSectionRepository.findById(classSectionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
            requireViewPermission(classSection);
            accessibleIds = Set.of(classSectionId);
        } else {
            accessibleIds = resolveAccessibleClassSectionIds();
            if (accessibleIds.isEmpty()) {
                return new PageResponse<>(pageNumber, 0, 0, List.of());
            }
        }

        int safePage = pageNumber != null && pageNumber > 0 ? pageNumber : 1;
        int safeSize = pageSize != null && pageSize > 0 ? pageSize : 10;
        Sort.Direction direction = "ASC".equalsIgnoreCase(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String resolvedContentKeyword = StringUtils.hasText(contentKeyword) ? contentKeyword : search;
        LocalDate resolvedDateFrom = dateFrom != null ? dateFrom : date;
        LocalDate resolvedDateTo = dateTo != null ? dateTo : date;

        Specification<Announcement> spec = Specification
                .where(AnnouncementSpecification.inClassSections(accessibleIds))
                .and(AnnouncementSpecification.createdBetween(resolvedDateFrom, resolvedDateTo));
        if (subjectId != null) {
            spec = spec.and(AnnouncementSpecification.hasSubjectId(subjectId));
        }
        if (StringUtils.hasText(subjectKeyword)) {
            spec = spec.and(AnnouncementSpecification.subjectTitleOrCodeContains(subjectKeyword));
        }
        if (StringUtils.hasText(classTitle)) {
            spec = spec.and(AnnouncementSpecification.classTitleContains(classTitle));
        }
        if (StringUtils.hasText(resolvedContentKeyword)) {
            spec = spec.and(AnnouncementSpecification.titleOrSummaryContains(resolvedContentKeyword));
        }

        Page<Announcement> page = announcementRepository.findAll(
                spec,
                PageRequest.of(safePage - 1, safeSize, Sort.by(direction, "createdAt"))
        );

        return new PageResponse<>(
                safePage,
                page.getTotalPages(),
                page.getTotalElements(),
                page.map(this::convertToResponse).getContent()
        );
    }

    @Override
    @Transactional
    public void deleteAnnouncement(Integer id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));
        requireManagePermission(announcement.getClassSection());
        announcementRepository.delete(announcement);
    }

    private Set<Integer> resolveAccessibleClassSectionIds() {
        User currentUser = requireCurrentUser();
        RoleType role = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : null;
        Set<Integer> ids = new LinkedHashSet<>();

        if (role == RoleType.ADMIN) {
            classSectionRepository.findAll().forEach(classSection -> ids.add(classSection.getId()));
            return ids;
        }

        if (role == RoleType.TEACHER) {
            classSectionRepository.findByTeacher_Id(currentUser.getId()).forEach(classSection -> ids.add(classSection.getId()));
            classMemberRepository.findClassSectionsByMemberRole(currentUser.getId(), ClassMemberRole.TEACHER)
                    .forEach(classSection -> ids.add(classSection.getId()));
            classMemberRepository.findClassSectionsByMemberRole(currentUser.getId(), ClassMemberRole.TA)
                    .forEach(classSection -> ids.add(classSection.getId()));
            return ids;
        }

        if (role == RoleType.STUDENT) {
            enrollmentRepository.findByStudent_IdAndApprovalStatus(currentUser.getId(), EnrollmentStatus.APPROVED, org.springframework.data.domain.Pageable.unpaged())
                    .forEach(enrollment -> {
                        if (enrollment.getClassSection() != null) {
                            ids.add(enrollment.getClassSection().getId());
                        }
                    });
        }
        return ids;
    }

    private void requireManagePermission(ClassSection classSection) {
        ensureClassSectionInteractive(classSection);
        User currentUser = requireCurrentUser();
        if (classMemberAuthorizationService.canPostAnnouncements(classSection, currentUser)) {
            return;
        }
        throw new UnauthorizedException("You do not have permission to manage announcements in this class section");
    }

    private void requireViewPermission(ClassSection classSection) {
        User currentUser = requireCurrentUser();
        if (classMemberAuthorizationService.isTeacherOrTa(classSection, currentUser)) {
            return;
        }
        if (currentUser.getRole() != null && currentUser.getRole().getRoleName() == RoleType.STUDENT
                && enrollmentRepository.existsByStudent_IdAndClassSection_IdAndApprovalStatus(
                currentUser.getId(),
                classSection.getId(),
                EnrollmentStatus.APPROVED
        )) {
            return;
        }
        throw new UnauthorizedException("You do not have permission to view announcements in this class section");
    }

    private AnnouncementResponse convertToResponse(Announcement announcement) {
        ClassSection classSection = announcement.getClassSection();
        Subject subject = classSection != null ? classSection.getSubject() : null;
        User createdBy = announcement.getCreatedByUser();
        return new AnnouncementResponse(
                announcement.getId(),
                classSection != null ? classSection.getId() : null,
                classSection != null ? classSection.getTitle() : null,
                subject != null ? subject.getId() : null,
                subject != null ? subject.getCode() : null,
                subject != null ? subject.getTitle() : null,
                announcement.getTitle(),
                announcement.getSummary(),
                createdBy != null ? createdBy.getId() : null,
                createdBy != null ? createdBy.getFullName() : null,
                announcement.getCreatedAt(),
                announcement.getUpdatedAt()
        );
    }

    private User requireCurrentUser() {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        return currentUser;
    }

    private void ensureClassSectionInteractive(ClassSection classSection) {
        if (classSection != null && classSection.getStatus() == ClassSectionStatus.ARCHIVED) {
            throw new BusinessException("Class section is archived and only supports read-only access");
        }
    }
}
