package com.example.backend.controller;

import com.example.backend.dto.request.LessonRequest;
import com.example.backend.dto.request.chapter.ChapterItemOrderRequest;
import com.example.backend.dto.request.quiz.QuizRequest;
import com.example.backend.dto.response.chapter.ChapterItemResponse;
import com.example.backend.service.ChapterItemService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lms")
@RequiredArgsConstructor
public class ChapterItemController {

    private final ChapterItemService chapterItemService;

    @Operation(summary = "Lấy danh sách item trong chương dành cho giáo viên và admin")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'TEACHER')")
    @GetMapping("/chapters/{chapterId}/items")
    public ResponseEntity<List<ChapterItemResponse>> getItemsByChapter(
            @PathVariable Integer chapterId
    ) {
        return ResponseEntity.ok(
                chapterItemService.getItemsByChapter(chapterId)
        );
    }

    @Operation(summary = "Lấy danh sách item trong chương dành cho sinh viên")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'TEACHER')")
    @GetMapping("/chapters/{chapterId}/items/student")
    public ResponseEntity<List<ChapterItemResponse>> getItemsByChapterForStudent(
            @PathVariable Integer chapterId
    ) {
        return ResponseEntity.ok(
                chapterItemService.getItemsByChapterForStudent(chapterId)
        );
    }


    @Operation(summary = "Cập nhật thứ tự item trong chương")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PutMapping("/chapters/{chapterId}/order-items")
    public ResponseEntity<Void> updateItemOrder(
            @PathVariable Integer chapterId,
            @RequestBody ChapterItemOrderRequest request
    ) {
        chapterItemService.updateOrder(chapterId, request.getOrderedItemIds());
        return ResponseEntity.ok().build();
    }

    // ========== NEW API =============== //
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/chapters/{chapterId}/lessons")
    public ResponseEntity<ChapterItemResponse> createLessonInChapter(
            @PathVariable Integer chapterId,
            @RequestBody LessonRequest lessonRequest
    ) {
        return ResponseEntity.ok(
                chapterItemService.createLessonInChapter(chapterId, lessonRequest)
        );
    }

    // ========== NEW API =============== //
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/chapters/{chapterId}/quizzes")
    public ResponseEntity<ChapterItemResponse> createQuizInChapter(
            @PathVariable Integer chapterId,
            @RequestBody QuizRequest quizRequest
    ) {
        return ResponseEntity.ok(
                chapterItemService.createQuizInChapter(chapterId, quizRequest)
        );
    }

    // ========== NEW API =============== //
    @Operation(summary = "Xóa nội dung trong 1 chương")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @DeleteMapping("/chaptersItems/{id}")
    public ResponseEntity<Void> deleteChapterItem(@PathVariable Integer id) {
        chapterItemService.deleteChapterItem(id);
        return ResponseEntity.noContent().build();
    }



    // ========== OLD API =============== //
    @Operation(summary = "Thêm bài học sẵn có vào chương")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/chapters/{chapterId}/quizzes/{lessonId}")
    public ResponseEntity<ChapterItemResponse> addLessonToChapter(
            @PathVariable Integer chapterId,
            @PathVariable Integer lessonId
    ) {
        return ResponseEntity.ok(
                chapterItemService.addLessonToChapter(chapterId, lessonId)
        );
    }

    // ========== OLD API =============== //
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/chapters/{chapterId}/quizzes/{quizId}")
    public ResponseEntity<ChapterItemResponse> addQuizToChapter(
            @PathVariable Integer chapterId,
            @PathVariable Integer quizId
    ) {
        return ResponseEntity.ok(
                chapterItemService.addQuizToChapter(chapterId, quizId)
        );
    }

}