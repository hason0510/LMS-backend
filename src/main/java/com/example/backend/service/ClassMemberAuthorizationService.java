package com.example.backend.service;

import com.example.backend.constant.ClassMemberRole;
import com.example.backend.constant.RoleType;
import com.example.backend.entity.ClassMember;
import com.example.backend.entity.ClassSection;
import com.example.backend.entity.User;
import com.example.backend.repository.ClassMemberRepository;
import com.example.backend.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ClassMemberAuthorizationService {
    public static final String CAP_VIEW_CLASS = "VIEW_CLASS";
    public static final String CAP_VIEW_PEOPLE = "VIEW_PEOPLE";
    public static final String CAP_VIEW_PROGRESS = "VIEW_PROGRESS";
    public static final String CAP_MANAGE_ASSIGNMENTS = "MANAGE_ASSIGNMENTS";
    @Deprecated
    public static final String CAP_GRADE_ASSIGNMENTS = "GRADE_ASSIGNMENTS";
    public static final String CAP_REVIEW_QUIZZES = "REVIEW_QUIZZES";
    public static final String CAP_POST_ANNOUNCEMENTS = "POST_ANNOUNCEMENTS";
    public static final String CAP_REPLY_COMMENTS = "REPLY_COMMENTS";
    public static final String CAP_MANAGE_ENROLLMENTS = "MANAGE_ENROLLMENTS";
    public static final String CAP_EDIT_CONTENT = "EDIT_CONTENT";
    public static final String CAP_MANAGE_CLASS_SETTINGS = "MANAGE_CLASS_SETTINGS";
    public static final String CAP_MANAGE_STAFF = "MANAGE_STAFF";

    private static final List<String> TEACHER_CAPABILITIES = List.of(
            CAP_VIEW_CLASS,
            CAP_VIEW_PEOPLE,
            CAP_VIEW_PROGRESS,
            CAP_MANAGE_ASSIGNMENTS,
            CAP_REVIEW_QUIZZES,
            CAP_POST_ANNOUNCEMENTS,
            CAP_REPLY_COMMENTS,
            CAP_MANAGE_ENROLLMENTS,
            CAP_EDIT_CONTENT,
            CAP_MANAGE_CLASS_SETTINGS,
            CAP_MANAGE_STAFF
    );

    private static final List<String> TA_CAPABILITIES = List.of(
            CAP_VIEW_CLASS,
            CAP_VIEW_PEOPLE,
            CAP_VIEW_PROGRESS,
            CAP_MANAGE_ASSIGNMENTS,
            CAP_REVIEW_QUIZZES,
            CAP_POST_ANNOUNCEMENTS,
            CAP_REPLY_COMMENTS
    );

    private static final List<String> STUDENT_CAPABILITIES = List.of(CAP_VIEW_CLASS);

    private final ClassMemberRepository classMemberRepository;
    private final EnrollmentRepository enrollmentRepository;

    public boolean isTeacher(ClassSection classSection, User user) {
        if (isAdmin(user)) {
            return true;
        }
        if (user == null || classSection == null || classSection.getId() == null) {
            return false;
        }
        if (classMemberRepository.existsByClassSection_IdAndUser_IdAndRole(
                classSection.getId(),
                user.getId(),
                ClassMemberRole.TEACHER
        )) {
            return true;
        }
        return classSection.getTeacher() != null
                && classSection.getTeacher().getId().equals(user.getId());
    }

    public boolean isTeacherOrTa(ClassSection classSection, User user) {
        if (isAdmin(user)) {
            return true;
        }
        if (user == null || classSection == null || classSection.getId() == null) {
            return false;
        }
        if (classMemberRepository.existsByClassSection_IdAndUser_IdAndRoleIn(
                classSection.getId(),
                user.getId(),
                Set.of(ClassMemberRole.TEACHER, ClassMemberRole.TA)
        )) {
            return true;
        }
        return classSection.getTeacher() != null
                && classSection.getTeacher().getId().equals(user.getId());
    }

    public Optional<ClassMemberRole> resolveTeachingRole(ClassSection classSection, User user) {
        if (user == null || classSection == null || classSection.getId() == null) {
            return Optional.empty();
        }

        Optional<ClassMember> member = classMemberRepository.findByClassSection_IdAndUser_Id(
                classSection.getId(),
                user.getId()
        );
        if (member.isPresent() && member.get().getRole() != null) {
            return Optional.of(member.get().getRole());
        }

        if (classSection.getTeacher() != null && classSection.getTeacher().getId().equals(user.getId())) {
            return Optional.of(ClassMemberRole.TEACHER);
        }

        return Optional.empty();
    }

    /**
     * Map userId → vai trò trong lớp (TEACHER/TA) cho toàn bộ thành viên giảng dạy của một lớp.
     * Dùng để hiển thị badge "Trợ giảng" trên bình luận: TA có role toàn cục là STUDENT nên
     * không phân biệt được nếu chỉ dựa vào role toàn cục.
     */
    public Map<Integer, ClassMemberRole> teachingRoleMap(Integer classSectionId) {
        Map<Integer, ClassMemberRole> map = new HashMap<>();
        if (classSectionId == null) {
            return map;
        }
        classMemberRepository.findByClassSection_Id(classSectionId).forEach(member -> {
            if (member.getUser() != null && member.getRole() != null) {
                map.put(member.getUser().getId(), member.getRole());
            }
        });
        return map;
    }

    public String resolveMyClassRole(ClassSection classSection, User user) {
        if (user == null || classSection == null || classSection.getId() == null) {
            return null;
        }
        if (isAdmin(user)) {
            return ClassMemberRole.TEACHER.name();
        }

        Optional<ClassMemberRole> teachingRole = resolveTeachingRole(classSection, user);
        if (teachingRole.isPresent()) {
            return teachingRole.get().name();
        }

        boolean enrolled = enrollmentRepository.findByStudent_IdAndClassSection_Id(
                user.getId(),
                classSection.getId()
        ) != null;
        return enrolled ? RoleType.STUDENT.name() : null;
    }

    public String resolveWorkspaceType(ClassSection classSection, User user) {
        String role = resolveMyClassRole(classSection, user);
        if (role == null) {
            return null;
        }
        if (ClassMemberRole.TEACHER.name().equals(role) || ClassMemberRole.TA.name().equals(role)) {
            return "TEACHING";
        }
        return "LEARNING";
    }

    public List<String> resolveCapabilities(ClassSection classSection, User user) {
        if (user == null || classSection == null || classSection.getId() == null) {
            return List.of();
        }
        if (isAdmin(user)) {
            return TEACHER_CAPABILITIES;
        }

        Optional<ClassMember> member = classMemberRepository.findByClassSection_IdAndUser_Id(
                classSection.getId(),
                user.getId()
        );
        if (member.isPresent() && member.get().getRole() != null) {
            ClassMember classMember = member.get();
            if (classMember.getRole() == ClassMemberRole.TEACHER) {
                return TEACHER_CAPABILITIES;
            }
            List<String> customCapabilities = normalizeCapabilities(classMember.getPermissions(), TA_CAPABILITIES);
            return customCapabilities.isEmpty() ? TA_CAPABILITIES : customCapabilities;
        }

        if (classSection.getTeacher() != null && classSection.getTeacher().getId().equals(user.getId())) {
            return TEACHER_CAPABILITIES;
        }

        if (enrollmentRepository.findByStudent_IdAndClassSection_Id(user.getId(), classSection.getId()) != null) {
            return STUDENT_CAPABILITIES;
        }
        return List.of();
    }

    public List<String> resolveDefaultCapabilities(ClassMemberRole role) {
        if (role == ClassMemberRole.TEACHER) {
            return List.copyOf(TEACHER_CAPABILITIES);
        }
        if (role == ClassMemberRole.TA) {
            return List.copyOf(TA_CAPABILITIES);
        }
        return List.of();
    }

    public List<String> getAssignableTaCapabilities() {
        return List.copyOf(TA_CAPABILITIES);
    }

    public List<String> normalizeTaCapabilities(Collection<String> capabilities) {
        return normalizeCapabilities(capabilities, TA_CAPABILITIES);
    }

    public boolean hasCapability(ClassSection classSection, User user, String capability) {
        if (capability == null) {
            return false;
        }
        return resolveCapabilities(classSection, user).contains(capability);
    }

    public boolean canManageStaff(ClassSection classSection, User user) {
        return hasCapability(classSection, user, CAP_MANAGE_STAFF);
    }

    public boolean canViewPeople(ClassSection classSection, User user) {
        return hasCapability(classSection, user, CAP_VIEW_PEOPLE);
    }

    public boolean canViewProgress(ClassSection classSection, User user) {
        return hasCapability(classSection, user, CAP_VIEW_PROGRESS);
    }

    public boolean canGradeAssignments(ClassSection classSection, User user) {
        return canManageAssignments(classSection, user);
    }

    public boolean canManageAssignments(ClassSection classSection, User user) {
        return hasCapability(classSection, user, CAP_MANAGE_ASSIGNMENTS);
    }

    public boolean canReviewQuizzes(ClassSection classSection, User user) {
        return hasCapability(classSection, user, CAP_REVIEW_QUIZZES);
    }

    public boolean canPostAnnouncements(ClassSection classSection, User user) {
        return hasCapability(classSection, user, CAP_POST_ANNOUNCEMENTS);
    }

    public boolean canReplyComments(ClassSection classSection, User user) {
        return hasCapability(classSection, user, CAP_REPLY_COMMENTS);
    }

    public boolean canManageEnrollments(ClassSection classSection, User user) {
        return hasCapability(classSection, user, CAP_MANAGE_ENROLLMENTS);
    }

    public boolean canEditContent(ClassSection classSection, User user) {
        return hasCapability(classSection, user, CAP_EDIT_CONTENT);
    }

    public boolean canReview(ClassSection classSection, User user) {
        return hasCapability(classSection, user, CAP_REVIEW_QUIZZES)
                || hasCapability(classSection, user, CAP_MANAGE_ASSIGNMENTS);
    }

    public boolean canViewTeachingWorkspace(ClassSection classSection, User user) {
        if (isAdmin(user)) {
            return true;
        }
        return resolveTeachingRole(classSection, user).isPresent();
    }

    public User resolvePrimaryTeacher(ClassSection classSection) {
        if (classSection == null || classSection.getId() == null) {
            return null;
        }
        return classMemberRepository.findByClassSection_IdAndRole(
                        classSection.getId(),
                        ClassMemberRole.TEACHER
                )
                .map(member -> member.getUser())
                .orElse(classSection.getTeacher());
    }

    private boolean isAdmin(User user) {
        return user != null
                && user.getRole() != null
                && user.getRole().getRoleName() == RoleType.ADMIN;
    }

    private List<String> normalizeCapabilities(Collection<String> capabilities, List<String> allowedCapabilities) {
        if (capabilities == null || capabilities.isEmpty()) {
            return List.of();
        }
        Set<String> requested = new LinkedHashSet<>();
        for (String capability : capabilities) {
            if (CAP_GRADE_ASSIGNMENTS.equals(capability)) {
                requested.add(CAP_MANAGE_ASSIGNMENTS);
                continue;
            }
            requested.add(capability);
        }
        return allowedCapabilities.stream()
                .filter(requested::contains)
                .toList();
    }
}
