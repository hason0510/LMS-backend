package com.example.backend.service;

import com.example.backend.dto.request.SubjectRequest;
import com.example.backend.dto.response.SubjectResponse;
import java.util.List;

public interface SubjectService {
    List<SubjectResponse> getAllSubjects();
    List<SubjectResponse> getSubjectsByCategoryId(Integer categoryId);
    SubjectResponse getSubjectById(Integer id);
    SubjectResponse createSubject(SubjectRequest request);
    SubjectResponse updateSubject(Integer id, SubjectRequest request);
    void deleteSubject(Integer id);
}
