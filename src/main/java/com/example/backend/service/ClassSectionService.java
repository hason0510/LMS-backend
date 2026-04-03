package com.example.backend.service;

import com.example.backend.dto.request.classsection.ClassSectionRequest;
import com.example.backend.dto.request.classsection.LegacyCourseMigrationRequest;
import com.example.backend.dto.response.classsection.ClassSectionResponse;

public interface ClassSectionService {
    ClassSectionResponse createFromTemplate(Integer curriculumVersionId, ClassSectionRequest request);

    ClassSectionResponse createFromLatestPublishedVersion(Integer templateId, ClassSectionRequest request);

    ClassSectionResponse migrateFromLegacyCourse(Integer legacyCourseId, LegacyCourseMigrationRequest request);

    ClassSectionResponse getClassSectionById(Integer id);

    ClassSectionResponse getClassSectionByLegacyCourseId(Integer legacyCourseId);

    java.util.List<ClassSectionResponse> getClassSections(Integer teacherId, Integer subjectId, Integer curriculumVersionId, boolean includeChapters);
}
