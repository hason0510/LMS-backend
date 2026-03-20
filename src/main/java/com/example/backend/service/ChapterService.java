package com.example.backend.service;

import com.example.backend.dto.request.chapter.ChapterRequest;
import com.example.backend.dto.response.chapter.ChapterResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.entity.Chapter;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ChapterService {
    //ChapterResponse createChapter(ChapterRequest request);
    ChapterResponse getChapterById(Integer id);

    @Transactional
    ChapterResponse createChapter(Integer courseId, ChapterRequest request);

    ChapterResponse updateChapter(Integer id, ChapterRequest request);
    void deleteChapter(Integer id);
    
    PageResponse<ChapterResponse> getChapterPage(Pageable pageable);
    List<ChapterResponse> getChaptersByCourseId(Integer courseId);
    ChapterResponse convertChapterToDTO(Chapter chapter);

    @Transactional
    void updateOrder(Integer courseId, List<Integer> orderedChapterIds);
}

