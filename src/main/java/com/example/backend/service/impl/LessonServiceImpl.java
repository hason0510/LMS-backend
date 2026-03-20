package com.example.backend.service.impl;

import com.example.backend.dto.request.LessonRequest;
import com.example.backend.dto.response.LessonResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.ResourceResponse;
import com.example.backend.entity.Lesson;
import com.example.backend.entity.User;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.LessonRepository;
import com.example.backend.repository.ProgressRepository;
import com.example.backend.service.LessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {
    private final LessonRepository lessonRepository;
    private final ProgressRepository progressRepository;

    @Override
    public LessonResponse getLessonById(Integer id) {
        Lesson lesson = lessonRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));
        return convertEntityToDTO(lesson);
    }

    @Override
    public LessonResponse createLesson(LessonRequest request) {
        Lesson lesson = new Lesson();
        lesson.setTitle(request.getTitle());
        lesson.setContent(request.getContent());
        lesson.setVideoUrl(request.getVideoUrl());
        lesson.setNotes(request.getNotes());
        lessonRepository.save(lesson);
        return convertEntityToDTO(lesson);
    }

    @Override
    public LessonResponse updateLesson(Integer id, LessonRequest request) {
        Lesson lesson = lessonRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));
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
        return convertEntityToDTO(lessonRepository.save(lesson));
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
        if (lesson.getResources() != null && !lesson.getResources().isEmpty()) {
            List<ResourceResponse> resourceResponses = lesson.getResources()
                    .stream()
                    .map(resource -> {
                        ResourceResponse rr = new ResourceResponse();
                        rr.setId(resource.getId());
                        rr.setTitle(resource.getTitle());
                        rr.setFileUrl(resource.getFileUrl());
                        rr.setEmbedUrl(resource.getEmbedUrl());
                        rr.setCloudinaryId(resource.getCloudinaryId());
                        rr.setType(resource.getType());
                        rr.setLessonId(resource.getLesson().getId());
                        rr.setLessonTitle(resource.getLesson().getTitle());
                        return rr;
                    })
                    .toList();
            response.setResources(resourceResponses);
        } else {
            response.setResources(List.of()); // tránh null cho frontend
        }
        return response;
    }
}
