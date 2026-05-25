package com.example.backend.service;

import com.example.backend.constant.ClassSectionStatus;
import com.example.backend.dto.request.classsection.ClassChapterOverrideRequest;
import com.example.backend.dto.request.classsection.ClassChapterCreateRequest;
import com.example.backend.dto.request.classsection.ClassContentItemOverrideRequest;
import com.example.backend.dto.request.classsection.ClassContentItemCreateRequest;
import com.example.backend.dto.request.classsection.ClassMemberPermissionsRequest;
import com.example.backend.dto.request.classsection.ClassMemberRequest;
import com.example.backend.dto.request.classsection.ClassMemberRoleRequest;
import com.example.backend.dto.request.classsection.ClassSectionRequest;
import com.example.backend.dto.request.classsection.ClassSectionSearchRequest;
import com.example.backend.dto.request.classsection.ClassSectionUpdateRequest;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.classsection.ClassChapterResponse;
import com.example.backend.dto.response.classsection.ClassContentItemResponse;
import com.example.backend.dto.response.classsection.ClassSectionJoinPreviewResponse;
import com.example.backend.dto.response.classsection.ClassMemberResponse;
import com.example.backend.dto.response.classsection.ClassSectionResponse;
import java.util.List;

public interface ClassSectionService {
    ClassSectionResponse createFromTemplate(Integer curriculumTemplateId, ClassSectionRequest request);

    ClassSectionResponse getClassSectionById(Integer id);

    List<ClassSectionResponse> getClassSections(Integer teacherId, Integer subjectId, Integer curriculumTemplateId, boolean includeChapters);

    PageResponse<ClassSectionResponse> searchClassSections(ClassSectionSearchRequest request);

    ClassSectionJoinPreviewResponse getJoinPreview(String classCode);

    ClassMemberResponse addMember(Integer classSectionId, ClassMemberRequest request);

    ClassMemberResponse updateMemberRole(Integer classSectionId, Integer userId, ClassMemberRoleRequest request);

    ClassMemberResponse updateMemberPermissions(Integer classSectionId, Integer userId, ClassMemberPermissionsRequest request);

    void removeMember(Integer classSectionId, Integer userId);

    List<ClassMemberResponse> getMembers(Integer classSectionId);

    ClassChapterResponse createClassChapter(Integer classSectionId, ClassChapterCreateRequest request);

    ClassChapterResponse updateClassChapter(
            Integer classSectionId,
            Integer classChapterId,
            ClassChapterOverrideRequest request
    );

    void deleteClassChapter(Integer classSectionId, Integer classChapterId);

    ClassContentItemResponse createClassContentItem(
            Integer classSectionId,
            Integer classChapterId,
            ClassContentItemCreateRequest request
    );

    ClassContentItemResponse updateClassContentItem(
            Integer classSectionId,
            Integer classContentItemId,
            ClassContentItemOverrideRequest request
    );

    void deleteClassContentItem(Integer classSectionId, Integer classContentItemId);

    List<ClassSectionResponse> getApprovedClassSectionsForStudent();

    List<ClassSectionResponse> getPendingClassSectionsForStudent();

    List<ClassSectionResponse> getAllClassSectionsForStudent();

    List<ClassChapterResponse> getClassChapters(Integer classSectionId);

    List<ClassContentItemResponse> getClassContentItems(Integer classSectionId, Integer classChapterId);

    ClassSectionResponse updateClassSectionStatus(Integer id, ClassSectionStatus status);

    ClassSectionResponse resetClassCode(Integer id);

    ClassSectionResponse deleteClassCode(Integer id);

    void deleteClassSection(Integer id);

    ClassSectionResponse updateClassSection(Integer id, ClassSectionUpdateRequest request);
}
