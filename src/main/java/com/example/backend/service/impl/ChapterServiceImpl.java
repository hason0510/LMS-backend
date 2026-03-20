package com.example.backend.service.impl;
import com.example.backend.dto.request.chapter.ChapterRequest;
import com.example.backend.dto.response.chapter.ChapterResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.entity.Chapter;
import com.example.backend.entity.Course;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.ChapterRepository;
import com.example.backend.repository.CourseRepository;
import com.example.backend.service.ChapterService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChapterServiceImpl implements ChapterService {

    private final ChapterRepository chapterRepository;
    private final CourseRepository courseRepository;

    public ChapterServiceImpl(ChapterRepository ChapterRepository, CourseRepository courseRepository) {
        this.chapterRepository = ChapterRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    public ChapterResponse convertChapterToDTO(Chapter chapter) {
        ChapterResponse responseDTO = new ChapterResponse();
        responseDTO.setId(chapter.getId());
        responseDTO.setTitle(chapter.getTitle());
        responseDTO.setOrderIndex(chapter.getOrderIndex());
        responseDTO.setDescription(chapter.getDescription());
        if (chapter.getCourse() != null) {
            responseDTO.setCourseId(chapter.getCourse().getId());
            responseDTO.setCourseTitle(chapter.getCourse().getTitle());
        }
        return responseDTO;
    }

    @Override
    public PageResponse<ChapterResponse> getChapterPage(Pageable pageable) {
        Page<Chapter> chapterPage = chapterRepository.findAll(pageable);
        Page<ChapterResponse> chapterResponsePage = chapterPage.map(this::convertChapterToDTO);
        return new PageResponse<>(
                chapterResponsePage.getNumber() + 1,
                chapterResponsePage.getTotalPages(),
                (int) chapterResponsePage.getTotalElements(),
                chapterResponsePage.getContent()
        );
    }

    @Override
    public ChapterResponse getChapterById(Integer id) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Chapter not found with id: " + id));
        return convertChapterToDTO(chapter);
    }

    @Transactional
    @Override
    public ChapterResponse createChapter(Integer courseId, ChapterRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        Chapter chapter = new Chapter();
        chapter.setTitle(request.getTitle());
        chapter.setOrderIndex(Integer.parseInt(request.getOrderIndex()));
        chapter.setDescription(request.getDescription());
        Integer maxOrder = chapterRepository.findByCourse_IdOrderByOrderIndexAsc(courseId)
                .stream()
                .map(Chapter::getOrderIndex)
                .max(Integer::compare)
                .orElse(0);
        chapter.setOrderIndex(maxOrder + 1);
        chapter.setCourse(course);
        // Tìm Course tương ứng Chapter
        chapterRepository.save(chapter);
        return convertChapterToDTO(chapter);
    }

    @Override
    @Transactional
    public ChapterResponse updateChapter(Integer id, ChapterRequest request) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Chapter not found with id: " + id));
        chapter.setTitle(request.getTitle());
        chapter.setDescription(request.getDescription());
        // Cập nhật lại Course khi courseId thay đổi
//        if (request.getCourseId() != null) {
//            Course course = courseRepository.findById(request.getCourseId())
//                    .orElseThrow(() -> new RuntimeException("Course not found with id: " + request.getCourseId()));
//            chapter.setCourse(course);
//        }
        chapterRepository.save(chapter);
        return convertChapterToDTO(chapter);
    }

    @Override
    @Transactional
    public void deleteChapter(Integer id) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Chapter not found with id: " + id));
        chapterRepository.delete(chapter);
    }

    @Override
    public List<ChapterResponse> getChaptersByCourseId(Integer courseId) {
        List<Chapter> chapters = chapterRepository.findByCourse_IdOrderByOrderIndexAsc(courseId);
        return chapters.stream()
                .map(this::convertChapterToDTO)
                .toList();
    }

    @Transactional
    @Override
    public void updateOrder(Integer courseId, List<Integer> orderedChapterIds) {
        for (int i = 0; i < orderedChapterIds.size(); i++) {
            int updated = chapterRepository.updateOrderIndex(orderedChapterIds.get(i), courseId, i + 1);
            if (updated == 0) {
                throw new ResourceNotFoundException("Chapter id " + orderedChapterIds.get(i) + " không thuộc course " + courseId);
            }
        }
    }
}
