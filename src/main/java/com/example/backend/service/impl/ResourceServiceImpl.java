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
import com.example.backend.entity.ResourceReference;
import com.example.backend.entity.User;
import com.example.backend.entity.assignment.Assignment;
import com.example.backend.entity.assignment.Submission;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.AssignmentRepository;
import com.example.backend.repository.LessonRepository;
import com.example.backend.repository.ResourceAuditLogRepository;
import com.example.backend.repository.ResourceReferenceRepository;
import com.example.backend.repository.ResourceRepository;
import com.example.backend.repository.SubmissionRepository;
import com.example.backend.service.CloudinaryService;
import com.example.backend.service.ResourceAuthorizationService;
import com.example.backend.service.ResourceService;
import com.example.backend.service.UserService;
import com.example.backend.specification.ResourceSpecification;
import com.example.backend.utils.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {
    private static final String ATTACHED_FIELD_NAME = "attached.resource";
    private static final String ENTITY_LESSON = "LESSON";
    private static final String ENTITY_ASSIGNMENT = "ASSIGNMENT";
    private static final String ENTITY_SUBMISSION = "SUBMISSION";

    private final ResourceRepository resourceRepository;
    private final LessonRepository lessonRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final ResourceAuditLogRepository resourceAuditLogRepository;
    private final ResourceReferenceRepository resourceReferenceRepository;
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
        resourceAuthorizationService.assertCanManage(resource);

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
        resourceAuthorizationService.assertCanDelete(resource);
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
    @Transactional
    public List<ResourceReferenceResponse> getResourceReferences(Integer resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        resourceAuthorizationService.assertCanUse(resource, null, null);
        syncResourceReferences(resource);
        return loadEnrichedResourceReferences(resource.getId());
    }

    private void syncResourceReferences(Resource resource) {
        List<ResourceReferenceSeed> seeds = loadCurrentReferenceSeeds(resource.getId());
        List<ResourceReference> activeReferences = resourceReferenceRepository.findByResource_IdOrderByIdDesc(resource.getId());

        if (hasSameReferenceSet(activeReferences, seeds)) {
            return;
        }

        resourceReferenceRepository.softDeleteAllActiveByResourceId(resource.getId());
        if (seeds.isEmpty()) {
            return;
        }

        List<ResourceReference> references = new ArrayList<>();
        for (ResourceReferenceSeed seed : seeds) {
            ResourceReference reference = new ResourceReference();
            reference.setResource(resource);
            reference.setEntityType(seed.entityType());
            reference.setEntityId(seed.entityId());
            reference.setFieldName(seed.fieldName());
            references.add(reference);
        }
        resourceReferenceRepository.saveAll(references);
    }

    private boolean hasSameReferenceSet(List<ResourceReference> current, List<ResourceReferenceSeed> target) {
        if (current.size() != target.size()) {
            return false;
        }

        List<String> currentKeys = current.stream()
                .map(item -> buildReferenceKey(item.getEntityType(), item.getEntityId(), item.getFieldName()))
                .sorted()
                .toList();
        List<String> targetKeys = target.stream()
                .map(item -> buildReferenceKey(item.entityType(), item.entityId(), item.fieldName()))
                .sorted()
                .toList();
        return currentKeys.equals(targetKeys);
    }

    private String buildReferenceKey(String entityType, Integer entityId, String fieldName) {
        return (entityType != null ? entityType : "") + "|" + (entityId != null ? entityId : -1) + "|" + (fieldName != null ? fieldName : "");
    }

    private List<ResourceReferenceSeed> loadCurrentReferenceSeeds(Integer resourceId) {
        String sql = """
                SELECT 'BANK_QUESTION' AS entity_type, id AS entity_id, 'question.resource' AS field_name
                FROM bank_questions WHERE resource_id = ? AND is_deleted = false
                UNION ALL
                SELECT 'BANK_QUESTION_OPTION' AS entity_type, id AS entity_id, 'option.resource' AS field_name
                FROM bank_question_options WHERE resource_id = ? AND is_deleted = false
                UNION ALL
                SELECT 'QUIZ_QUESTION' AS entity_type, id AS entity_id, 'question.resource' AS field_name
                FROM quiz_question WHERE resource_id = ? AND is_deleted = false
                UNION ALL
                SELECT 'QUIZ_ANSWER' AS entity_type, id AS entity_id, 'answer.resource' AS field_name
                FROM quiz_answer WHERE resource_id = ? AND is_deleted = false
                UNION ALL
                SELECT 'INTERACTION_ITEM' AS entity_type, id AS entity_id, 'interaction.resource' AS field_name
                FROM question_interaction_items WHERE resource_id = ? AND is_deleted = false
                UNION ALL
                SELECT 'LESSON' AS entity_type, lesson_id AS entity_id, 'lesson.resources' AS field_name
                FROM resource WHERE id = ? AND lesson_id IS NOT NULL AND is_deleted = false
                UNION ALL
                SELECT 'ASSIGNMENT' AS entity_type, assignment_id AS entity_id, 'assignment.resources' AS field_name
                FROM resource WHERE id = ? AND assignment_id IS NOT NULL AND is_deleted = false
                UNION ALL
                SELECT 'SUBMISSION' AS entity_type, submission_id AS entity_id, 'submission.resources' AS field_name
                FROM resource WHERE id = ? AND submission_id IS NOT NULL AND is_deleted = false
                UNION ALL
                SELECT entity_type, entity_id, field_name
                FROM resource_references
                WHERE resource_id = ?
                  AND is_deleted = false
                  AND field_name = 'attached.resource'
                  AND entity_type IN ('LESSON', 'ASSIGNMENT', 'SUBMISSION')
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new ResourceReferenceSeed(
                rs.getString("entity_type"),
                rs.getInt("entity_id"),
                rs.getString("field_name")
        ), resourceId, resourceId, resourceId, resourceId, resourceId, resourceId, resourceId, resourceId, resourceId);
    }

    private List<ResourceReferenceResponse> loadEnrichedResourceReferences(Integer resourceId) {
        String sql = """
                SELECT
                    rr.entity_type,
                    rr.entity_id,
                    rr.field_name,
                    CONCAT('Câu hỏi ngân hàng #', bq.id) AS label,
                    CONCAT('Môn: ', COALESCE(s.title, '-'), ' -> Ngân hàng: ', COALESCE(qb.name, '-')) AS context_path,
                    NULL AS class_section_id,
                    NULL AS class_section_title,
                    NULL AS quiz_id,
                    NULL AS quiz_title,
                    qb.id AS question_bank_id,
                    qb.name AS question_bank_name,
                    s.id AS subject_id,
                    s.title AS subject_title
                FROM resource_references rr
                JOIN bank_questions bq ON rr.entity_type = 'BANK_QUESTION' AND rr.entity_id = bq.id AND bq.is_deleted = false
                JOIN question_banks qb ON qb.id = bq.question_bank_id AND qb.is_deleted = false
                LEFT JOIN subjects s ON s.id = qb.subject_id AND s.is_deleted = false
                WHERE rr.resource_id = ? AND rr.is_deleted = false
                UNION ALL
                SELECT
                    rr.entity_type,
                    rr.entity_id,
                    rr.field_name,
                    CONCAT('Đáp án ngân hàng #', bqo.id) AS label,
                    CONCAT('Môn: ', COALESCE(s.title, '-'), ' -> Ngân hàng: ', COALESCE(qb.name, '-'), ' -> Câu hỏi #', bq.id) AS context_path,
                    NULL AS class_section_id,
                    NULL AS class_section_title,
                    NULL AS quiz_id,
                    NULL AS quiz_title,
                    qb.id AS question_bank_id,
                    qb.name AS question_bank_name,
                    s.id AS subject_id,
                    s.title AS subject_title
                FROM resource_references rr
                JOIN bank_question_options bqo ON rr.entity_type = 'BANK_QUESTION_OPTION' AND rr.entity_id = bqo.id AND bqo.is_deleted = false
                JOIN bank_questions bq ON bq.id = bqo.bank_question_id AND bq.is_deleted = false
                JOIN question_banks qb ON qb.id = bq.question_bank_id AND qb.is_deleted = false
                LEFT JOIN subjects s ON s.id = qb.subject_id AND s.is_deleted = false
                WHERE rr.resource_id = ? AND rr.is_deleted = false
                UNION ALL
                SELECT
                    rr.entity_type,
                    rr.entity_id,
                    rr.field_name,
                    CONCAT('Câu hỏi quiz #', qq.id) AS label,
                    CONCAT('Lớp: ', COALESCE(cs.title, '-'), ' -> Quiz: ', COALESCE(q.title, '-')) AS context_path,
                    cs.id AS class_section_id,
                    cs.title AS class_section_title,
                    q.id AS quiz_id,
                    q.title AS quiz_title,
                    NULL AS question_bank_id,
                    NULL AS question_bank_name,
                    s.id AS subject_id,
                    s.title AS subject_title
                FROM resource_references rr
                JOIN quiz_question qq ON rr.entity_type = 'QUIZ_QUESTION' AND rr.entity_id = qq.id AND qq.is_deleted = false
                JOIN quiz q ON q.id = qq.quiz_id AND q.is_deleted = false
                LEFT JOIN class_sections cs ON cs.id = q.class_section_id AND cs.is_deleted = false
                LEFT JOIN subjects s ON s.id = cs.subject_id AND s.is_deleted = false
                WHERE rr.resource_id = ? AND rr.is_deleted = false
                UNION ALL
                SELECT
                    rr.entity_type,
                    rr.entity_id,
                    rr.field_name,
                    CONCAT('Đáp án quiz #', qa.id) AS label,
                    CONCAT('Lớp: ', COALESCE(cs.title, '-'), ' -> Quiz: ', COALESCE(q.title, '-'), ' -> Câu hỏi #', qq.id) AS context_path,
                    cs.id AS class_section_id,
                    cs.title AS class_section_title,
                    q.id AS quiz_id,
                    q.title AS quiz_title,
                    NULL AS question_bank_id,
                    NULL AS question_bank_name,
                    s.id AS subject_id,
                    s.title AS subject_title
                FROM resource_references rr
                JOIN quiz_answer qa ON rr.entity_type = 'QUIZ_ANSWER' AND rr.entity_id = qa.id AND qa.is_deleted = false
                JOIN quiz_question qq ON qq.id = qa.quiz_question_id AND qq.is_deleted = false
                JOIN quiz q ON q.id = qq.quiz_id AND q.is_deleted = false
                LEFT JOIN class_sections cs ON cs.id = q.class_section_id AND cs.is_deleted = false
                LEFT JOIN subjects s ON s.id = cs.subject_id AND s.is_deleted = false
                WHERE rr.resource_id = ? AND rr.is_deleted = false
                UNION ALL
                SELECT
                    rr.entity_type,
                    rr.entity_id,
                    rr.field_name,
                    CONCAT('Mục tương tác #', qii.id) AS label,
                    CASE
                        WHEN qq.id IS NOT NULL THEN CONCAT('Lớp: ', COALESCE(cs.title, '-'), ' -> Quiz: ', COALESCE(q.title, '-'), ' -> Câu hỏi #', qq.id)
                        WHEN bq.id IS NOT NULL THEN CONCAT('Môn: ', COALESCE(s2.title, '-'), ' -> Ngân hàng: ', COALESCE(qb2.name, '-'), ' -> Câu hỏi #', bq.id)
                        ELSE 'Không xác định'
                    END AS context_path,
                    cs.id AS class_section_id,
                    cs.title AS class_section_title,
                    q.id AS quiz_id,
                    q.title AS quiz_title,
                    qb2.id AS question_bank_id,
                    qb2.name AS question_bank_name,
                    COALESCE(s.id, s2.id) AS subject_id,
                    COALESCE(s.title, s2.title) AS subject_title
                FROM resource_references rr
                JOIN question_interaction_items qii ON rr.entity_type = 'INTERACTION_ITEM' AND rr.entity_id = qii.id AND qii.is_deleted = false
                LEFT JOIN quiz_question qq ON qq.id = qii.quiz_question_id AND qq.is_deleted = false
                LEFT JOIN quiz q ON q.id = qq.quiz_id AND q.is_deleted = false
                LEFT JOIN class_sections cs ON cs.id = q.class_section_id AND cs.is_deleted = false
                LEFT JOIN subjects s ON s.id = cs.subject_id AND s.is_deleted = false
                LEFT JOIN bank_questions bq ON bq.id = qii.bank_question_id AND bq.is_deleted = false
                LEFT JOIN question_banks qb2 ON qb2.id = bq.question_bank_id AND qb2.is_deleted = false
                LEFT JOIN subjects s2 ON s2.id = qb2.subject_id AND s2.is_deleted = false
                WHERE rr.resource_id = ? AND rr.is_deleted = false
                UNION ALL
                SELECT
                    rr.entity_type,
                    rr.entity_id,
                    rr.field_name,
                    CONCAT('Bài học #', l.id) AS label,
                    CONCAT('Môn: ', COALESCE(lx.subject_title, '-'), ' -> Lớp: ', COALESCE(lx.class_titles, '-')) AS context_path,
                    lx.class_section_id,
                    lx.class_section_title,
                    NULL AS quiz_id,
                    NULL AS quiz_title,
                    NULL AS question_bank_id,
                    NULL AS question_bank_name,
                    lx.subject_id,
                    lx.subject_title
                FROM resource_references rr
                JOIN lesson l ON rr.entity_type = 'LESSON' AND rr.entity_id = l.id AND l.is_deleted = false
                LEFT JOIN (
                    SELECT
                        cci.lesson_id,
                        MIN(cs.id) AS class_section_id,
                        MIN(cs.title) AS class_section_title,
                        MIN(s.id) AS subject_id,
                        MIN(s.title) AS subject_title,
                        GROUP_CONCAT(DISTINCT cs.title ORDER BY cs.title SEPARATOR ', ') AS class_titles
                    FROM class_content_items cci
                    JOIN class_chapters cch ON cch.id = cci.class_chapter_id AND cch.is_deleted = false
                    JOIN class_sections cs ON cs.id = cch.class_section_id AND cs.is_deleted = false
                    LEFT JOIN subjects s ON s.id = cs.subject_id AND s.is_deleted = false
                    WHERE cci.is_deleted = false AND cci.lesson_id IS NOT NULL
                    GROUP BY cci.lesson_id
                ) lx ON lx.lesson_id = l.id
                WHERE rr.resource_id = ? AND rr.is_deleted = false
                UNION ALL
                SELECT
                    rr.entity_type,
                    rr.entity_id,
                    rr.field_name,
                    CONCAT('Bài tập #', a.id) AS label,
                    CONCAT('Môn: ', COALESCE(s.title, '-'), ' -> Lớp: ', COALESCE(cs.title, '-')) AS context_path,
                    cs.id AS class_section_id,
                    cs.title AS class_section_title,
                    NULL AS quiz_id,
                    NULL AS quiz_title,
                    NULL AS question_bank_id,
                    NULL AS question_bank_name,
                    s.id AS subject_id,
                    s.title AS subject_title
                FROM resource_references rr
                JOIN assignments a ON rr.entity_type = 'ASSIGNMENT' AND rr.entity_id = a.id AND a.is_deleted = false
                LEFT JOIN class_sections cs ON cs.id = a.class_section_id AND cs.is_deleted = false
                LEFT JOIN subjects s ON s.id = cs.subject_id AND s.is_deleted = false
                WHERE rr.resource_id = ? AND rr.is_deleted = false
                UNION ALL
                SELECT
                    rr.entity_type,
                    rr.entity_id,
                    rr.field_name,
                    CONCAT('Bài nộp #', sub.id) AS label,
                    CONCAT('Môn: ', COALESCE(s.title, '-'), ' -> Lớp: ', COALESCE(cs.title, '-'), ' -> Bài tập: ', COALESCE(a.title, CONCAT('#', a.id))) AS context_path,
                    cs.id AS class_section_id,
                    cs.title AS class_section_title,
                    NULL AS quiz_id,
                    NULL AS quiz_title,
                    NULL AS question_bank_id,
                    NULL AS question_bank_name,
                    s.id AS subject_id,
                    s.title AS subject_title
                FROM resource_references rr
                JOIN submission sub ON rr.entity_type = 'SUBMISSION' AND rr.entity_id = sub.id AND sub.is_deleted = false
                LEFT JOIN assignments a ON a.id = sub.assignment_id AND a.is_deleted = false
                LEFT JOIN class_sections cs ON cs.id = COALESCE(sub.class_section_id, a.class_section_id) AND cs.is_deleted = false
                LEFT JOIN subjects s ON s.id = cs.subject_id AND s.is_deleted = false
                WHERE rr.resource_id = ? AND rr.is_deleted = false
                ORDER BY entity_type, entity_id
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new ResourceReferenceResponse(
                        rs.getString("entity_type"),
                        rs.getInt("entity_id"),
                        rs.getString("field_name"),
                        rs.getString("label"),
                        rs.getString("context_path"),
                        nullableInt(rs, "class_section_id"),
                        rs.getString("class_section_title"),
                        nullableInt(rs, "quiz_id"),
                        rs.getString("quiz_title"),
                        nullableInt(rs, "question_bank_id"),
                        rs.getString("question_bank_name"),
                        nullableInt(rs, "subject_id"),
                        rs.getString("subject_title")
                ),
                resourceId, resourceId, resourceId, resourceId, resourceId, resourceId, resourceId, resourceId
        );
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
        List<Resource> resources = mergeLegacyAndAttachedResources(
                resourceRepository.findByLesson_Id(lessonId),
                resourceReferenceRepository.findByEntityTypeAndEntityIdAndFieldNameOrderByIdDesc(
                        ENTITY_LESSON,
                        lessonId,
                        ATTACHED_FIELD_NAME
                )
        );
        return resources.stream().map(this::convertEntityToDTO).toList();
    }

    @Override
    public List<ResourceResponse> getResourcesByAssignmentId(Integer assignmentId) {
        List<Resource> resources = mergeLegacyAndAttachedResources(
                resourceRepository.findByAssignment_Id(assignmentId),
                resourceReferenceRepository.findByEntityTypeAndEntityIdAndFieldNameOrderByIdDesc(
                        ENTITY_ASSIGNMENT,
                        assignmentId,
                        ATTACHED_FIELD_NAME
                )
        );
        return resources.stream().map(this::convertEntityToDTO).toList();
    }

    @Override
    public List<ResourceResponse> getResourcesBySubmissionId(Integer submissionId) {
        List<Resource> resources = mergeLegacyAndAttachedResources(
                resourceRepository.findBySubmission_Id(submissionId),
                resourceReferenceRepository.findByEntityTypeAndEntityIdAndFieldNameOrderByIdDesc(
                        ENTITY_SUBMISSION,
                        submissionId,
                        ATTACHED_FIELD_NAME
                )
        );
        return resources.stream().map(this::convertEntityToDTO).toList();
    }

    @Override
    @Transactional
    public void attachResourceToLesson(Integer lessonId, Integer resourceId) {
        lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));
        attachResource(ENTITY_LESSON, lessonId, resourceId);
    }

    @Override
    @Transactional
    public void attachResourceToAssignment(Integer assignmentId, Integer resourceId) {
        assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        attachResource(ENTITY_ASSIGNMENT, assignmentId, resourceId);
    }

    @Override
    @Transactional
    public void detachResourceFromLesson(Integer lessonId, Integer resourceId) {
        lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));
        detachResource(ENTITY_LESSON, lessonId, resourceId);
    }

    @Override
    @Transactional
    public void detachResourceFromAssignment(Integer assignmentId, Integer resourceId) {
        assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        detachResource(ENTITY_ASSIGNMENT, assignmentId, resourceId);
    }

    @Override
    @Transactional
    public void replaceAttachedResources(String entityType, Integer entityId, List<Integer> resourceIds) {
        if (!StringUtils.hasText(entityType) || entityId == null) {
            return;
        }
        resourceReferenceRepository.softDeleteByEntity(entityType, entityId, ATTACHED_FIELD_NAME);
        if (resourceIds == null || resourceIds.isEmpty()) {
            return;
        }

        for (Integer resourceId : resourceIds) {
            if (resourceId == null) {
                continue;
            }
            attachResource(entityType, entityId, resourceId);
        }
    }

    @Override
    public ResourceResponse convertEntityToDTO(Resource resource) {
        ResourceResponse response = new ResourceResponse();
        ResourceType normalizedType = resource.getType();
        ResourceSource normalizedSource = resource.getSource();
        String normalizedFileUrl = resource.getFileUrl();
        String normalizedEmbedUrl = resource.getEmbedUrl();
        String normalizedCloudinaryId = resource.getCloudinaryId();

        // Backward compatibility: old LINK rows could be stored as EMBED + embedUrl.
        if (normalizedType == ResourceType.LINK) {
            normalizedSource = ResourceSource.LINK;
            if (!StringUtils.hasText(normalizedFileUrl) && StringUtils.hasText(normalizedEmbedUrl)) {
                normalizedFileUrl = normalizedEmbedUrl;
            }
            normalizedEmbedUrl = null;
            normalizedCloudinaryId = null;
        }

        response.setId(resource.getId());
        response.setTitle(resource.getTitle());
        response.setDescription(resource.getDescription());
        response.setType(normalizedType);
        response.setSource(normalizedSource);
        response.setScopeType(resource.getScopeType());
        response.setScopeId(resource.getScopeId());
        response.setVisibility(resource.getVisibility());
        response.setStatus(resource.getStatus());
        Integer usageCount = resolveUsageCount(resource.getId());
        response.setUsageCount(usageCount);
        response.setLastUsedAt(resource.getLastUsedAt());
        response.setCreatedBy(resource.getCreatedBy());
        response.setEmbedUrl(normalizedEmbedUrl);
        response.setCloudinaryId(normalizedCloudinaryId);
        response.setFileUrl(normalizedFileUrl);
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
        boolean isAdmin = currentUser != null
                && currentUser.getRole() != null
                && currentUser.getRole().getRoleName() == RoleType.ADMIN;
        boolean ownerLibrary = Boolean.TRUE.equals(safeRequest.getOwnerLibrary());
        boolean includeCurrentScope = Boolean.TRUE.equals(safeRequest.getIncludeCurrentScope());
        boolean hasRequestedScope = safeRequest.getScopeType() != null && safeRequest.getScopeId() != null;
        boolean canBrowseRequestedScope = hasRequestedScope
                && resourceAuthorizationService.canBrowseScope(safeRequest.getScopeType(), safeRequest.getScopeId());
        boolean skipStrictScopeFilter = ownerLibrary && includeCurrentScope && hasRequestedScope;

        Specification<Resource> spec = Specification.where(
                ResourceSpecification.visibilityForBrowse(
                        isAdmin,
                        currentUsername,
                        ownerLibrary,
                        includeCurrentScope,
                        safeRequest.getScopeType(),
                        safeRequest.getScopeId(),
                        canBrowseRequestedScope
                )
        );
        if (Boolean.TRUE.equals(safeRequest.getCreatedByMe()) && StringUtils.hasText(currentUsername)) {
            spec = spec.and(ResourceSpecification.createdBy(currentUsername));
        }
        spec = spec.and(ResourceSpecification.ownerContains(safeRequest.getOwner()));
        if (!skipStrictScopeFilter) {
            spec = spec.and(ResourceSpecification.hasScopeType(safeRequest.getScopeType()));
            spec = spec.and(ResourceSpecification.hasScopeId(safeRequest.getScopeId()));
        }
        spec = spec.and(ResourceSpecification.hasType(safeRequest.getType()));
        spec = spec.and(ResourceSpecification.hasSource(safeRequest.getSource()));
        spec = spec.and(safeRequest.getStatus() != null
                ? ResourceSpecification.hasStatus(safeRequest.getStatus())
                : ResourceSpecification.activeByDefault());
        spec = spec.and(ResourceSpecification.searchContains(safeRequest.getSearch()));
        return spec;
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
                  (SELECT COUNT(*) FROM resource WHERE id = ? AND submission_id IS NOT NULL AND is_deleted = false) +
                  (SELECT COUNT(*)
                     FROM resource_references
                    WHERE resource_id = ?
                      AND is_deleted = false
                      AND field_name = 'attached.resource'
                      AND entity_type IN ('LESSON', 'ASSIGNMENT', 'SUBMISSION'))
                """;
        Integer count = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                resourceId, resourceId, resourceId, resourceId, resourceId, resourceId, resourceId, resourceId, resourceId
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
            if (StringUtils.hasText(resource.getEmbedUrl())) {
                source = ResourceSource.EMBED;
            } else if (resource.getType() == ResourceType.LINK && StringUtils.hasText(resource.getFileUrl())) {
                source = ResourceSource.LINK;
            } else {
                source = ResourceSource.UPLOAD;
            }
            resource.setSource(source);
        }

        if (source == ResourceSource.EMBED) {
            if (!StringUtils.hasText(resource.getEmbedUrl())) {
                throw new BusinessException("Embed URL is required for EMBED resource");
            }
            resource.setEmbedUrl(normalizeEmbedUrl(resource.getEmbedUrl()));
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

        if (source == ResourceSource.LINK) {
            if (!StringUtils.hasText(resource.getFileUrl())) {
                throw new BusinessException("File URL is required for LINK resource");
            }
            resource.setEmbedUrl(null);
            resource.setCloudinaryId(null);
            resource.setFileUrl(resource.getFileUrl().trim());
            resource.setType(ResourceType.LINK);
            if (!StringUtils.hasText(resource.getTitle())) {
                resource.setTitle(buildDefaultTitle(resource.getFileUrl()));
            }
            return;
        }

        if (resource.getType() == ResourceType.LINK && StringUtils.hasText(resource.getFileUrl())) {
            resource.setSource(ResourceSource.LINK);
            resource.setEmbedUrl(null);
            resource.setCloudinaryId(null);
            resource.setFileUrl(resource.getFileUrl().trim());
            if (!StringUtils.hasText(resource.getTitle())) {
                resource.setTitle(buildDefaultTitle(resource.getFileUrl()));
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

    private String normalizeEmbedUrl(String rawUrl) {
        if (!StringUtils.hasText(rawUrl)) {
            return rawUrl;
        }

        String normalized = rawUrl.trim();

        Matcher youtubeMatcher = YOUTUBE_EMBED_PATTERN.matcher(normalized);
        if (youtubeMatcher.find()) {
            return "https://www.youtube.com/embed/" + youtubeMatcher.group(1);
        }

        Matcher vimeoMatcher = VIMEO_EMBED_PATTERN.matcher(normalized);
        if (vimeoMatcher.find()) {
            return "https://player.vimeo.com/video/" + vimeoMatcher.group(1);
        }

        return normalized;
    }

    private static final Pattern YOUTUBE_EMBED_PATTERN = Pattern.compile(
            "(?:youtube\\.com/(?:watch\\?v=|embed/|shorts/)|youtu\\.be/)([A-Za-z0-9_-]{6,})",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern VIMEO_EMBED_PATTERN = Pattern.compile(
            "vimeo\\.com/(?:video/)?(\\d+)",
            Pattern.CASE_INSENSITIVE
    );

    private List<Resource> mergeLegacyAndAttachedResources(
            List<Resource> legacyResources,
            List<ResourceReference> attachedReferences
    ) {
        Map<Integer, Resource> merged = new java.util.LinkedHashMap<>();
        if (legacyResources != null) {
            for (Resource resource : legacyResources) {
                if (resource != null && resource.getId() != null) {
                    merged.put(resource.getId(), resource);
                }
            }
        }
        if (attachedReferences != null) {
            for (ResourceReference reference : attachedReferences) {
                Resource resource = reference != null ? reference.getResource() : null;
                if (resource != null && resource.getId() != null) {
                    merged.putIfAbsent(resource.getId(), resource);
                }
            }
        }
        return new ArrayList<>(merged.values());
    }

    private void attachResource(String entityType, Integer entityId, Integer resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        resourceAuthorizationService.assertCanUse(resource, null, null);
        if (resource.getStatus() == ResourceStatus.ARCHIVED) {
            throw new BusinessException("Archived media cannot be attached");
        }

        boolean exists = resourceReferenceRepository.existsByResource_IdAndEntityTypeAndEntityIdAndFieldName(
                resourceId,
                entityType,
                entityId,
                ATTACHED_FIELD_NAME
        );
        if (exists) {
            return;
        }

        ResourceReference reference = new ResourceReference();
        reference.setResource(resource);
        reference.setEntityType(entityType);
        reference.setEntityId(entityId);
        reference.setFieldName(ATTACHED_FIELD_NAME);
        resourceReferenceRepository.save(reference);

        resource.setLastUsedAt(LocalDateTime.now());
        resourceRepository.save(resource);
        logResourceAction(resource, "ATTACH", "Gắn media vào nội dung");
    }

    private void detachResource(String entityType, Integer entityId, Integer resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        resourceAuthorizationService.assertCanUse(resource, null, null);

        boolean detached = false;
        if (ENTITY_LESSON.equals(entityType)
                && resource.getLesson() != null
                && resource.getLesson().getId() != null
                && resource.getLesson().getId().equals(entityId)) {
            resource.setLesson(null);
            detached = true;
        }
        if (ENTITY_ASSIGNMENT.equals(entityType)
                && resource.getAssignment() != null
                && resource.getAssignment().getId() != null
                && resource.getAssignment().getId().equals(entityId)) {
            resource.setAssignment(null);
            detached = true;
        }
        if (ENTITY_SUBMISSION.equals(entityType)
                && resource.getSubmission() != null
                && resource.getSubmission().getId() != null
                && resource.getSubmission().getId().equals(entityId)) {
            resource.setSubmission(null);
            detached = true;
        }

        resourceReferenceRepository.softDeleteByEntityAndResource(entityType, entityId, ATTACHED_FIELD_NAME, resourceId);
        resourceReferenceRepository.softDeleteByEntityAndResource(entityType, entityId, entityType.toLowerCase(Locale.ROOT) + ".resources", resourceId);

        if (detached) {
            resourceRepository.save(resource);
        }
        logResourceAction(resource, "DETACH", "Gỡ media khỏi nội dung");
    }

    private void validateStandaloneResource(Resource resource) {
        if (resource.getSource() == ResourceSource.EMBED && !StringUtils.hasText(resource.getEmbedUrl())) {
            throw new BusinessException("Embed URL is required");
        }

        if (resource.getSource() == ResourceSource.LINK && !StringUtils.hasText(resource.getFileUrl())) {
            throw new BusinessException("File URL is required");
        }

        if (resource.getSource() == ResourceSource.UPLOAD && !StringUtils.hasText(resource.getFileUrl())) {
            throw new BusinessException("File URL is required");
        }
    }

    private Integer nullableInt(ResultSet rs, String column) throws SQLException {
        Number number = (Number) rs.getObject(column);
        return number != null ? number.intValue() : null;
    }

    private record ResourceReferenceSeed(String entityType, Integer entityId, String fieldName) {
    }
}
