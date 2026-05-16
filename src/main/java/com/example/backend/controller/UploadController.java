package com.example.backend.controller;

import com.example.backend.constant.ResourceScopeType;
import com.example.backend.constant.ResourceType;
import com.example.backend.constant.ResourceSource;
import com.example.backend.constant.ResourceStatus;
import com.example.backend.constant.ResourceVisibility;
import com.example.backend.dto.response.CloudinaryResponse;
import com.example.backend.entity.Resource;
import com.example.backend.entity.ResourceAuditLog;
import com.example.backend.entity.User;
import com.example.backend.repository.ResourceAuditLogRepository;
import com.example.backend.repository.ResourceRepository;
import com.example.backend.service.CloudinaryService;
import com.example.backend.service.ResourceAuthorizationService;
import com.example.backend.service.UserService;
import com.example.backend.utils.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/lms")
@RequiredArgsConstructor
public class UploadController {

    private final CloudinaryService cloudinaryService;
    private final ResourceRepository resourceRepository;
    private final ResourceAuditLogRepository resourceAuditLogRepository;
    private final ResourceAuthorizationService resourceAuthorizationService;
    private final UserService userService;

    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @PostMapping("/upload/image")
    public ResponseEntity<CloudinaryResponse> uploadImage(
            @RequestPart MultipartFile file
    ) {
        CloudinaryResponse response =
                cloudinaryService.uploadEditorImage(file);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/upload/resource")
    public ResponseEntity<CloudinaryResponse> uploadResource(
            @RequestPart MultipartFile file,
            @RequestParam(value = "scopeType", required = false) ResourceScopeType scopeType,
            @RequestParam(value = "scopeId", required = false) Integer scopeId
    ) {
        resourceAuthorizationService.assertCanCreateInScope(scopeType, scopeId);
        FileUploadUtil.assertAllowed(file, "resource");
        ResourceType inferredType = FileUploadUtil.resolveResourceType(file.getOriginalFilename());
        String uploadType = switch (inferredType) {
            case VIDEO -> "video";
            case AUDIO -> "audio";
            case IMAGE, PDF -> "image";
            default -> "raw";
        };
        String fileName = FileUploadUtil.getFileName(file.getOriginalFilename());
        if ("raw".equals(uploadType)) {
            String ext = FilenameUtils.getExtension(file.getOriginalFilename());
            if (ext != null && !ext.isEmpty()) {
                fileName = fileName + "." + ext.toLowerCase();
            }
        }
        CloudinaryResponse response = cloudinaryService.uploadFile(file, fileName, uploadType);

        Resource resource = new Resource();
        resource.setTitle(file.getOriginalFilename());
        resource.setFileUrl(response.getUrl());
        resource.setCloudinaryId(response.getPublicId());
        resource.setHlsUrl(response.getHlsUrl());
        resource.setMimeType(file.getContentType());
        resource.setFileSize(file.getSize());
        resource.setType(inferredType);
        resource.setSource(ResourceSource.UPLOAD);
        resource.setScopeType(scopeType != null ? scopeType : ResourceScopeType.PRIVATE_USER);
        resource.setScopeId(scopeId);
        resource.setVisibility(resolveVisibility(resource.getScopeType()));
        resource.setStatus(ResourceStatus.ACTIVE);
        Resource saved = resourceRepository.save(resource);
        ResourceAuditLog log = new ResourceAuditLog();
        User currentUser = userService.getCurrentUser();
        log.setResource(saved);
        log.setActionType("UPLOAD");
        log.setActorUsername(currentUser != null ? currentUser.getUserName() : saved.getCreatedBy());
        log.setSummary("Tải media lên thư viện");
        resourceAuditLogRepository.save(log);

        response.setId(saved.getId());
        response.setResourceId(saved.getId());
        response.setTitle(saved.getTitle());
        response.setMimeType(saved.getMimeType());
        response.setFileSize(saved.getFileSize());
        response.setType(saved.getType() != null ? saved.getType().name() : response.getType());
        return ResponseEntity.ok(response);
    }

    private ResourceVisibility resolveVisibility(ResourceScopeType scopeType) {
        if (scopeType == ResourceScopeType.INSTITUTION_SHARED) {
            return ResourceVisibility.INSTITUTION;
        }
        if (scopeType == null || scopeType == ResourceScopeType.PRIVATE_USER) {
            return ResourceVisibility.PRIVATE;
        }
        return ResourceVisibility.SHARED;
    }

}
