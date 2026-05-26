package com.example.backend.service.impl;

import com.example.backend.dto.request.SubjectRequest;
import com.example.backend.dto.response.SubjectResponse;
import com.example.backend.entity.Category;
import com.example.backend.entity.Subject;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.CategoryRepository;
import com.example.backend.repository.SubjectRepository;
import com.example.backend.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public List<SubjectResponse> getAllSubjects() {
        return subjectRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SubjectResponse> getSubjectsByCategoryId(Integer categoryId) {
        return subjectRepository.findByCategoryId(categoryId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SubjectResponse getSubjectById(Integer id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + id));
        return mapToResponse(subject);
    }

    @Override
    public SubjectResponse createSubject(SubjectRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        String normalizedCode = normalizeSubjectCode(request.getCode());
        if (subjectRepository.existsByCode(normalizedCode)) {
            throw new BusinessException("Subject code already exists");
        }

        Subject subject = new Subject();
        subject.setCode(normalizedCode);
        subject.setTitle(request.getTitle());
        subject.setDescription(request.getDescription());
        subject.setCategory(category);

        return mapToResponse(subjectRepository.save(subject));
    }

    @Override
    public SubjectResponse updateSubject(Integer id, SubjectRequest request) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + id));
        
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        String normalizedCode = normalizeSubjectCode(request.getCode());
        if (subjectRepository.existsByCodeAndIdNot(normalizedCode, id)) {
            throw new BusinessException("Subject code already exists");
        }

        subject.setCode(normalizedCode);
        subject.setTitle(request.getTitle());
        subject.setDescription(request.getDescription());
        subject.setCategory(category);

        return mapToResponse(subjectRepository.save(subject));
    }

    @Override
    public void deleteSubject(Integer id) {
        if (!subjectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Subject not found with id: " + id);
        }
        subjectRepository.deleteById(id);
    }

    private SubjectResponse mapToResponse(Subject subject) {
        return SubjectResponse.builder()
                .id(subject.getId())
                .code(subject.getCode())
                .title(subject.getTitle())
                .description(subject.getDescription())
                .imageUrl(subject.getImageUrl())
                .categoryId(subject.getCategory() != null ? subject.getCategory().getId() : null)
                .categoryTitle(subject.getCategory() != null ? subject.getCategory().getTitle() : null)
                .build();
    }

    private String normalizeSubjectCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BusinessException("Subject code is required");
        }
        return code.trim();
    }
}
