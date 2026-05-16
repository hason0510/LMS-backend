package com.example.backend.service;

import com.example.backend.dto.request.ResourceRequest;
import com.example.backend.dto.request.ResourceSearchRequest;
import com.example.backend.dto.response.CloudinaryResponse;
import com.example.backend.dto.response.ResourceResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.ResourceAuditLogResponse;
import com.example.backend.dto.response.ResourceReferenceResponse;
import com.example.backend.dto.response.ResourceUploadPolicyResponse;
import com.example.backend.entity.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ResourceService {
    ResourceResponse getResourceById(Integer id);

    @Transactional
    ResourceResponse createResource(Integer lessonId, ResourceRequest request);

    @Transactional
    ResourceResponse createAssignmentResource(Integer assignmentId, ResourceRequest request);

    @Transactional
    ResourceResponse createSubmissionResource(Integer submissionId, ResourceRequest request);
    @Transactional
    ResourceResponse createStandaloneResource(ResourceRequest request);

    ResourceResponse updateResource(Integer id, ResourceRequest request);
    void deleteResource(Integer id);

    PageResponse<ResourceResponse> getResourcePage(ResourceSearchRequest request, Pageable pageable);

    ResourceUploadPolicyResponse getUploadPolicy();

    List<ResourceReferenceResponse> getResourceReferences(Integer resourceId);
    List<ResourceAuditLogResponse> getResourceAuditLogs(Integer resourceId);

    List<ResourceResponse> getResourcesByLessonId(Integer lessonId);
    List<ResourceResponse> getResourcesByAssignmentId(Integer assignmentId);
    List<ResourceResponse> getResourcesBySubmissionId(Integer submissionId);

    ResourceResponse convertEntityToDTO(Resource resource);
    Resource buildDetachedResource(ResourceRequest request);

    CloudinaryResponse uploadVideoResource(Integer id, MultipartFile file);

    CloudinaryResponse uploadSlideResource(Integer id, MultipartFile file);

    CloudinaryResponse uploadAttachmentResource(Integer id, MultipartFile file);
}
