package com.example.backend.service.impl;

import com.example.backend.dto.request.curriculum.CurriculumTemplateRequest;
import com.example.backend.dto.response.curriculum.CurriculumTemplateResponse;
import com.example.backend.entity.CurriculumTemplate;
import com.example.backend.entity.Subject;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.CurriculumTemplateRepository;
import com.example.backend.repository.SubjectRepository;
import com.example.backend.service.CurriculumTemplateService;
import com.example.backend.service.CurriculumVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CurriculumTemplateServiceImpl implements CurriculumTemplateService {
    private final CurriculumTemplateRepository curriculumTemplateRepository;
    private final SubjectRepository subjectRepository;
    private final CurriculumVersionService curriculumVersionService;

    @Override
    @Transactional
    public CurriculumTemplateResponse createTemplate(CurriculumTemplateRequest request) {
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        CurriculumTemplate template = new CurriculumTemplate();
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setDefault(Boolean.TRUE.equals(request.getIsDefault()));
        template.setSubject(subject);

        return convertToResponse(curriculumTemplateRepository.save(template), false);
    }

    @Override
    @Transactional
    public CurriculumTemplateResponse updateTemplate(Integer id, CurriculumTemplateRequest request) {
        CurriculumTemplate template = curriculumTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curriculum template not found"));

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setDefault(Boolean.TRUE.equals(request.getIsDefault()));
        template.setSubject(subject);

        return convertToResponse(curriculumTemplateRepository.save(template), false);
    }

    @Override
    @Transactional(readOnly = true)
    public CurriculumTemplateResponse getTemplateById(Integer id) {
        CurriculumTemplate template = curriculumTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curriculum template not found"));
        return convertToResponse(template, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CurriculumTemplateResponse> getTemplates(Integer subjectId, boolean includeVersions) {
        List<CurriculumTemplate> templates = subjectId != null
                ? curriculumTemplateRepository.findBySubject_Id(subjectId)
                : curriculumTemplateRepository.findAll();

        return templates.stream()
                .map(template -> convertToResponse(template, includeVersions))
                .toList();
    }

    private CurriculumTemplateResponse convertToResponse(CurriculumTemplate template, boolean includeVersions) {
        CurriculumTemplateResponse response = new CurriculumTemplateResponse();
        response.setId(template.getId());
        response.setName(template.getName());
        response.setDescription(template.getDescription());
        response.setIsDefault(template.isDefault());
        response.setSubjectId(template.getSubject() != null ? template.getSubject().getId() : null);
        response.setSubjectTitle(template.getSubject() != null ? template.getSubject().getTitle() : null);
        response.setVersionCount(template.getVersions() != null ? template.getVersions().size() : 0);
        response.setVersions(includeVersions ? curriculumVersionService.getVersionsByTemplate(template.getId()) : null);
        return response;
    }
}
