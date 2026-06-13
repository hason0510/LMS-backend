package com.example.backend.service.impl;

import com.example.backend.constant.ClassContentAvailabilityStatus;
import com.example.backend.constant.ContentItemType;
import com.example.backend.dto.request.LessonRequest;
import com.example.backend.dto.response.LessonResponse;
import com.example.backend.dto.response.ResourceResponse;
import com.example.backend.entity.ClassContentItem;
import com.example.backend.entity.Lesson;
import com.example.backend.entity.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.repository.ClassContentItemRepository;
import com.example.backend.repository.LessonRepository;
import com.example.backend.repository.ProgressRepository;
import com.example.backend.service.ClassContentAccessResult;
import com.example.backend.service.ClassContentAccessService;
import com.example.backend.service.LessonService;
import com.example.backend.service.ResourceService;
import com.example.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {
    private final LessonRepository lessonRepository;
    private final ProgressRepository progressRepository;
    private final ClassContentItemRepository classContentItemRepository;
    private final UserService userService;
    private final ClassContentAccessService classContentAccessService;
    private final ResourceService resourceService;

    @Override
    public LessonResponse getLessonById(Integer id) {
        return getLessonById(id, null);
    }

    @Override
    public LessonResponse getLessonById(Integer id, Integer classContentItemId) {
        Lesson lesson = lessonRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));
        if (classContentItemId != null) {
            validateLessonAccess(lesson, classContentItemId);
        }
        return convertEntityToDTO(lesson);
    }

    @Override
    public LessonResponse createLesson(LessonRequest request) {
        Lesson lesson = new Lesson();
        lesson.setTitle(request.getTitle());
        lesson.setContent(request.getContent());
        lesson.setVideoUrl(request.getVideoUrl());
        lesson.setNotes(request.getNotes());
        Lesson savedLesson = lessonRepository.save(lesson);
        if (request.getResourceIds() != null) {
            resourceService.replaceAttachedResources("LESSON", savedLesson.getId(), request.getResourceIds());
        }
        return convertEntityToDTO(savedLesson);
    }

    @Override
    public LessonResponse updateLesson(Integer id, LessonRequest request) {
        Lesson lesson = lessonRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));
        String previousTitle = lesson.getTitle();
        if(request.getTitle() != null){
            lesson.setTitle(request.getTitle());
        }
        if(request.getContent() != null){
            lesson.setContent(request.getContent());
        }
        if(request.getVideoUrl() != null){
            lesson.setVideoUrl(request.getVideoUrl());
        }
        if(request.getNotes() != null){
            lesson.setNotes(request.getNotes());
        }
        Lesson savedLesson = lessonRepository.save(lesson);
        if (request.getResourceIds() != null) {
            resourceService.replaceAttachedResources("LESSON", savedLesson.getId(), request.getResourceIds());
        }
        syncLinkedClassContentItemTitle(savedLesson, previousTitle);
        return convertEntityToDTO(savedLesson);
    }

    @Override
    public void deleteLesson(Integer id) {
        Lesson lesson = lessonRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));
        lesson.getResources().forEach(r -> r.set_deleted(true));
        lesson.getComments().forEach(c -> c.set_deleted(true));
        //lessonRepository.delete(lesson);
        lesson.set_deleted(true);
    }

//    @Override
//    public PageResponse<LessonResponse> getLessonPage(Pageable pageable) {
//        return null;
//    }
    @Override
    public LessonResponse convertEntityToDTO(Lesson lesson) {
        LessonResponse response = new LessonResponse();
        response.setId(lesson.getId());
        response.setTitle(lesson.getTitle());
        response.setContent(lesson.getContent());
        response.setVideoUrl(lesson.getVideoUrl());
        response.setNotes(lesson.getNotes());
        response.setResources(resourceService.getResourcesByLessonId(lesson.getId()));
        return response;
    }

    private void syncLinkedClassContentItemTitle(Lesson lesson, String previousLessonTitle) {
        classContentItemRepository.findByLesson_Id(lesson.getId()).ifPresent(classContentItem -> {
            String itemTitle = classContentItem.getTitle();
            if (!StringUtils.hasText(itemTitle) || Objects.equals(itemTitle, previousLessonTitle)) {
                classContentItem.setTitle(lesson.getTitle());
                classContentItemRepository.save(classContentItem);
            }
        });
    }

    private void validateLessonAccess(Lesson lesson, Integer classContentItemId) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Unauthorized");
        }

        ClassContentItem classContentItem = classContentItemRepository.findById(classContentItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Class content item not found"));
        if (classContentItem.getItemType() != ContentItemType.LESSON
                || classContentItem.getLesson() == null
                || !classContentItem.getLesson().getId().equals(lesson.getId())) {
            throw new UnauthorizedException("Nội dung bài học không hợp lệ");
        }

        ClassContentAccessResult accessResult = classContentAccessService.evaluateForUser(classContentItem, currentUser);
        if (accessResult.accessible()) {
            return;
        }
        if (accessResult.availabilityStatus() == ClassContentAvailabilityStatus.NO_ENROLLMENT) {
            throw new UnauthorizedException(accessResult.message());
        }
        throw new BusinessException(accessResult.message() != null
                ? accessResult.message()
                : "Bạn không có quyền truy cập nội dung này");
    }
}
