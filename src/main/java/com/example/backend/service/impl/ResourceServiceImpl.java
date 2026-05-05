package com.example.backend.service.impl;

import com.example.backend.constant.ResourceSource;
import com.example.backend.constant.ResourceType;
import com.example.backend.dto.request.ResourceRequest;
import com.example.backend.dto.response.CloudinaryResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.ResourceResponse;
import com.example.backend.entity.Lesson;
import com.example.backend.entity.Resource;
import com.example.backend.entity.assignment.Assignment;
import com.example.backend.entity.assignment.Submission;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.AssignmentRepository;
import com.example.backend.repository.LessonRepository;
import com.example.backend.repository.ResourceRepository;
import com.example.backend.repository.SubmissionRepository;
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
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    public ResourceResponse getResourceById(Integer id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        return convertEntityToDTO(resource);
    }

    @Override
    public ResourceResponse createResource(Integer lessonId, ResourceRequest request) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));

        Resource newResource = buildDetachedResource(request);
        newResource.setLesson(lesson);
        newResource.setAssignment(null);
        newResource.setSubmission(null);

        resourceRepository.save(newResource);
        return convertEntityToDTO(newResource);
    }

    @Override
    public ResourceResponse createAssignmentResource(Integer assignmentId, ResourceRequest request) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));

        Resource newResource = buildDetachedResource(request);
        newResource.setAssignment(assignment);
        newResource.setLesson(null);
        newResource.setSubmission(null);

        resourceRepository.save(newResource);
        return convertEntityToDTO(newResource);
    }

    @Override
    public ResourceResponse createSubmissionResource(Integer submissionId, ResourceRequest request) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));

        Resource newResource = buildDetachedResource(request);
        newResource.setSubmission(submission);
        newResource.setLesson(null);
        newResource.setAssignment(null);

        resourceRepository.save(newResource);
        return convertEntityToDTO(newResource);
    }

    @Override
    public ResourceResponse createStandaloneResource(ResourceRequest request) {
        Resource newResource = buildDetachedResource(request);
        validateStandaloneResource(newResource);
        newResource.setLesson(null);
        newResource.setAssignment(null);
        newResource.setSubmission(null);
        resourceRepository.save(newResource);
        return convertEntityToDTO(newResource);
    }

    @Override
    public ResourceResponse updateResource(Integer id, ResourceRequest request) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        if (request.getTitle() != null) {
            resource.setTitle(normalizeTitle(request.getTitle()));
        }
        if (request.getDescription() != null) {
            resource.setDescription(request.getDescription());
        }
        if (request.getType() != null) {
            resource.setType(request.getType());
        }
        if (request.getSource() != null) {
            resource.setSource(request.getSource());
        }
        if (request.getFileUrl() != null) {
            resource.setFileUrl(request.getFileUrl());
        }
        if (request.getEmbedUrl() != null) {
            resource.setEmbedUrl(request.getEmbedUrl());
        }
        if (request.getCloudinaryId() != null) {
            resource.setCloudinaryId(request.getCloudinaryId());
        }
        if (request.getMimeType() != null) {
            resource.setMimeType(request.getMimeType());
        }
        if (request.getFileSize() != null) {
            resource.setFileSize(request.getFileSize());
        }

        normalizeResourcePayload(resource, false);
        resourceRepository.save(resource);
        return convertEntityToDTO(resource);
    }

    @Override
    public void deleteResource(Integer id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        resourceRepository.delete(resource);
    }

    @Override
    public PageResponse<ResourceResponse> getResourcePage(Pageable pageable) {
        Page<Resource> resourcePage = resourceRepository.findAll(pageable);
        Page<ResourceResponse> resourceResponsePage = resourcePage.map(this::convertEntityToDTO);
        return new PageResponse<>(
                resourceResponsePage.getNumber() + 1,
                resourceResponsePage.getTotalPages(),
                resourceResponsePage.getNumberOfElements(),
                resourceResponsePage.getContent()
        );
    }

    @Override
    public List<ResourceResponse> getResourcesByLessonId(Integer lessonId) {
        return resourceRepository.findByLesson_Id(lessonId)
                .stream()
                .map(this::convertEntityToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ResourceResponse> getResourcesByAssignmentId(Integer assignmentId) {
        return resourceRepository.findByAssignment_Id(assignmentId)
                .stream()
                .map(this::convertEntityToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ResourceResponse> getResourcesBySubmissionId(Integer submissionId) {
        return resourceRepository.findBySubmission_Id(submissionId)
                .stream()
                .map(this::convertEntityToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ResourceResponse convertEntityToDTO(Resource resource) {
        ResourceResponse response = new ResourceResponse();
        response.setId(resource.getId());
        response.setTitle(resource.getTitle());
        response.setDescription(resource.getDescription());
        response.setType(resource.getType());
        response.setSource(resource.getSource());
        response.setEmbedUrl(resource.getEmbedUrl());
        response.setCloudinaryId(resource.getCloudinaryId());
        response.setFileUrl(resource.getFileUrl());
        response.setHlsUrl(resource.getHlsUrl());
        response.setMimeType(resource.getMimeType());
        response.setFileSize(resource.getFileSize());
        response.setLessonId(resource.getLesson() != null ? resource.getLesson().getId() : null);
        response.setLessonTitle(resource.getLesson() != null ? resource.getLesson().getTitle() : null);
        response.setAssignmentId(resource.getAssignment() != null ? resource.getAssignment().getId() : null);
        response.setSubmissionId(resource.getSubmission() != null ? resource.getSubmission().getId() : null);
        return response;
    }

    @Override
    public Resource buildDetachedResource(ResourceRequest request) {
        Resource resource = new Resource();
        resource.setTitle(normalizeTitle(request.getTitle()));
        resource.setDescription(request.getDescription());
        resource.setType(request.getType());
        resource.setSource(request.getSource());
        resource.setFileUrl(request.getFileUrl());
        resource.setEmbedUrl(request.getEmbedUrl());
        resource.setCloudinaryId(request.getCloudinaryId());
        resource.setMimeType(request.getMimeType());
        resource.setFileSize(request.getFileSize());
        normalizeResourcePayload(resource, true);
        return resource;
    }

    @Override
    public CloudinaryResponse uploadVideoResource(Integer id, MultipartFile file) {
        final Resource uploadResource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        if (uploadResource.getType() != ResourceType.VIDEO) {
            throw new IllegalStateException("Resource is not VIDEO");
        }
        if (uploadResource.getSource() != ResourceSource.UPLOAD) {
            throw new IllegalStateException("Resource is not UPLOAD type");
        }

        FileUploadUtil.assertAllowed(file, "video");
        final String cloudinaryId = uploadResource.getCloudinaryId();
        if (StringUtils.hasText(cloudinaryId)) {
            cloudinaryService.deleteFile(cloudinaryId, ResourceType.VIDEO);
        }
        final String fileName = FileUploadUtil.getFileName(file.getOriginalFilename());
        final CloudinaryResponse response = this.cloudinaryService.uploadFile(file, fileName, "video");
        uploadResource.setFileUrl(response.getUrl());
        uploadResource.setCloudinaryId(response.getPublicId());
        uploadResource.setHlsUrl(response.getHlsUrl());
        uploadResource.setMimeType(file.getContentType());
        uploadResource.setFileSize(file.getSize());
        resourceRepository.save(uploadResource);
        return response;
    }

    @Override
    public CloudinaryResponse uploadSlideResource(Integer id, MultipartFile file) {
        final Resource uploadResource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        if (uploadResource.getType() != ResourceType.PDF) {
            throw new IllegalStateException("Resource is not PDF type");
        }
        if (uploadResource.getSource() != ResourceSource.UPLOAD) {
            throw new IllegalStateException("Resource is not UPLOAD type");
        }

        FileUploadUtil.assertAllowed(file, "pdf");
        final String cloudinaryId = uploadResource.getCloudinaryId();
        if (StringUtils.hasText(cloudinaryId)) {
            cloudinaryService.deleteFile(cloudinaryId, ResourceType.PDF);
        }
        final String fileName = FileUploadUtil.getFileName(file.getOriginalFilename());
        final CloudinaryResponse response = this.cloudinaryService.uploadFile(file, fileName, "raw");
        uploadResource.setFileUrl(response.getUrl());
        uploadResource.setCloudinaryId(response.getPublicId());
        uploadResource.setMimeType(file.getContentType());
        uploadResource.setFileSize(file.getSize());
        resourceRepository.save(uploadResource);
        return response;
    }

    @Override
    public CloudinaryResponse uploadAttachmentResource(Integer id, MultipartFile file) {
        Resource uploadResource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        if (uploadResource.getSource() != ResourceSource.UPLOAD) {
            throw new BusinessException("Only upload-type resources can receive files");
        }

        FileUploadUtil.assertAllowed(file, "resource");
        ResourceType inferredType = FileUploadUtil.resolveResourceType(file.getOriginalFilename());
        String uploadType = switch (inferredType) {
            case VIDEO -> "video";
            case IMAGE -> "image";
            default -> "raw";
        };

        if (StringUtils.hasText(uploadResource.getCloudinaryId())) {
            cloudinaryService.deleteFile(
                    uploadResource.getCloudinaryId(),
                    uploadResource.getType() != null ? uploadResource.getType() : inferredType
            );
        }

        String fileName = FileUploadUtil.getFileName(file.getOriginalFilename());
        CloudinaryResponse response = cloudinaryService.uploadFile(file, fileName, uploadType);

        uploadResource.setFileUrl(response.getUrl());
        uploadResource.setCloudinaryId(response.getPublicId());
        uploadResource.setType(inferredType);
        uploadResource.setMimeType(file.getContentType());
        uploadResource.setFileSize(file.getSize());
        if (!StringUtils.hasText(uploadResource.getTitle())) {
            uploadResource.setTitle(file.getOriginalFilename());
        }
        resourceRepository.save(uploadResource);
        return response;
    }

    private void normalizeResourcePayload(Resource resource, boolean forCreate) {
        ResourceSource source = resource.getSource();
        if (source == null) {
            source = StringUtils.hasText(resource.getEmbedUrl()) ? ResourceSource.EMBED : ResourceSource.UPLOAD;
            resource.setSource(source);
        }

        if (source == ResourceSource.EMBED) {
            if (!StringUtils.hasText(resource.getEmbedUrl())) {
                throw new BusinessException("Embed URL is required for EMBED resource");
            }
            resource.setFileUrl(null);
            resource.setCloudinaryId(null);
            if (resource.getType() == null) {
                resource.setType(ResourceType.LINK);
            }
            if (!StringUtils.hasText(resource.getTitle())) {
                resource.setTitle(buildDefaultTitle(resource.getEmbedUrl()));
            }
            return;
        }

        resource.setEmbedUrl(null);
        if (resource.getType() == null) {
            resource.setType(ResourceType.FILE);
        }
        if (!StringUtils.hasText(resource.getTitle())) {
            if (StringUtils.hasText(resource.getFileUrl())) {
                resource.setTitle(buildDefaultTitle(resource.getFileUrl()));
            } else if (!forCreate) {
                resource.setTitle("Resource");
            }
        }
    }

    private String normalizeTitle(String title) {
        return StringUtils.hasText(title) ? title.trim() : null;
    }

    private String buildDefaultTitle(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return "Resource";
        }
        String normalized = rawValue.trim();
        int slashIndex = normalized.lastIndexOf('/');
        String candidate = slashIndex >= 0 ? normalized.substring(slashIndex + 1) : normalized;
        if (!StringUtils.hasText(candidate)) {
            return "Resource";
        }
        return candidate.length() > 120 ? candidate.substring(0, 120) : candidate;
    }

    private void validateStandaloneResource(Resource resource) {
        if (resource.getSource() == ResourceSource.EMBED && !StringUtils.hasText(resource.getEmbedUrl())) {
            throw new BusinessException("Embed URL is required");
        }

        if (resource.getSource() == ResourceSource.UPLOAD && !StringUtils.hasText(resource.getFileUrl())) {
            throw new BusinessException("File URL is required");
        }
    }
}
