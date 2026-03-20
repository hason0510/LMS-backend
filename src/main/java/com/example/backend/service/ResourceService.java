package com.example.backend.service;

import com.example.backend.dto.request.ResourceRequest;
import com.example.backend.dto.response.CloudinaryResponse;
import com.example.backend.dto.response.ResourceResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.entity.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ResourceService {
    ResourceResponse getResourceById(Integer id);

    @Transactional
    ResourceResponse createResource(Integer lessonId, ResourceRequest request);

    ResourceResponse updateResource(Integer id, ResourceRequest request);
    void deleteResource(Integer id);

    PageResponse<ResourceResponse> getResourcePage(Pageable pageable);

    List<ResourceResponse> getResourcesByLessonId(Integer lessonId);

    ResourceResponse convertEntityToDTO(Resource resource);

    CloudinaryResponse uploadVideoResource(Integer id, MultipartFile file);

    CloudinaryResponse uploadSlideResource(Integer id, MultipartFile file);
}