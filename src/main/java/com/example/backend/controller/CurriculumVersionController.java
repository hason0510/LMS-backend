package com.example.backend.controller;

import com.example.backend.dto.request.curriculum.CurriculumVersionRequest;
import com.example.backend.dto.response.curriculum.CurriculumVersionResponse;
import com.example.backend.service.CurriculumVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/lms/curriculum-versions")
@RequiredArgsConstructor
@Tag(name = "Curriculum Version Management", description = "APIs for publishing and updating curriculum versions")
public class CurriculumVersionController {
    private final CurriculumVersionService curriculumVersionService;

    @Operation(summary = "Lấy curriculum version theo id")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/{id}")
    public ResponseEntity<CurriculumVersionResponse> getVersionById(@PathVariable Integer id) {
        return ResponseEntity.ok(curriculumVersionService.getVersionById(id));
    }

    @Operation(summary = "Cập nhật curriculum version")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PutMapping("/{id}")
    public ResponseEntity<CurriculumVersionResponse> updateVersion(
            @PathVariable Integer id,
            @Valid @RequestBody CurriculumVersionRequest request
    ) {
        return ResponseEntity.ok(curriculumVersionService.updateVersion(id, request));
    }

    @Operation(summary = "Publish curriculum version")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/{id}/publish")
    public ResponseEntity<CurriculumVersionResponse> publishVersion(@PathVariable Integer id) {
        return ResponseEntity.ok(curriculumVersionService.publishVersion(id));
    }

    @Operation(summary = "Archive curriculum version")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/{id}/archive")
    public ResponseEntity<CurriculumVersionResponse> archiveVersion(@PathVariable Integer id) {
        return ResponseEntity.ok(curriculumVersionService.archiveVersion(id));
    }
}
