package com.example.backend.service;

import com.example.backend.dto.request.curriculum.CurriculumVersionRequest;
import com.example.backend.dto.response.curriculum.CurriculumVersionResponse;

import java.util.List;

public interface CurriculumVersionService {
    CurriculumVersionResponse createVersion(Integer templateId, CurriculumVersionRequest request);

    CurriculumVersionResponse updateVersion(Integer id, CurriculumVersionRequest request);

    CurriculumVersionResponse getVersionById(Integer id);

    List<CurriculumVersionResponse> getVersionsByTemplate(Integer templateId);

    CurriculumVersionResponse getLatestPublishedVersion(Integer templateId);

    CurriculumVersionResponse publishVersion(Integer id);

    CurriculumVersionResponse archiveVersion(Integer id);
}
