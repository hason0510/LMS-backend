package com.example.backend.service;

import com.example.backend.dto.request.LessonRequest;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.LessonResponse;
import com.example.backend.entity.Lesson;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface LessonService {

    LessonResponse getLessonById(Integer id);

    LessonResponse createLesson(LessonRequest request);

    LessonResponse updateLesson(Integer id, LessonRequest request);

    void deleteLesson(Integer id);

    //    @Override
    //    public PageResponse<LessonResponse> getLessonPage(Pageable pageable) {
    //        return null;
    //    }
    LessonResponse convertEntityToDTO(Lesson lesson);


}
