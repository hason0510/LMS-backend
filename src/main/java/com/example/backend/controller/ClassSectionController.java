package com.example.backend.controller;

import com.example.backend.dto.request.classsection.ClassSectionRequest;
import com.example.backend.dto.request.classsection.LegacyCourseMigrationRequest;
import com.example.backend.dto.response.classsection.ClassSectionResponse;
import com.example.backend.service.ClassSectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lms/class-sections")
@RequiredArgsConstructor
@Tag(name = "Class Section Management", description = "APIs for the new class section model and legacy migration")
public class ClassSectionController {
    private final ClassSectionService classSectionService;

    @Operation(summary = "Tạo lớp học từ curriculum version")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/from-template/{curriculumVersionId}")
    public ResponseEntity<ClassSectionResponse> createFromTemplate(
            @PathVariable Integer curriculumVersionId,
            @Valid @RequestBody ClassSectionRequest request
    ) {
        ClassSectionResponse response = classSectionService.createFromTemplate(curriculumVersionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Tạo lớp học từ published version mới nhất của template")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/from-template-latest/{templateId}")
    public ResponseEntity<ClassSectionResponse> createFromLatestPublishedVersion(
            @PathVariable Integer templateId,
            @Valid @RequestBody ClassSectionRequest request
    ) {
        ClassSectionResponse response = classSectionService.createFromLatestPublishedVersion(templateId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

   /* @Operation(summary = "Migrate course cũ sang class section mới")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/migrate-from-course/{courseId}")
    public ResponseEntity<ClassSectionResponse> migrateFromLegacyCourse(
            @PathVariable Integer courseId,
            @Valid @RequestBody LegacyCourseMigrationRequest request
    ) {
        ClassSectionResponse response = classSectionService.migrateFromLegacyCourse(courseId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }*/

    @Operation(summary = "Lấy chi tiết class section")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/{id}")
    public ResponseEntity<ClassSectionResponse> getClassSectionById(@PathVariable Integer id) {
        return ResponseEntity.ok(classSectionService.getClassSectionById(id));
    }

    /*@Operation(summary = "Lấy class section đã migrate từ course cũ")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/legacy-course/{courseId}")
    public ResponseEntity<ClassSectionResponse> getClassSectionByLegacyCourse(@PathVariable Integer courseId) {
        return ResponseEntity.ok(classSectionService.getClassSectionByLegacyCourseId(courseId));
    }*/

    @Operation(summary = "Lấy danh sách class sections")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping
    public ResponseEntity<List<ClassSectionResponse>> getClassSections(
            @RequestParam(required = false) Integer teacherId,
            @RequestParam(required = false) Integer subjectId,
            @RequestParam(required = false) Integer curriculumVersionId,
            @RequestParam(defaultValue = "false") boolean includeChapters
    ) {
        return ResponseEntity.ok(
                classSectionService.getClassSections(teacherId, subjectId, curriculumVersionId, includeChapters)
        );
    }
}
