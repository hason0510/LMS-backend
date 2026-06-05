package com.example.backend.service;

import com.example.backend.constant.EnrollmentStatus;
import com.example.backend.constant.RoleType;
import com.example.backend.entity.ClassContentItem;
import com.example.backend.entity.ClassSection;
import com.example.backend.entity.User;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.repository.ClassContentItemRepository;
import com.example.backend.repository.EnrollmentRepository;
import com.example.backend.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentAccessService {
    private final LessonRepository lessonRepository;
    private final ClassContentItemRepository classContentItemRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassMemberAuthorizationService classMemberAuthorizationService;

    public void assertCanAccessLesson(Integer lessonId, User user) {
        if (!canAccessLesson(lessonId, user)) {
            throw new UnauthorizedException("Bạn không có quyền truy cập bình luận của bài giảng này");
        }
    }

    public boolean canAccessLesson(Integer lessonId, User user) {
        if (lessonId == null || user == null) {
            return false;
        }
        ClassSection classSection = resolveClassSectionByLessonId(lessonId);
        if (user.getRole() != null && user.getRole().getRoleName() == RoleType.ADMIN) {
            return true;
        }
        if (classMemberAuthorizationService.isTeacherOrTa(classSection, user)) {
            return true;
        }
        return enrollmentRepository.existsByStudent_IdAndClassSection_IdAndApprovalStatus(
                user.getId(),
                classSection.getId(),
                EnrollmentStatus.APPROVED
        );
    }

    public ClassSection resolveClassSectionByLessonId(Integer lessonId) {
        lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài giảng!"));
        ClassContentItem contentItem = classContentItemRepository.findByLesson_Id(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài giảng trong lớp học!"));
        return contentItem.getClassChapter().getClassSection();
    }
}
