package com.example.backend.controller;

import com.example.backend.constant.ClassSectionStatus;
import com.example.backend.dto.request.classsection.ClassChapterCreateRequest;
import com.example.backend.dto.request.classsection.ClassChapterOverrideRequest;
import com.example.backend.dto.request.classsection.ClassContentItemCreateRequest;
import com.example.backend.dto.request.classsection.ClassContentItemOverrideRequest;
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
import com.example.backend.service.ClassSectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lms/class-sections")
@RequiredArgsConstructor
@Tag(name = "Class Section Management")
public class ClassSectionController {
    private final ClassSectionService classSectionService;

    @Operation(summary = "Create class section from curriculum template")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/from-template/{curriculumTemplateId}")
    public ResponseEntity<ClassSectionResponse> createFromTemplate(
            @PathVariable Integer curriculumTemplateId,
            @Valid @RequestBody ClassSectionRequest request
    ) {
        ClassSectionResponse response = classSectionService.createFromTemplate(curriculumTemplateId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /*@Operation(summary = "Create class section from latest template alias")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/from-template-latest/{curriculumTemplateId}")
    public ResponseEntity<ClassSectionResponse> createFromTemplateLatest(
            @PathVariable Integer curriculumTemplateId,
            @Valid @RequestBody ClassSectionRequest request
    ) {
        ClassSectionResponse response = classSectionService.createFromTemplate(curriculumTemplateId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }*/

    @Operation(summary = "Search class sections with paging and filters")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    @GetMapping("/search")
    public ResponseEntity<PageResponse<ClassSectionResponse>> searchClassSections(ClassSectionSearchRequest request) {
        return ResponseEntity.ok(classSectionService.searchClassSections(request));
    }

    @Operation(summary = "Get class section detail")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    @GetMapping("/{id}")
    public ResponseEntity<ClassSectionResponse> getClassSectionById(@PathVariable Integer id) {
        return ResponseEntity.ok(classSectionService.getClassSectionById(id));
    }

    @Operation(summary = "Get class section list")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    @GetMapping
    public ResponseEntity<List<ClassSectionResponse>> getClassSections(
            @RequestParam(required = false) Integer teacherId,
            @RequestParam(required = false) Integer subjectId,
            @RequestParam(required = false) Integer curriculumTemplateId,
            @RequestParam(defaultValue = "false") boolean includeChapters
    ) {
        return ResponseEntity.ok(
                classSectionService.getClassSections(teacherId, subjectId, curriculumTemplateId, includeChapters)
        );
    }

    @Operation(summary = "Preview class section by join code")
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/join-preview")
    public ResponseEntity<ClassSectionJoinPreviewResponse> getJoinPreview(@RequestParam String classCode) {
        return ResponseEntity.ok(classSectionService.getJoinPreview(classCode));
    }

    @Operation(summary = "Add teaching member (TEACHER/TA)")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/{classSectionId}/members")
    public ResponseEntity<ClassMemberResponse> addMember(
            @PathVariable Integer classSectionId,
            @Valid @RequestBody ClassMemberRequest request
    ) {
        ClassMemberResponse response = classSectionService.addMember(classSectionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Update class member role")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PutMapping("/{classSectionId}/members/{userId}")
    public ResponseEntity<ClassMemberResponse> updateMemberRole(
            @PathVariable Integer classSectionId,
            @PathVariable Integer userId,
            @Valid @RequestBody ClassMemberRoleRequest request
    ) {
        return ResponseEntity.ok(classSectionService.updateMemberRole(classSectionId, userId, request));
    }

    @Operation(summary = "Update TA permissions")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PatchMapping("/{classSectionId}/members/{userId}/permissions")
    public ResponseEntity<ClassMemberResponse> updateMemberPermissions(
            @PathVariable Integer classSectionId,
            @PathVariable Integer userId,
            @Valid @RequestBody ClassMemberPermissionsRequest request
    ) {
        return ResponseEntity.ok(classSectionService.updateMemberPermissions(classSectionId, userId, request));
    }

    @Operation(summary = "Remove class member")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @DeleteMapping("/{classSectionId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Integer classSectionId,
            @PathVariable Integer userId
    ) {
        classSectionService.removeMember(classSectionId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get teaching members by class section")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/{classSectionId}/members")
    public ResponseEntity<List<ClassMemberResponse>> getMembers(@PathVariable Integer classSectionId) {
        return ResponseEntity.ok(classSectionService.getMembers(classSectionId));
    }

    @Operation(summary = "Create class chapter")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/{classSectionId}/chapters")
    public ResponseEntity<ClassChapterResponse> createClassChapter(
            @PathVariable Integer classSectionId,
            @RequestBody ClassChapterCreateRequest request
    ) {
        return ResponseEntity.ok(classSectionService.createClassChapter(classSectionId, request));
    }

    @Operation(summary = "Update class chapter")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PatchMapping("/{classSectionId}/chapters/{classChapterId}")
    public ResponseEntity<ClassChapterResponse> updateClassChapter(
            @PathVariable Integer classSectionId,
            @PathVariable Integer classChapterId,
            @RequestBody ClassChapterOverrideRequest request
    ) {
        return ResponseEntity.ok(classSectionService.updateClassChapter(classSectionId, classChapterId, request));
    }

    @Operation(summary = "Delete class chapter")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @DeleteMapping("/{classSectionId}/chapters/{classChapterId}")
    public ResponseEntity<Void> deleteClassChapter(
            @PathVariable Integer classSectionId,
            @PathVariable Integer classChapterId
    ) {
        classSectionService.deleteClassChapter(classSectionId, classChapterId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Create class content item")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/{classSectionId}/chapters/{classChapterId}/content-items")
    public ResponseEntity<ClassContentItemResponse> createClassContentItem(
            @PathVariable Integer classSectionId,
            @PathVariable Integer classChapterId,
            @RequestBody ClassContentItemCreateRequest request
    ) {
        return ResponseEntity.ok(
                classSectionService.createClassContentItem(classSectionId, classChapterId, request)
        );
    }

    @Operation(summary = "Update class content item")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PatchMapping("/{classSectionId}/content-items/{classContentItemId}")
    public ResponseEntity<ClassContentItemResponse> updateClassContentItem(
            @PathVariable Integer classSectionId,
            @PathVariable Integer classContentItemId,
            @RequestBody ClassContentItemOverrideRequest request
    ) {
        return ResponseEntity.ok(
                classSectionService.updateClassContentItem(classSectionId, classContentItemId, request)
        );
    }

    @Operation(summary = "Delete class content item")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @DeleteMapping("/{classSectionId}/content-items/{classContentItemId}")
    public ResponseEntity<Void> deleteClassContentItem(
            @PathVariable Integer classSectionId,
            @PathVariable Integer classContentItemId
    ) {
        classSectionService.deleteClassContentItem(classSectionId, classContentItemId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get student approved class sections")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @GetMapping("/approved")
    public ResponseEntity<List<ClassSectionResponse>> getApprovedClassSectionsForStudent() {
        return ResponseEntity.ok(classSectionService.getApprovedClassSectionsForStudent());
    }

    @Operation(summary = "Get student pending class sections")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @GetMapping("/pending")
    public ResponseEntity<List<ClassSectionResponse>> getPendingClassSectionsForStudent() {
        return ResponseEntity.ok(classSectionService.getPendingClassSectionsForStudent());
    }

    @Operation(summary = "Get all student class sections (enrolled)")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @GetMapping("/my-classes")
    public ResponseEntity<List<ClassSectionResponse>> getAllClassSectionsForStudent() {
        return ResponseEntity.ok(classSectionService.getAllClassSectionsForStudent());
    }

    @Operation(summary = "Get class chapters")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/chapters")
    public ResponseEntity<List<ClassChapterResponse>> getClassChapters(@PathVariable Integer id) {
        return ResponseEntity.ok(classSectionService.getClassChapters(id));
    }

    @Operation(summary = "Get class content items")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/chapters/{chapterId}/content-items")
    public ResponseEntity<List<ClassContentItemResponse>> getClassContentItems(
            @PathVariable Integer id,
            @PathVariable Integer chapterId
    ) {
        return ResponseEntity.ok(classSectionService.getClassContentItems(id, chapterId));
    }

    @Operation(summary = "Update class section status")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ClassSectionResponse> updateClassSectionStatus(
            @PathVariable Integer id,
            @RequestParam ClassSectionStatus status
    ) {
        return ResponseEntity.ok(classSectionService.updateClassSectionStatus(id, status));
    }

    @Operation(summary = "Regenerate class code")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/{id}/class-code/reset")
    public ResponseEntity<ClassSectionResponse> resetClassCode(@PathVariable Integer id) {
        return ResponseEntity.ok(classSectionService.resetClassCode(id));
    }

    @Operation(summary = "Delete (disable) class code")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @DeleteMapping("/{id}/class-code")
    public ResponseEntity<ClassSectionResponse> deleteClassCode(@PathVariable Integer id) {
        return ResponseEntity.ok(classSectionService.deleteClassCode(id));
    }

    @Operation(summary = "Delete class section")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClassSection(@PathVariable Integer id) {
        classSectionService.deleteClassSection(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update class section basic info")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PatchMapping("/{id}")
    public ResponseEntity<ClassSectionResponse> updateClassSection(
            @PathVariable Integer id,
            @RequestBody ClassSectionUpdateRequest request
    ) {
        return ResponseEntity.ok(classSectionService.updateClassSection(id, request));
    }
}
