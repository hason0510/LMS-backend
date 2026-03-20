package com.example.backend.service.impl;

import com.example.backend.constant.ResourceSource;
import com.example.backend.constant.ResourceType;
import com.example.backend.dto.request.ResourceRequest;
import com.example.backend.dto.response.CloudinaryResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.ResourceResponse;
import com.example.backend.entity.Lesson;
import com.example.backend.entity.Resource;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.LessonRepository;
import com.example.backend.repository.ResourceRepository;
import com.example.backend.service.CloudinaryService;
import com.example.backend.service.ResourceService;
import com.example.backend.utils.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository resourceRepository;
    private final LessonRepository lessonRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    public ResourceResponse getResourceById(Integer id) {
        Resource resource = resourceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        return convertEntityToDTO(resource);
    }

    @Override
    public ResourceResponse createResource(Integer lessonId, ResourceRequest request) {
        Resource newResource = new Resource();
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));
        newResource.setLesson(lesson);
        newResource.setDescription(request.getDescription());
        newResource.setEmbedUrl(request.getEmbedUrl());
        newResource.setTitle(request.getTitle());
        newResource.setType(request.getType());
        newResource.setSource(request.getSource());
        // ===== EMBED =====
        if (request.getSource() == ResourceSource.EMBED) {
            if (!StringUtils.hasText(request.getEmbedUrl())) {
                throw new IllegalArgumentException("Embed URL is required");
            }
            newResource.setEmbedUrl(request.getEmbedUrl());
            newResource.setFileUrl(null);
            newResource.setCloudinaryId(null);
        }
        // ===== UPLOAD =====
        if (request.getSource() == ResourceSource.UPLOAD) {
            newResource.setEmbedUrl(null);
        }
        resourceRepository.save(newResource);
        return convertEntityToDTO(newResource);
    }

    @Override
    public ResourceResponse updateResource(Integer id, ResourceRequest request) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        if (request.getTitle() != null) {
            resource.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            resource.setDescription(request.getDescription());
        }
        if (request.getSource() == ResourceSource.EMBED) {
            resource.setEmbedUrl(request.getEmbedUrl());
            resource.setFileUrl(null);
            resource.setCloudinaryId(null);
        }
        resourceRepository.save(resource);
        return convertEntityToDTO(resource);
    }

    @Override
    public void deleteResource(Integer id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        resourceRepository.delete(resource);
    }

    @Override
    public PageResponse<ResourceResponse> getResourcePage(Pageable pageable) {
        Page<Resource> resourcePage = resourceRepository.findAll(pageable);
        Page<ResourceResponse> resourceResponsePage = resourcePage.map(this::convertEntityToDTO);
        PageResponse<ResourceResponse> response = new PageResponse<>(
                resourceResponsePage.getNumber() + 1,
                resourceResponsePage.getTotalPages(),
                resourceResponsePage.getNumberOfElements(),
                resourceResponsePage.getContent()
        );
        return response;
    }

    @Override
    public List<ResourceResponse> getResourcesByLessonId(Integer lessonId) {
        List<Resource> resources = resourceRepository.findByLesson_Id(lessonId);
        return resources.stream().map(this::convertEntityToDTO).collect(Collectors.toList());
    }

    @Override
    public ResourceResponse convertEntityToDTO(Resource resource) {
        ResourceResponse response = new ResourceResponse();
        response.setId(resource.getId());
        response.setTitle(resource.getTitle());
        response.setType(resource.getType());
        response.setEmbedUrl(resource.getEmbedUrl());
        response.setCloudinaryId(resource.getCloudinaryId());
        response.setFileUrl(resource.getFileUrl());
        response.setLessonId(resource.getLesson().getId());
        response.setLessonTitle(resource.getLesson().getTitle());
        response.setCloudinaryId(resource.getCloudinaryId());
        return response;
    }

    @Override
    public CloudinaryResponse uploadVideoResource(Integer id, MultipartFile file) {
        final Resource uploadResource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (uploadResource.getType() != ResourceType.VIDEO) {
            throw new IllegalStateException("Resource is not VIDEO");
        }
        if (uploadResource.getSource() != ResourceSource.UPLOAD) {
            throw new IllegalStateException("Resource is not UPLOAD type");
        }

        FileUploadUtil.assertAllowed(file, "video");
        final String cloudinaryId = uploadResource.getCloudinaryId();
        if(StringUtils.hasText(cloudinaryId)) {
            cloudinaryService.deleteFile(cloudinaryId, ResourceType.VIDEO);
        }
        final String fileName = FileUploadUtil.getFileName(file.getOriginalFilename());
        final CloudinaryResponse response = this.cloudinaryService.uploadFile(file, fileName, "video");
        uploadResource.setFileUrl(response.getUrl());
        uploadResource.setCloudinaryId(response.getPublicId());
        resourceRepository.save(uploadResource);
        return response;
    }

    @Override
    public CloudinaryResponse uploadSlideResource(Integer id, MultipartFile file) {
        final Resource uploadResource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (uploadResource.getType() != ResourceType.PDF) {
            throw new IllegalStateException("Resource is not PDF type");
        }
        // if (uploadResource.getSource() != ResourceSource.UPLOAD) {
        //     throw new IllegalStateException("Resource is not UPLOAD type");
        // }

        FileUploadUtil.assertAllowed(file, "pdf");
        final String cloudinaryId = uploadResource.getCloudinaryId();
        if(StringUtils.hasText(cloudinaryId)) {
            cloudinaryService.deleteFile(cloudinaryId, ResourceType.PDF);
        }
        final String fileName = FileUploadUtil.getFileName(file.getOriginalFilename());
        final CloudinaryResponse response = this.cloudinaryService.uploadFile(file, fileName, "pdf");
        uploadResource.setFileUrl(response.getUrl());
        uploadResource.setCloudinaryId(response.getPublicId());
        resourceRepository.save(uploadResource);
        return response;
    }


}
