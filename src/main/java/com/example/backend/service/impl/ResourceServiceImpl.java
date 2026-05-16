package com.example.backend.service.impl;

import com.example.backend.constant.ResourceScopeType;
import com.example.backend.constant.ResourceSource;
import com.example.backend.constant.ResourceStatus;
import com.example.backend.constant.ResourceType;
import com.example.backend.constant.ResourceVisibility;
import com.example.backend.constant.RoleType;
import com.example.backend.dto.request.ResourceRequest;
import com.example.backend.dto.request.ResourceSearchRequest;
import com.example.backend.dto.response.CloudinaryResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.ResourceAuditLogResponse;
import com.example.backend.dto.response.ResourceReferenceResponse;
import com.example.backend.dto.response.ResourceResponse;
import com.example.backend.dto.response.ResourceUploadPolicyResponse;
import com.example.backend.entity.Lesson;
import com.example.backend.entity.Resource;
import com.example.backend.entity.ResourceAuditLog;
import com.example.backend.entity.User;
import com.example.backend.entity.assignment.Assignment;
import com.example.backend.entity.assignment.Submission;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.AssignmentRepository;
import com.example.backend.repository.LessonRepository;
import com.example.backend.repository.ResourceAuditLogRepository;
import com.example.backend.repository.ResourceRepository;
import com.example.backend.repository.SubmissionRepository;
import com.example.backend.service.CloudinaryService;
import com.example.backend.service.ResourceAuthorizationService;
import com.example.backend.service.ResourceService;
import com.example.backend.service.UserService;
import com.example.backend.utils.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository resourceRepository;
    private final LessonRepository lessonRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final ResourceAuditLogRepository resourceAuditLogRepository;
    private final JdbcTemplate jdbcTemplate;
    private final CloudinaryService cloudinaryService;
    private final UserService userService;
    private final ResourceAuthorizationService resourceAuthorizationService;

    @Override
    public ResourceResponse getResourceById(Integer id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        resourceAuthorizationService.assertCanUse(resource, null, null);
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
        logResourceAction(newResource, "CREATE", "Tạo tài nguyên cho bài học");
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
        logResourceAction(newResource, "CREATE", "Tạo tài nguyên cho bài tập");
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
        logResourceAction(newResource, "CREATE", "Tạo tài nguyên cho bài nộp");
        return convertEntityToDTO(newResource);
    }

    @Override
    public ResourceResponse createStandaloneResource(ResourceRequest request) {
        Resource newResource = buildDetachedResource(request);
        resourceAuthorizationService.assertCanCreateInScope(newResource.getScopeType(), newResource.getScopeId());
        validateStandaloneResource(newResource);
        newResource.setLesson(null);
        newResource.setAssignment(null);
        newResource.setSubmission(null);
        resourceRepository.save(newResource);
        logResourceAction(newResource, "CREATE", "Tạo tài nguyên độc lập");
        return convertEntityToDTO(newResource);
    }

    @Override
    public ResourceResponse updateResource(Integer id, ResourceRequest request) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        resourceAuthorizationService.assertCanUse(resource, resource.getScopeType(), resource.getScopeId());

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
        if (request.getScopeType() != null) {
            resource.setScopeType(request.getScopeType());
        }
        if (request.getScopeId() != null) {
            resource.setScopeId(request.getScopeId());
        }
        if (request.getVisibility() != null) {
            resource.setVisibility(request.getVisibility());
        }
        ResourceStatus previousStatus = resource.getStatus();
        if (request.getStatus() != null) {
            resource.setStatus(request.getStatus());
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
        if (request.getStatus() != null && request.getStatus() != previousStatus) {
            logResourceAction(resource, request.getStatus() == ResourceStatus.ARCHIVED ? "ARCHIVE" : "RESTORE", "Đổi trạng thái tài nguyên");
        } else {
            logResourceAction(resource, "UPDATE", "Cập nhật thông tin tài nguyên");
        }
        return convertEntityToDTO(resource);
    }

    @Override
    public void deleteResource(Integer id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        resourceAuthorizationService.assertCanUse(resource, resource.getScopeType(), resource.getScopeId());
        if (resolveUsageCount(resource.getId()) > 0) {
            throw new BusinessException("Không thể xóa media đang được sử dụng. Hãy lưu trữ media thay vì xóa.");
        }
        logResourceAction(resource, "DELETE", "Xóa tài nguyên");
        resourceRepository.delete(resource);
    }

    @Override
    public PageResponse<ResourceResponse> getResourcePage(ResourceSearchRequest request, Pageable pageable) {
        Page<Resource> resourcePage = resourceRepository.findAll(buildResourceSpecification(request), pageable);
        Page<ResourceResponse> resourceResponsePage = resourcePage.map(this::convertEntityToDTO);
        return new PageResponse<>(
                resourceResponsePage.getNumber() + 1,
                resourceResponsePage.getTotalPages(),
                resourceResponsePage.getTotalElements(),
                resourceResponsePage.getContent()
        );
    }

    @Override
    public ResourceUploadPolicyResponse getUploadPolicy() {
        return new ResourceUploadPolicyResponse(
                FileUploadUtil.MAX_RESOURCE_SIZE,
                FileUploadUtil.getAllowedExtensions("resource"),
                Map.copyOf(FileUploadUtil.MAX_SIZE_BY_TYPE),
                Map.copyOf(FileUploadUtil.ALLOWED_EXTENSIONS_BY_TYPE)
        );
    }

    @Override
    public List<ResourceReferenceResponse> getResourceReferences(Integer resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        resourceAuthorizationService.assertCanUse(resource, null, null);

        String sql = """
                SELECT 'BANK_QUESTION' AS entity_type, id AS entity_id, 'question.resource' AS field_name, CONCAT('Câu hỏi ngân hàng #', id) AS label
                FROM bank_questions WHERE resource_id = ? AND is_deleted = false
                UNION ALL
                SELECT 'BANK_QUESTION_OPTION' AS entity_type, id AS entity_id, 'option.resource' AS field_name, CONCAT('Đáp án ngân hàng #', id) AS label
                FROM bank_question_options WHERE resource_id = ? AND is_deleted = false
                UNION ALL
                SELECT 'QUIZ_QUESTION' AS entity_type, id AS entity_id, 'question.resource' AS field_name, CONCAT('Câu hỏi quiz #', id) AS label
                FROM quiz_question WHERE resource_id = ? AND is_deleted = false
                UNION ALL
                SELECT 'QUIZ_ANSWER' AS entity_type, id AS entity_id, 'answer.resource' AS field_name, CONCAT('Đáp án quiz #', id) AS label
                FROM quiz_answer WHERE resource_id = ? AND is_deleted = false
                UNION ALL
                SELECT 'INTERACTION_ITEM' AS entity_type, id AS entity_id, 'interaction.resource' AS field_name, CONCAT('Tương tác #', id) AS label
                FROM question_interaction_items WHERE resource_id = ? AND is_deleted = false
                UNION ALL
                SELECT 'LESSON' AS entity_type, lesson_id AS entity_id, 'lesson.resources' AS field_name, CONCAT('Bài học #', lesson_id) AS label
                FROM resource WHERE id = ? AND lesson_id IS NOT NULL AND is_deleted = false
                UNION ALL
                SELECT 'ASSIGNMENT' AS entity_type, assignment_id AS entity_id, 'assignment.resources' AS field_name, CONCAT('Bài tập #', assignment_id) AS label
                FROM resource WHERE id = ? AND assignment_id IS NOT NULL AND is_deleted = false
                UNION ALL
                SELECT 'SUBMISSION' AS entity_type, submission_id AS entity_id, 'submission.resources' AS field_name, CONCAT('Bài nộp #', submission_id) AS label
                FROM resource WHERE id = ? AND submission_id IS NOT NULL AND is_deleted = false
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new ResourceReferenceResponse(
                rs.getString("entity_type"),
                rs.getInt("entity_id"),
                rs.getString("field_name"),
                rs.getString("label")
        ), resourceId, resourceId, resourceId, resourceId, resourceId, resourceId, resourceId, resourceId);
    }

    @Override
    public List<ResourceAuditLogResponse> getResourceAuditLogs(Integer resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        resourceAuthorizationService.assertCanUse(resource, null, null);
        return resourceAuditLogRepository.findTop20ByResource_IdOrderByIdDesc(resourceId).stream()
                .map(log -> new ResourceAuditLogResponse(
                        log.getId(),
                        log.getResource() != null ? log.getResource().getId() : null,
                        log.getActionType(),
                        log.getActorUsername(),
                        log.getSummary(),
                        log.getCreatedDate()
                ))
                .toList();
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
        response.setScopeType(resource.getScopeType());
        response.setScopeId(resource.getScopeId());
        response.setVisibility(resource.getVisibility());
        response.setStatus(resource.getStatus());
        Integer usageCount = resolveUsageCount(resource.getId());
        response.setUsageCount(usageCount);
        response.setLastUsedAt(resource.getLastUsedAt());
        response.setCreatedBy(resource.getCreatedBy());
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
        resource.setScopeType(request.getScopeType());
        resource.setScopeId(request.getScopeId());
        resource.setVisibility(request.getVisibility());
        resource.setStatus(request.getStatus());
        resource.setFileUrl(request.getFileUrl());
        resource.setEmbedUrl(request.getEmbedUrl());
        resource.setCloudinaryId(request.getCloudinaryId());
        resource.setMimeType(request.getMimeType());
        resource.setFileSize(request.getFileSize());
        normalizeResourcePayload(resource, true);
        return resource;
    }

    private Specification<Resource> buildResourceSpecification(ResourceSearchRequest request) {
        ResourceSearchRequest safeRequest = request != null ? request : new ResourceSearchRequest();
        User currentUser = userService.getCurrentUser();
        String currentUsername = currentUser != null ? currentUser.getUserName() : null;

        Specification<Resource> spec = (root, query, cb) -> cb.conjunction();
        if (!isAdmin(currentUser)) {
            spec = spec.and((root, query, cb) -> {
                var legacyResource = cb.isNull(root.get("createdBy"));
                var ownResource = cb.equal(root.get("createdBy"), currentUsername);
                var institutionResource = cb.equal(root.get("visibility"), ResourceVisibility.INSTITUTION);

                if (safeRequest.getScopeType() != null
                        && safeRequest.getScopeId() != null
                        && resourceAuthorizationService.canBrowseScope(safeRequest.getScopeType(), safeRequest.getScopeId())) {
                    var sharedInRequestedScope = cb.and(
                            cb.equal(root.get("scopeType"), safeRequest.getScopeType()),
                            cb.equal(root.get("scopeId"), safeRequest.getScopeId()),
                            cb.equal(root.get("visibility"), ResourceVisibility.SHARED)
                    );
                    return cb.or(legacyResource, ownResource, institutionResource, sharedInRequestedScope);
                }

                return cb.or(legacyResource, ownResource, institutionResource);
            });
        }
        if (Boolean.TRUE.equals(safeRequest.getCreatedByMe()) && StringUtils.hasText(currentUsername)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("createdBy"), currentUsername));
        }
        if (safeRequest.getScopeType() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("scopeType"), safeRequest.getScopeType()));
        }
        if (safeRequest.getScopeId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("scopeId"), safeRequest.getScopeId()));
        }
        if (safeRequest.getType() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), safeRequest.getType()));
        }
        if (safeRequest.getSource() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("source"), safeRequest.getSource()));
        }
        if (safeRequest.getStatus() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), safeRequest.getStatus()));
        } else {
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.isNull(root.get("status")),
                    cb.equal(root.get("status"), ResourceStatus.ACTIVE)
            ));
        }
        if (StringUtils.hasText(safeRequest.getSearch())) {
            String keyword = "%" + safeRequest.getSearch().trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), keyword),
                    cb.like(cb.lower(root.get("description")), keyword),
                    cb.like(cb.lower(root.get("mimeType")), keyword)
            ));
        }
        return spec;
    }

    private boolean isAdmin(User user) {
        return user != null
                && user.getRole() != null
                && user.getRole().getRoleName() == RoleType.ADMIN;
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
        logResourceAction(uploadResource, "UPLOAD", "Cập nhật tệp video");
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
        logResourceAction(uploadResource, "UPLOAD", "Cập nhật tệp tài liệu");
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
            case AUDIO -> "audio";
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
        logResourceAction(uploadResource, "UPLOAD", "Cập nhật tệp đính kèm");
        return response;
    }

    private Integer resolveUsageCount(Integer resourceId) {
        if (resourceId == null) {
            return 0;
        }
        String sql = """
                SELECT
                  (SELECT COUNT(*) FROM bank_questions WHERE resource_id = ? AND is_deleted = false) +
                  (SELECT COUNT(*) FROM bank_question_options WHERE resource_id = ? AND is_deleted = false) +
                  (SELECT COUNT(*) FROM quiz_question WHERE resource_id = ? AND is_deleted = false) +
                  (SELECT COUNT(*) FROM quiz_answer WHERE resource_id = ? AND is_deleted = false) +
                  (SELECT COUNT(*) FROM question_interaction_items WHERE resource_id = ? AND is_deleted = false) +
                  (SELECT COUNT(*) FROM resource WHERE id = ? AND lesson_id IS NOT NULL AND is_deleted = false) +
                  (SELECT COUNT(*) FROM resource WHERE id = ? AND assignment_id IS NOT NULL AND is_deleted = false) +
                  (SELECT COUNT(*) FROM resource WHERE id = ? AND submission_id IS NOT NULL AND is_deleted = false)
                """;
        Integer count = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                resourceId, resourceId, resourceId, resourceId, resourceId, resourceId, resourceId, resourceId
        );
        return count != null ? count : 0;
    }

    private void logResourceAction(Resource resource, String actionType, String summary) {
        if (resource == null || resource.getId() == null) {
            return;
        }
        User currentUser = userService.getCurrentUser();
        ResourceAuditLog log = new ResourceAuditLog();
        log.setResource(resource);
        log.setActionType(actionType);
        log.setActorUsername(currentUser != null ? currentUser.getUserName() : resource.getLastModifiedBy());
        log.setSummary(summary);
        resourceAuditLogRepository.save(log);
    }

    private void normalizeResourcePayload(Resource resource, boolean forCreate) {
        if (forCreate && resource.getStatus() == null) {
            resource.setStatus(ResourceStatus.ACTIVE);
        }
        if (forCreate && resource.getScopeType() == null) {
            resource.setScopeType(ResourceScopeType.PRIVATE_USER);
        }
        if (forCreate && resource.getVisibility() == null) {
            if (resource.getScopeType() == ResourceScopeType.INSTITUTION_SHARED) {
                resource.setVisibility(ResourceVisibility.INSTITUTION);
            } else if (resource.getScopeType() == ResourceScopeType.PRIVATE_USER) {
                resource.setVisibility(ResourceVisibility.PRIVATE);
            } else {
                resource.setVisibility(ResourceVisibility.SHARED);
            }
        }

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
