package com.example.backend.controller;

import com.example.backend.dto.request.curriculum.CurriculumTemplateRequest;
import com.example.backend.dto.request.classsection.ClassSectionRequest;
import com.example.backend.dto.request.curriculum.CurriculumVersionRequest;
import com.example.backend.dto.response.classsection.ClassSectionResponse;
import com.example.backend.dto.response.curriculum.CurriculumTemplateResponse;
import com.example.backend.dto.response.curriculum.CurriculumVersionResponse;
import com.example.backend.service.ClassSectionService;
import com.example.backend.service.CurriculumTemplateService;
import com.example.backend.service.CurriculumVersionService;
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
@RequestMapping("/api/v1/lms/curriculum-templates")
@RequiredArgsConstructor
@Tag(name = "Curriculum Template Management", description = "APIs for the new curriculum template and version model")
public class CurriculumTemplateController {
    private final CurriculumTemplateService curriculumTemplateService;
    private final CurriculumVersionService curriculumVersionService;
    private final ClassSectionService classSectionService;

    @Operation(summary = "Tạo curriculum template")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping
    public ResponseEntity<CurriculumTemplateResponse> createTemplate(@Valid @RequestBody CurriculumTemplateRequest request) {
        CurriculumTemplateResponse response = curriculumTemplateService.createTemplate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Cập nhật curriculum template")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PutMapping("/{id}")
    public ResponseEntity<CurriculumTemplateResponse> updateTemplate(
            @PathVariable Integer id,
            @Valid @RequestBody CurriculumTemplateRequest request
    ) {
        return ResponseEntity.ok(curriculumTemplateService.updateTemplate(id, request));
    }

    @Operation(summary = "Lấy curriculum template theo id")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/{id}")
    public ResponseEntity<CurriculumTemplateResponse> getTemplateById(@PathVariable Integer id) {
        return ResponseEntity.ok(curriculumTemplateService.getTemplateById(id));
    }

    @Operation(summary = "Lấy danh sách curriculum templates")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping
    public ResponseEntity<List<CurriculumTemplateResponse>> getTemplates(
            @RequestParam(required = false) Integer subjectId,
            @RequestParam(defaultValue = "false") boolean includeVersions
    ) {
        return ResponseEntity.ok(curriculumTemplateService.getTemplates(subjectId, includeVersions));
    }

    @Operation(summary = "Tạo version cho template")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/{templateId}/versions")
    public ResponseEntity<CurriculumVersionResponse> createVersion(
            @PathVariable Integer templateId,
            @Valid @RequestBody CurriculumVersionRequest request
    ) {
        CurriculumVersionResponse response = curriculumVersionService.createVersion(templateId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Lấy danh sách version của template")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/{templateId}/versions")
    public ResponseEntity<List<CurriculumVersionResponse>> getVersionsByTemplate(@PathVariable Integer templateId) {
        return ResponseEntity.ok(curriculumVersionService.getVersionsByTemplate(templateId));
    }

    @Operation(summary = "Lấy published version mới nhất của template")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/{templateId}/versions/published")
    public ResponseEntity<CurriculumVersionResponse> getLatestPublishedVersion(@PathVariable Integer templateId) {
        return ResponseEntity.ok(curriculumVersionService.getLatestPublishedVersion(templateId));
    }

    @Operation(summary = "Tạo class section trực tiếp từ published version của template")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/{templateId}/class-sections")
    public ResponseEntity<ClassSectionResponse> createClassSectionFromTemplate(
            @PathVariable Integer templateId,
            @Valid @RequestBody ClassSectionRequest request
    ) {
        ClassSectionResponse response = classSectionService.createFromLatestPublishedVersion(templateId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
