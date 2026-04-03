package com.example.backend.service;

import com.example.backend.dto.request.curriculum.CurriculumTemplateRequest;
import com.example.backend.dto.response.curriculum.CurriculumTemplateResponse;

import java.util.List;

public interface CurriculumTemplateService {
    CurriculumTemplateResponse createTemplate(CurriculumTemplateRequest request);

    CurriculumTemplateResponse updateTemplate(Integer id, CurriculumTemplateRequest request);

    CurriculumTemplateResponse getTemplateById(Integer id);

    List<CurriculumTemplateResponse> getTemplates(Integer subjectId, boolean includeVersions);
}
