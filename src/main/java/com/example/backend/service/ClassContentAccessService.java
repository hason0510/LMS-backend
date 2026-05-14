package com.example.backend.service;

import com.example.backend.constant.ClassContentAvailabilityStatus;
import com.example.backend.constant.ContentItemType;
import com.example.backend.constant.EnrollmentStatus;
import com.example.backend.constant.RoleType;
import com.example.backend.entity.ClassChapter;
import com.example.backend.entity.ClassContentItem;
import com.example.backend.entity.ClassSection;
import com.example.backend.entity.User;
import com.example.backend.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ClassContentAccessService {
    private final EnrollmentRepository enrollmentRepository;
    private final ClassMemberAuthorizationService classMemberAuthorizationService;

    public ClassContentAccessResult evaluateForUser(ClassContentItem classContentItem, User user) {
        ClassContentAvailabilityStatus chapterStatus = resolveAvailabilityStatus(classContentItem.getClassChapter());
        ClassContentAvailabilityStatus itemStatus = resolveAvailabilityStatus(classContentItem);
        ClassContentAvailabilityStatus baseStatus = mergeAvailabilityStatus(chapterStatus, itemStatus);
        String baseMessage = resolveMessage(baseStatus);
        String baseMessageKey = resolveMessageKey(baseStatus);

        if (isManager(classContentItem, user)) {
            return new ClassContentAccessResult(baseStatus, true, baseMessage, baseMessageKey);
        }

        if (isStudent(user)) {
            Integer classSectionId = classContentItem.getClassChapter().getClassSection().getId();
            boolean isApproved = enrollmentRepository.existsByStudent_IdAndClassSection_IdAndApprovalStatus(
                    user.getId(),
                    classSectionId,
                    EnrollmentStatus.APPROVED
            );
            if (!isApproved) {
                return new ClassContentAccessResult(
                        ClassContentAvailabilityStatus.NO_ENROLLMENT,
                        false,
                        resolveMessage(ClassContentAvailabilityStatus.NO_ENROLLMENT),
                        resolveMessageKey(ClassContentAvailabilityStatus.NO_ENROLLMENT)
                );
            }
            return new ClassContentAccessResult(
                    baseStatus,
                    baseStatus == ClassContentAvailabilityStatus.AVAILABLE,
                    baseMessage,
                    baseMessageKey
            );
        }

        return new ClassContentAccessResult(
                baseStatus,
                false,
                resolveTeachingPermissionMessage(classContentItem),
                "classContent.access.forbidden"
        );
    }

    public ClassContentAvailabilityStatus resolveAvailabilityStatus(ClassChapter classChapter) {
        if (classChapter == null) {
            return ClassContentAvailabilityStatus.AVAILABLE;
        }
        LocalDateTime now = LocalDateTime.now();
        if (Boolean.TRUE.equals(classChapter.getIsHidden())) {
            return ClassContentAvailabilityStatus.HIDDEN;
        }
        if (Boolean.TRUE.equals(classChapter.getIsLocked())) {
            return ClassContentAvailabilityStatus.LOCKED;
        }
        if (classChapter.getAvailableFrom() != null && now.isBefore(classChapter.getAvailableFrom())) {
            return ClassContentAvailabilityStatus.NOT_OPEN_YET;
        }
        if (classChapter.getAvailableTo() != null && now.isAfter(classChapter.getAvailableTo())) {
            return ClassContentAvailabilityStatus.CLOSED;
        }
        return ClassContentAvailabilityStatus.AVAILABLE;
    }

    public ClassContentAvailabilityStatus resolveAvailabilityStatus(ClassContentItem classContentItem) {
        if (classContentItem == null) {
            return ClassContentAvailabilityStatus.AVAILABLE;
        }
        LocalDateTime now = LocalDateTime.now();
        if (Boolean.TRUE.equals(classContentItem.getIsHidden())) {
            return ClassContentAvailabilityStatus.HIDDEN;
        }
        if (Boolean.TRUE.equals(classContentItem.getIsLocked())) {
            return ClassContentAvailabilityStatus.LOCKED;
        }
        if (classContentItem.getAvailableFrom() != null && now.isBefore(classContentItem.getAvailableFrom())) {
            return ClassContentAvailabilityStatus.NOT_OPEN_YET;
        }
        if (classContentItem.getAvailableTo() != null && now.isAfter(classContentItem.getAvailableTo())) {
            return ClassContentAvailabilityStatus.CLOSED;
        }
        return ClassContentAvailabilityStatus.AVAILABLE;
    }

    private ClassContentAvailabilityStatus mergeAvailabilityStatus(
            ClassContentAvailabilityStatus chapterStatus,
            ClassContentAvailabilityStatus itemStatus
    ) {
        if (chapterStatus == ClassContentAvailabilityStatus.HIDDEN || itemStatus == ClassContentAvailabilityStatus.HIDDEN) {
            return ClassContentAvailabilityStatus.HIDDEN;
        }
        if (chapterStatus == ClassContentAvailabilityStatus.LOCKED || itemStatus == ClassContentAvailabilityStatus.LOCKED) {
            return ClassContentAvailabilityStatus.LOCKED;
        }
        if (chapterStatus == ClassContentAvailabilityStatus.NOT_OPEN_YET || itemStatus == ClassContentAvailabilityStatus.NOT_OPEN_YET) {
            return ClassContentAvailabilityStatus.NOT_OPEN_YET;
        }
        if (chapterStatus == ClassContentAvailabilityStatus.CLOSED || itemStatus == ClassContentAvailabilityStatus.CLOSED) {
            return ClassContentAvailabilityStatus.CLOSED;
        }
        return ClassContentAvailabilityStatus.AVAILABLE;
    }

    private String resolveMessage(ClassContentAvailabilityStatus status) {
        return switch (status) {
            case AVAILABLE -> null;
            case HIDDEN -> "Nội dung này đang bị ẩn";
            case LOCKED -> "Nội dung này đang bị khóa";
            case NOT_OPEN_YET -> "Nội dung này chưa đến thời gian mở";
            case CLOSED -> "Nội dung này đã đóng";
            case NO_ENROLLMENT -> "Bạn chưa được duyệt vào lớp học này";
        };
    }

    private String resolveMessageKey(ClassContentAvailabilityStatus status) {
        return switch (status) {
            case AVAILABLE -> null;
            case HIDDEN -> "classContent.access.hidden";
            case LOCKED -> "classContent.access.locked";
            case NOT_OPEN_YET -> "classContent.access.notOpenYet";
            case CLOSED -> "classContent.access.closed";
            case NO_ENROLLMENT -> "classContent.access.noEnrollment";
        };
    }

    private boolean isManager(ClassContentItem classContentItem, User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        if (user.getRole().getRoleName() == RoleType.ADMIN) {
            return true;
        }
        ClassSection classSection = classContentItem.getClassChapter().getClassSection();
        if (!classMemberAuthorizationService.isTeacherOrTa(classSection, user)) {
            return false;
        }

        if (classMemberAuthorizationService.canEditContent(classSection, user)) {
            return true;
        }

        if (classContentItem.getItemType() == ContentItemType.QUIZ) {
            return classMemberAuthorizationService.canReviewQuizzes(classSection, user);
        }
        if (classContentItem.getItemType() == ContentItemType.ASSIGNMENT) {
            return classMemberAuthorizationService.canGradeAssignments(classSection, user);
        }
        return true;
    }

    private boolean isStudent(User user) {
        return user != null
                && user.getRole() != null
                && user.getRole().getRoleName() == RoleType.STUDENT;
    }

    private String resolveTeachingPermissionMessage(ClassContentItem classContentItem) {
        if (classContentItem == null || classContentItem.getItemType() == null) {
            return "Bạn không có quyền truy cập nội dung này";
        }
        return switch (classContentItem.getItemType()) {
            case QUIZ -> "Bạn không có quyền rà soát bài quiz này";
            case ASSIGNMENT -> "Bạn không có quyền chấm bài tập này";
            default -> "Bạn không có quyền truy cập nội dung này";
        };
    }
}
