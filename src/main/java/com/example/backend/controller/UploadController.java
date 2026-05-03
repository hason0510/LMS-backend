package com.example.backend.controller;

import com.example.backend.constant.ResourceType;
import com.example.backend.constant.ResourceSource;
import com.example.backend.dto.response.CloudinaryResponse;
import com.example.backend.entity.Resource;
import com.example.backend.repository.ResourceRepository;
import com.example.backend.service.CloudinaryService;
import com.example.backend.utils.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/lms")
@RequiredArgsConstructor
public class UploadController {

    private final CloudinaryService cloudinaryService;
    private final ResourceRepository resourceRepository;

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
            @RequestPart MultipartFile file
    ) {
        FileUploadUtil.assertAllowed(file, "resource");
        ResourceType inferredType = FileUploadUtil.resolveResourceType(file.getOriginalFilename());
        String uploadType = switch (inferredType) {
            case VIDEO, AUDIO -> "video";
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
        Resource saved = resourceRepository.save(resource);

        response.setId(saved.getId());
        response.setResourceId(saved.getId());
        response.setTitle(saved.getTitle());
        response.setMimeType(saved.getMimeType());
        response.setFileSize(saved.getFileSize());
        response.setType(saved.getType() != null ? saved.getType().name() : response.getType());
        return ResponseEntity.ok(response);
    }

}
