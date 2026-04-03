package com.example.backend.service.impl;

import com.example.backend.constant.ContentItemType;
import com.example.backend.constant.CurriculumStatus;
import com.example.backend.dto.request.curriculum.ChapterTemplateRequest;
import com.example.backend.dto.request.curriculum.ContentItemTemplateRequest;
import com.example.backend.dto.request.curriculum.CurriculumVersionRequest;
import com.example.backend.dto.response.curriculum.ChapterTemplateResponse;
import com.example.backend.dto.response.curriculum.ContentItemTemplateResponse;
import com.example.backend.dto.response.curriculum.CurriculumVersionResponse;
import com.example.backend.entity.*;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.AssignmentRepository;
import com.example.backend.repository.CurriculumTemplateRepository;
import com.example.backend.repository.CurriculumVersionRepository;
import com.example.backend.repository.LessonRepository;
import com.example.backend.repository.QuizRepository;
import com.example.backend.service.CurriculumVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CurriculumVersionServiceImpl implements CurriculumVersionService {
    private final CurriculumTemplateRepository curriculumTemplateRepository;
    private final CurriculumVersionRepository curriculumVersionRepository;
    private final LessonRepository lessonRepository;
    private final QuizRepository quizRepository;
    private final AssignmentRepository assignmentRepository;

    @Override
    @Transactional
    public CurriculumVersionResponse createVersion(Integer templateId, CurriculumVersionRequest request) {
        CurriculumTemplate template = curriculumTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Curriculum template not found"));

        CurriculumVersion version = new CurriculumVersion();
        version.setTemplate(template);
        version.setVersionNo(resolveVersionNo(template, request.getVersionNo()));
        version.setStatus(request.getStatus() != null ? request.getStatus() : CurriculumStatus.DRAFT);
        version.setBasedOnVersion(resolveBasedOnVersion(request.getBasedOnVersionId()));
        version.setChapters(buildChapterTemplates(version, request.getChapters()));

        return convertToResponse(curriculumVersionRepository.save(version));
    }

    @Override
    @Transactional
    public CurriculumVersionResponse updateVersion(Integer id, CurriculumVersionRequest request) {
        CurriculumVersion version = curriculumVersionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curriculum version not found"));

        Integer versionNo = request.getVersionNo() != null ? request.getVersionNo() : version.getVersionNo();
        ensureVersionNoUnique(version.getTemplate(), versionNo, version.getId());

        version.setVersionNo(versionNo);
        if (request.getStatus() != null) {
            version.setStatus(request.getStatus());
        }
        version.setBasedOnVersion(resolveBasedOnVersion(request.getBasedOnVersionId()));
        if (version.getChapters() == null) {
            version.setChapters(new ArrayList<>());
        } else {
            version.getChapters().clear();
        }
        version.getChapters().addAll(buildChapterTemplates(version, request.getChapters()));

        return convertToResponse(curriculumVersionRepository.save(version));
    }

    @Override
    @Transactional(readOnly = true)
    public CurriculumVersionResponse getVersionById(Integer id) {
        CurriculumVersion version = curriculumVersionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curriculum version not found"));
        return convertToResponse(version);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CurriculumVersionResponse> getVersionsByTemplate(Integer templateId) {
        return curriculumVersionRepository.findByTemplate_IdOrderByVersionNoDesc(templateId).stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CurriculumVersionResponse getLatestPublishedVersion(Integer templateId) {
        CurriculumVersion version = curriculumVersionRepository
                .findFirstByTemplate_IdAndStatusOrderByVersionNoDesc(templateId, CurriculumStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Published curriculum version not found"));
        return convertToResponse(version);
    }

    @Override
    @Transactional
    public CurriculumVersionResponse publishVersion(Integer id) {
        CurriculumVersion version = curriculumVersionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curriculum version not found"));

        List<CurriculumVersion> templateVersions = curriculumVersionRepository.findByTemplate_IdOrderByVersionNoDesc(version.getTemplate().getId());
        for (CurriculumVersion existingVersion : templateVersions) {
            if (!existingVersion.getId().equals(version.getId()) && existingVersion.getStatus() == CurriculumStatus.PUBLISHED) {
                existingVersion.setStatus(CurriculumStatus.ARCHIVED);
            }
        }
        curriculumVersionRepository.saveAll(templateVersions);

        version.setStatus(CurriculumStatus.PUBLISHED);
        return convertToResponse(curriculumVersionRepository.save(version));
    }

    @Override
    @Transactional
    public CurriculumVersionResponse archiveVersion(Integer id) {
        CurriculumVersion version = curriculumVersionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curriculum version not found"));
        version.setStatus(CurriculumStatus.ARCHIVED);
        return convertToResponse(curriculumVersionRepository.save(version));
    }

    private Integer resolveVersionNo(CurriculumTemplate template, Integer requestedVersionNo) {
        if (requestedVersionNo != null) {
            ensureVersionNoUnique(template, requestedVersionNo, null);
            return requestedVersionNo;
        }

        return curriculumVersionRepository.findByTemplate_IdOrderByVersionNoDesc(template.getId()).stream()
                .map(CurriculumVersion::getVersionNo)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private void ensureVersionNoUnique(CurriculumTemplate template, Integer versionNo, Integer currentVersionId) {
        boolean duplicate = curriculumVersionRepository.findByTemplate_IdOrderByVersionNoDesc(template.getId()).stream()
                .anyMatch(existing -> existing.getVersionNo().equals(versionNo)
                        && (currentVersionId == null || !existing.getId().equals(currentVersionId)));
        if (duplicate) {
            throw new BusinessException("Version number already exists for this template");
        }
    }

    private CurriculumVersion resolveBasedOnVersion(Integer basedOnVersionId) {
        if (basedOnVersionId == null) {
            return null;
        }

        return curriculumVersionRepository.findById(basedOnVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("Base curriculum version not found"));
    }

    private List<ChapterTemplate> buildChapterTemplates(CurriculumVersion version, List<ChapterTemplateRequest> chapterRequests) {
        List<ChapterTemplate> chapters = new ArrayList<>();
        if (chapterRequests == null) {
            return chapters;
        }

        int order = 1;
        for (ChapterTemplateRequest chapterRequest : chapterRequests) {
            ChapterTemplate chapter = new ChapterTemplate();
            chapter.setCurriculumVersion(version);
            chapter.setTitle(chapterRequest.getTitle());
            chapter.setDescription(chapterRequest.getDescription());
            chapter.setOrderIndex(chapterRequest.getOrderIndex() != null ? chapterRequest.getOrderIndex() : order++);
            chapter.setContentItems(buildContentItems(chapter, chapterRequest.getContentItems()));
            chapters.add(chapter);
        }
        return chapters;
    }

    private List<ContentItemTemplate> buildContentItems(ChapterTemplate chapter, List<ContentItemTemplateRequest> itemRequests) {
        List<ContentItemTemplate> contentItems = new ArrayList<>();
        if (itemRequests == null) {
            return contentItems;
        }

        int order = 1;
        for (ContentItemTemplateRequest itemRequest : itemRequests) {
            ContentItemTemplate item = new ContentItemTemplate();
            item.setChapterTemplate(chapter);
            item.setItemType(itemRequest.getItemType());
            item.setOrderIndex(itemRequest.getOrderIndex() != null ? itemRequest.getOrderIndex() : order++);
            applyContentReference(item, itemRequest);
            contentItems.add(item);
        }
        return contentItems;
    }

    private void applyContentReference(ContentItemTemplate item, ContentItemTemplateRequest request) {
        if (request.getItemType() == ContentItemType.LESSON) {
            item.setLesson(lessonRepository.findById(requireId(request.getLessonId(), "lessonId"))
                    .orElseThrow(() -> new ResourceNotFoundException("Lesson not found")));
            return;
        }
        if (request.getItemType() == ContentItemType.QUIZ) {
            item.setQuiz(quizRepository.findById(requireId(request.getQuizId(), "quizId"))
                    .orElseThrow(() -> new ResourceNotFoundException("Quiz not found")));
            return;
        }
        if (request.getItemType() == ContentItemType.ASSIGNMENT) {
            item.setAssignment(assignmentRepository.findById(requireId(request.getAssignmentId(), "assignmentId"))
                    .orElseThrow(() -> new ResourceNotFoundException("Assignment not found")));
            return;
        }
        throw new BusinessException("Unsupported content item type");
    }

    private Integer requireId(Integer id, String fieldName) {
        if (id == null) {
            throw new BusinessException(fieldName + " is required for the selected item type");
        }
        return id;
    }

    private CurriculumVersionResponse convertToResponse(CurriculumVersion version) {
        CurriculumVersionResponse response = new CurriculumVersionResponse();
        response.setId(version.getId());
        response.setVersionNo(version.getVersionNo());
        response.setStatus(version.getStatus());
        response.setTemplateId(version.getTemplate() != null ? version.getTemplate().getId() : null);
        response.setBasedOnVersionId(version.getBasedOnVersion() != null ? version.getBasedOnVersion().getId() : null);
        response.setClassSectionCount(version.getClassSections() != null ? version.getClassSections().size() : 0);
        response.setChapters((version.getChapters() != null ? version.getChapters() : Collections.<ChapterTemplate>emptyList()).stream()
                .map(this::convertChapterToResponse)
                .toList());
        return response;
    }

    private ChapterTemplateResponse convertChapterToResponse(ChapterTemplate chapter) {
        ChapterTemplateResponse response = new ChapterTemplateResponse();
        response.setId(chapter.getId());
        response.setTitle(chapter.getTitle());
        response.setDescription(chapter.getDescription());
        response.setOrderIndex(chapter.getOrderIndex());
        response.setContentItems((chapter.getContentItems() != null ? chapter.getContentItems() : Collections.<ContentItemTemplate>emptyList()).stream()
                .map(this::convertContentItemToResponse)
                .toList());
        return response;
    }

    private ContentItemTemplateResponse convertContentItemToResponse(ContentItemTemplate item) {
        ContentItemTemplateResponse response = new ContentItemTemplateResponse();
        response.setId(item.getId());
        response.setItemType(item.getItemType());
        response.setOrderIndex(item.getOrderIndex());
        response.setLessonId(item.getLesson() != null ? item.getLesson().getId() : null);
        response.setLessonTitle(item.getLesson() != null ? item.getLesson().getTitle() : null);
        response.setQuizId(item.getQuiz() != null ? item.getQuiz().getId() : null);
        response.setQuizTitle(item.getQuiz() != null ? item.getQuiz().getTitle() : null);
        response.setAssignmentId(item.getAssignment() != null ? item.getAssignment().getId() : null);
        response.setAssignmentTitle(item.getAssignment() != null ? item.getAssignment().getTitle() : null);
        return response;
    }
}
