package com.example.backend.service;

import com.example.backend.constant.QuestionBankMemberRole;
import com.example.backend.constant.ResourceScopeType;
import com.example.backend.constant.ResourceStatus;
import com.example.backend.constant.ResourceVisibility;
import com.example.backend.constant.RoleType;
import com.example.backend.entity.ClassSection;
import com.example.backend.entity.Resource;
import com.example.backend.entity.User;
import com.example.backend.entity.quiz.QuestionBankMember;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.repository.ClassSectionRepository;
import com.example.backend.repository.QuestionBankMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ResourceAuthorizationService {
    private final UserService userService;
    private final ClassSectionRepository classSectionRepository;
    private final QuestionBankMemberRepository questionBankMemberRepository;
    private final ClassMemberAuthorizationService classMemberAuthorizationService;

    public void assertCanUse(Resource resource, ResourceScopeType expectedScopeType, Integer expectedScopeId) {
        if (!canUse(resource, expectedScopeType, expectedScopeId)) {
            throw new UnauthorizedException("You have no permission to use this resource");
        }
    }

    public void assertCanManage(Resource resource) {
        if (!canManage(resource)) {
            throw new UnauthorizedException("You have no permission to manage this resource");
        }
    }

    public void assertCanDelete(Resource resource) {
        if (!canDelete(resource)) {
            throw new UnauthorizedException("You have no permission to delete this resource");
        }
    }

    public boolean canUse(Resource resource, ResourceScopeType expectedScopeType, Integer expectedScopeId) {
        if (resource == null) {
            return false;
        }
        User currentUser = userService.getCurrentUser();
        if (isAdmin(currentUser)) {
            return true;
        }
        if (resource.getStatus() == ResourceStatus.ARCHIVED) {
            return isCreatedBy(resource, currentUser);
        }

        if (resource.getVisibility() == ResourceVisibility.INSTITUTION) {
            return true;
        }

        if (isLegacyResource(resource)) {
            return true;
        }

        if (isCreatedBy(resource, currentUser)) {
            return true;
        }

        if (expectedScopeType != null
                && expectedScopeType == resource.getScopeType()
                && expectedScopeId != null
                && expectedScopeId.equals(resource.getScopeId())
                && resource.getVisibility() == ResourceVisibility.SHARED) {
            return canBrowseScope(expectedScopeType, expectedScopeId);
        }

        return resource.getVisibility() == ResourceVisibility.SHARED
                && canBrowseScope(resource.getScopeType(), resource.getScopeId());
    }

    public boolean canManage(Resource resource) {
        if (resource == null) {
            return false;
        }
        User currentUser = userService.getCurrentUser();
        return isAdmin(currentUser) || isCreatedBy(resource, currentUser);
    }

    public boolean canDelete(Resource resource) {
        return canManage(resource);
    }

    public boolean canBrowseScope(ResourceScopeType scopeType, Integer scopeId) {
        User currentUser = userService.getCurrentUser();
        if (isAdmin(currentUser)) {
            return true;
        }
        if (scopeType == null) {
            return false;
        }
        if (scopeType == ResourceScopeType.INSTITUTION_SHARED) {
            return false;
        }
        if (scopeType == ResourceScopeType.QUESTION_BANK) {
            return currentUser != null
                    && scopeId != null
                    && questionBankMemberRepository.existsByQuestionBank_IdAndUser_Id(scopeId, currentUser.getId());
        }
        if (scopeType == ResourceScopeType.CLASS_SECTION) {
            if (currentUser == null || scopeId == null) {
                return false;
            }
            ClassSection classSection = classSectionRepository.findById(scopeId).orElse(null);
            return classMemberAuthorizationService.isTeacherOrTa(classSection, currentUser);
        }
        if (scopeType == ResourceScopeType.CURRICULUM_TEMPLATE) {
            return currentUser != null && currentUser.getRole() != null
                    && currentUser.getRole().getRoleName() == RoleType.TEACHER;
        }
        return false;
    }

    public void assertCanCreateInScope(ResourceScopeType scopeType, Integer scopeId) {
        if (!canCreateInScope(scopeType, scopeId)) {
            throw new UnauthorizedException("You have no permission to create media in this scope");
        }
    }

    public boolean canCreateInScope(ResourceScopeType scopeType, Integer scopeId) {
        User currentUser = userService.getCurrentUser();
        if (isAdmin(currentUser)) {
            return true;
        }
        if (currentUser == null) {
            return false;
        }
        if (scopeType == null || scopeType == ResourceScopeType.PRIVATE_USER) {
            return true;
        }
        if (scopeType == ResourceScopeType.INSTITUTION_SHARED) {
            return false;
        }
        if (scopeType == ResourceScopeType.QUESTION_BANK) {
            QuestionBankMember member = scopeId == null
                    ? null
                    : questionBankMemberRepository.findByQuestionBank_IdAndUser_Id(scopeId, currentUser.getId()).orElse(null);
            return member != null
                    && (member.getRole() == QuestionBankMemberRole.OWNER
                    || member.getRole() == QuestionBankMemberRole.EDITOR);
        }
        if (scopeType == ResourceScopeType.CLASS_SECTION) {
            ClassSection classSection = scopeId == null ? null : classSectionRepository.findById(scopeId).orElse(null);
            return classMemberAuthorizationService.canEditContent(classSection, currentUser)
                    || classMemberAuthorizationService.canManageAssignments(classSection, currentUser);
        }
        if (scopeType == ResourceScopeType.CURRICULUM_TEMPLATE) {
            return currentUser.getRole() != null
                    && currentUser.getRole().getRoleName() == RoleType.TEACHER;
        }
        return false;
    }

    private boolean isLegacyResource(Resource resource) {
        return !StringUtils.hasText(resource.getCreatedBy())
                && resource.getScopeType() == null
                && resource.getScopeId() == null;
    }

    private boolean isCreatedBy(Resource resource, User user) {
        return user != null
                && StringUtils.hasText(resource.getCreatedBy())
                && resource.getCreatedBy().equals(user.getUserName());
    }

    private boolean isAdmin(User user) {
        return user != null
                && user.getRole() != null
                && user.getRole().getRoleName() == RoleType.ADMIN;
    }
}
