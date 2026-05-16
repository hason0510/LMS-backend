package com.example.backend.controller;

import com.example.backend.constant.ResourceScopeType;
import com.example.backend.constant.ResourceSource;
import com.example.backend.constant.ResourceStatus;
import com.example.backend.constant.ResourceType;
import com.example.backend.dto.request.ResourceRequest;
import com.example.backend.dto.request.ResourceSearchRequest;
import com.example.backend.dto.response.CloudinaryResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.ResourceAuditLogResponse;
import com.example.backend.dto.response.ResourceReferenceResponse;
import com.example.backend.dto.response.ResourceResponse;
import com.example.backend.dto.response.ResourceUploadPolicyResponse;
import com.example.backend.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lms")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @Operation(summary = "Create resource for lesson")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/lessons/{lessonId}/resources")
    public ResponseEntity<ResourceResponse> createResource(
            @PathVariable Integer lessonId,
            @RequestBody ResourceRequest request
    ) {
        ResourceResponse response = resourceService.createResource(lessonId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Create resource for assignment")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/assignments/{assignmentId}/resources")
    public ResponseEntity<ResourceResponse> createAssignmentResource(
            @PathVariable Integer assignmentId,
            @RequestBody ResourceRequest request
    ) {
        ResourceResponse response = resourceService.createAssignmentResource(assignmentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Create resource for submission")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    @PostMapping("/submissions/{submissionId}/resources")
    public ResponseEntity<ResourceResponse> createSubmissionResource(
            @PathVariable Integer submissionId,
            @RequestBody ResourceRequest request
    ) {
        ResourceResponse response = resourceService.createSubmissionResource(submissionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Create standalone resource")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/resources/standalone")
    public ResponseEntity<ResourceResponse> createStandaloneResource(
            @RequestBody ResourceRequest request
    ) {
        ResourceResponse response = resourceService.createStandaloneResource(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Update resource")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PutMapping("/resources/{id}")
    public ResponseEntity<ResourceResponse> updateResource(
            @PathVariable Integer id,
            @RequestBody ResourceRequest request
    ) {
        ResourceResponse response = resourceService.updateResource(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete resource")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @DeleteMapping("/resources/{id}")
    public ResponseEntity<Void> deleteResource(@PathVariable Integer id) {
        resourceService.deleteResource(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get resource by id")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/resources/{id}")
    public ResponseEntity<ResourceResponse> getResourceById(@PathVariable Integer id) {
        ResourceResponse response = resourceService.getResourceById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get resource references")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/resources/{id}/references")
    public ResponseEntity<List<ResourceReferenceResponse>> getResourceReferences(@PathVariable Integer id) {
        return ResponseEntity.ok(resourceService.getResourceReferences(id));
    }

    @Operation(summary = "Get resource audit log")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/resources/{id}/audit-log")
    public ResponseEntity<List<ResourceAuditLogResponse>> getResourceAuditLogs(@PathVariable Integer id) {
        return ResponseEntity.ok(resourceService.getResourceAuditLogs(id));
    }

    @Operation(summary = "Get paged resources")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/resources")
    public ResponseEntity<PageResponse<ResourceResponse>> getAllResources(
            @RequestParam(value = "pageNumber", defaultValue = "1") Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "5") Integer pageSize,
            @RequestParam(value = "scopeType", required = false) ResourceScopeType scopeType,
            @RequestParam(value = "scopeId", required = false) Integer scopeId,
            @RequestParam(value = "type", required = false) ResourceType type,
            @RequestParam(value = "source", required = false) ResourceSource source,
            @RequestParam(value = "status", required = false) ResourceStatus status,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "createdByMe", required = false) Boolean createdByMe,
            @RequestParam(value = "recent", required = false) Boolean recent,
            @RequestParam(value = "sortBy", defaultValue = "date") String sortBy
    ) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, resolveResourceSort(sortBy));
        ResourceSearchRequest request = new ResourceSearchRequest();
        request.setScopeType(scopeType);
        request.setScopeId(scopeId);
        request.setType(type);
        request.setSource(source);
        request.setStatus(status);
        request.setSearch(search);
        request.setCreatedByMe(createdByMe);
        request.setRecent(recent);
        request.setSortBy(sortBy);
        PageResponse<ResourceResponse> response = resourceService.getResourcePage(request, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get resource upload policy")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/resources/upload-policy")
    public ResponseEntity<ResourceUploadPolicyResponse> getUploadPolicy() {
        return ResponseEntity.ok(resourceService.getUploadPolicy());
    }

    @Operation(summary = "Get resources by lesson")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/lessons/{lessonId}/resources")
    public ResponseEntity<List<ResourceResponse>> getResourcesByLesson(
            @PathVariable Integer lessonId
    ) {
        return ResponseEntity.ok(resourceService.getResourcesByLessonId(lessonId));
    }

    @Operation(summary = "Get resources by assignment")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/assignments/{assignmentId}/resources")
    public ResponseEntity<List<ResourceResponse>> getResourcesByAssignment(
            @PathVariable Integer assignmentId
    ) {
        return ResponseEntity.ok(resourceService.getResourcesByAssignmentId(assignmentId));
    }

    @Operation(summary = "Get resources by submission")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/submissions/{submissionId}/resources")
    public ResponseEntity<List<ResourceResponse>> getResourcesBySubmission(
            @PathVariable Integer submissionId
    ) {
        return ResponseEntity.ok(resourceService.getResourcesBySubmissionId(submissionId));
    }

    @Operation(summary = "Upload video for resource")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/resources/{id}/video")
    public ResponseEntity<CloudinaryResponse> uploadVideo(
            @PathVariable Integer id,
            @RequestPart MultipartFile file
    ) {
        CloudinaryResponse response = resourceService.uploadVideoResource(id, file);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Upload slide for resource")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/resources/{id}/slide")
    public ResponseEntity<CloudinaryResponse> uploadSlide(
            @PathVariable Integer id,
            @RequestPart MultipartFile file
    ) {
        CloudinaryResponse response = resourceService.uploadSlideResource(id, file);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Upload file attachment for resource")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    @PostMapping("/resources/{id}/file")
    public ResponseEntity<CloudinaryResponse> uploadAttachment(
            @PathVariable Integer id,
            @RequestPart MultipartFile file
    ) {
        CloudinaryResponse response = resourceService.uploadAttachmentResource(id, file);
        return ResponseEntity.ok(response);
    }

    private Sort resolveResourceSort(String sortBy) {
        String normalized = sortBy == null ? "date" : sortBy.trim().toLowerCase();
        return switch (normalized) {
            case "name" -> Sort.by(Sort.Direction.ASC, "title");
            case "size" -> Sort.by(Sort.Direction.DESC, "fileSize");
            default -> Sort.by(Sort.Direction.DESC, "id");
        };
    }
}
