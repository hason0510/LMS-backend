package com.example.backend.controller;

import com.example.backend.dto.request.classsection.ClassSectionRequest;
import com.example.backend.dto.request.curriculum.ChapterTemplateUpsertRequest;
import com.example.backend.dto.request.curriculum.ContentItemTemplateRequest;
import com.example.backend.dto.request.curriculum.CurriculumTemplateRequest;
import com.example.backend.dto.request.curriculum.LessonTemplateRequest;
import com.example.backend.dto.request.curriculum.QuizTemplateRequest;
import com.example.backend.dto.response.classsection.ClassSectionResponse;
import com.example.backend.dto.response.curriculum.ChapterTemplateResponse;
import com.example.backend.dto.response.curriculum.ContentItemTemplateResponse;
import com.example.backend.dto.response.curriculum.CurriculumTemplateResponse;
import com.example.backend.dto.response.curriculum.LessonTemplateResponse;
import com.example.backend.dto.response.curriculum.QuizTemplateResponse;
import com.example.backend.service.ClassSectionService;
import com.example.backend.service.CurriculumTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lms/curriculum-templates")
@RequiredArgsConstructor
@Tag(name = "Curriculum Template Management", description = "APIs for curriculum templates and snapshot content")
public class CurriculumTemplateController {
    private final CurriculumTemplateService curriculumTemplateService;
    private final ClassSectionService classSectionService;

    @Operation(summary = "Tao curriculum template")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping
    public ResponseEntity<CurriculumTemplateResponse> createTemplate(@Valid @RequestBody CurriculumTemplateRequest request) {
        CurriculumTemplateResponse response = curriculumTemplateService.createTemplate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Cap nhat curriculum template")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PutMapping("/{id}")
    public ResponseEntity<CurriculumTemplateResponse> updateTemplate(
            @PathVariable Integer id,
            @Valid @RequestBody CurriculumTemplateRequest request
    ) {
        return ResponseEntity.ok(curriculumTemplateService.updateTemplate(id, request));
    }

    @Operation(summary = "Lay curriculum template theo id")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/{id}")
    public ResponseEntity<CurriculumTemplateResponse> getTemplateById(@PathVariable Integer id) {
        return ResponseEntity.ok(curriculumTemplateService.getTemplateById(id));
    }

    @Operation(summary = "Lay danh sach curriculum templates")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping
    public ResponseEntity<List<CurriculumTemplateResponse>> getTemplates(
            @RequestParam(required = false) Integer subjectId,
            @RequestParam(defaultValue = "false") boolean includeChapters
    ) {
        return ResponseEntity.ok(curriculumTemplateService.getTemplates(subjectId, includeChapters));
    }

    @Operation(summary = "Tao chapter cho template")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/{templateId}/chapters")
    public ResponseEntity<ChapterTemplateResponse> createChapter(
            @PathVariable Integer templateId,
            @Valid @RequestBody ChapterTemplateUpsertRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(curriculumTemplateService.createChapter(templateId, request));
    }

    @Operation(summary = "Cap nhat chapter trong template")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PutMapping("/{templateId}/chapters/{chapterId}")
    public ResponseEntity<ChapterTemplateResponse> updateChapter(
            @PathVariable Integer templateId,
            @PathVariable Integer chapterId,
            @Valid @RequestBody ChapterTemplateUpsertRequest request
    ) {
        return ResponseEntity.ok(curriculumTemplateService.updateChapter(templateId, chapterId, request));
    }

    @Operation(summary = "Xoa chapter trong template")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @DeleteMapping("/{templateId}/chapters/{chapterId}")
    public ResponseEntity<Void> deleteChapter(
            @PathVariable Integer templateId,
            @PathVariable Integer chapterId
    ) {
        curriculumTemplateService.deleteChapter(templateId, chapterId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Them content item vao chapter")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/{templateId}/chapters/{chapterId}/content-items")
    public ResponseEntity<ContentItemTemplateResponse> createContentItem(
            @PathVariable Integer templateId,
            @PathVariable Integer chapterId,
            @Valid @RequestBody ContentItemTemplateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(curriculumTemplateService.createContentItem(templateId, chapterId, request));
    }

    @Operation(summary = "Cap nhat content item trong chapter")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PutMapping("/{templateId}/chapters/{chapterId}/content-items/{contentItemId}")
    public ResponseEntity<ContentItemTemplateResponse> updateContentItem(
            @PathVariable Integer templateId,
            @PathVariable Integer chapterId,
            @PathVariable Integer contentItemId,
            @Valid @RequestBody ContentItemTemplateRequest request
    ) {
        return ResponseEntity.ok(
                curriculumTemplateService.updateContentItem(templateId, chapterId, contentItemId, request)
        );
    }

    @Operation(summary = "Xoa content item trong chapter")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @DeleteMapping("/{templateId}/chapters/{chapterId}/content-items/{contentItemId}")
    public ResponseEntity<Void> deleteContentItem(
            @PathVariable Integer templateId,
            @PathVariable Integer chapterId,
            @PathVariable Integer contentItemId
    ) {
        curriculumTemplateService.deleteContentItem(templateId, chapterId, contentItemId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Xoa curriculum template")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Integer id) {
        curriculumTemplateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Tao class section truc tiep tu template")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/{templateId}/class-sections")
    public ResponseEntity<ClassSectionResponse> createClassSectionFromTemplate(
            @PathVariable Integer templateId,
            @Valid @RequestBody ClassSectionRequest request
    ) {
        ClassSectionResponse response = classSectionService.createFromTemplate(templateId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── Lesson Template CRUD ──────────────────────────────────────────────────

    @Operation(summary = "Tao lesson template")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/lesson-templates")
    public ResponseEntity<LessonTemplateResponse> createLessonTemplate(
            @Valid @RequestBody LessonTemplateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(curriculumTemplateService.createLessonTemplate(request));
    }

    @Operation(summary = "Lay lesson template theo id")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/lesson-templates/{id}")
    public ResponseEntity<LessonTemplateResponse> getLessonTemplateById(@PathVariable Integer id) {
        return ResponseEntity.ok(curriculumTemplateService.getLessonTemplateById(id));
    }

    @Operation(summary = "Cap nhat lesson template")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PutMapping("/lesson-templates/{id}")
    public ResponseEntity<LessonTemplateResponse> updateLessonTemplate(
            @PathVariable Integer id,
            @Valid @RequestBody LessonTemplateRequest request
    ) {
        return ResponseEntity.ok(curriculumTemplateService.updateLessonTemplate(id, request));
    }

    // ── Quiz Template CRUD ────────────────────────────────────────────────────

    @Operation(summary = "Tao quiz template")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/quiz-templates")
    public ResponseEntity<QuizTemplateResponse> createQuizTemplate(
            @Valid @RequestBody QuizTemplateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(curriculumTemplateService.createQuizTemplate(request));
    }

    @Operation(summary = "Lay quiz template theo id")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/quiz-templates/{id}")
    public ResponseEntity<QuizTemplateResponse> getQuizTemplateById(@PathVariable Integer id) {
        return ResponseEntity.ok(curriculumTemplateService.getQuizTemplateById(id));
    }

    @Operation(summary = "Cap nhat quiz template")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PutMapping("/quiz-templates/{id}")
    public ResponseEntity<QuizTemplateResponse> updateQuizTemplate(
            @PathVariable Integer id,
            @Valid @RequestBody QuizTemplateRequest request
    ) {
        return ResponseEntity.ok(curriculumTemplateService.updateQuizTemplate(id, request));
    }
}
