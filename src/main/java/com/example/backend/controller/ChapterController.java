package com.example.backend.controller;

import com.example.backend.dto.request.chapter.ChapterOrderRequest;
import com.example.backend.dto.request.chapter.ChapterRequest;
import com.example.backend.dto.response.chapter.ChapterResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.service.ChapterService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/lms/chapters")
public class ChapterController {
    private final ChapterService chapterService;

    public ChapterController(ChapterService chapterService) {
        this.chapterService = chapterService;
    }

    @Operation(summary = "Tạo chương mới cho khóa học")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/{courseId}")
    public ResponseEntity<ChapterResponse> createChapter(@PathVariable Integer courseId, @RequestBody ChapterRequest request) {
        return ResponseEntity.ok(chapterService.createChapter(courseId, request));
    }

    @Operation(summary = "Lấy thông tin chương theo ID")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'TEACHER')")
    @GetMapping("/{id}")
    public ResponseEntity<ChapterResponse> getChapterById(@PathVariable Integer id) {
        return ResponseEntity.ok(chapterService.getChapterById(id));
    }

    @Operation(summary = "Lấy danh sách chương có phân trang")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'TEACHER')")
    @GetMapping
    public ResponseEntity<PageResponse<ChapterResponse>> getAllChapters(
            @RequestParam(value = "pageNumber", defaultValue = "1", required = false) Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "5", required = false) Integer pageSize
    ) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        PageResponse<ChapterResponse> chapterPage = chapterService.getChapterPage(pageable);
        return ResponseEntity.ok(chapterPage);
    }

    @Operation(summary = "Lấy danh sách chương theo khóa học")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'TEACHER')")
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<ChapterResponse>> getChaptersByCourseId(@PathVariable Integer courseId) {
        return ResponseEntity.ok(chapterService.getChaptersByCourseId(courseId));
    }

    @Operation(summary = "Cập nhật thông tin chương")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PutMapping("/{id}")
    public ResponseEntity<ChapterResponse> updateChapter(
            @PathVariable Integer id,
            @RequestBody ChapterRequest request
    ) {
        return ResponseEntity.ok(chapterService.updateChapter(id, request));
    }

    @Operation(summary = "Xóa chương")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChapter(@PathVariable Integer id) {
        chapterService.deleteChapter(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Cập nhật thứ tự chương trong khóa học")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PutMapping("/course/{courseId}/order-chapters")
    public ResponseEntity<Void> updateChapterOrder(
            @PathVariable Integer courseId,
            @RequestBody ChapterOrderRequest request
    ) {
        chapterService.updateOrder(courseId, request.getOrderedChapterIds());
        return ResponseEntity.ok().build();
    }
}