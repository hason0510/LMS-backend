package com.example.backend.controller;

import com.example.backend.dto.request.SubjectRequest;
import com.example.backend.dto.response.SubjectResponse;
import com.example.backend.service.SubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lms/subjects")
@RequiredArgsConstructor
@Tag(name = "Subject Management", description = "APIs for managing subjects (courses)")
public class SubjectController {

    private final SubjectService subjectService;

    @Operation(summary = "Lấy tất cả môn học")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    @GetMapping
    public ResponseEntity<List<SubjectResponse>> getAllSubjects() {
        return ResponseEntity.ok(subjectService.getAllSubjects());
    }

    @Operation(summary = "Lấy môn học theo danh mục")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<SubjectResponse>> getSubjectsByCategory(@PathVariable Integer categoryId) {
        return ResponseEntity.ok(subjectService.getSubjectsByCategoryId(categoryId));
    }

    @Operation(summary = "Lấy môn học theo id")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    @GetMapping("/{id}")
    public ResponseEntity<SubjectResponse> getSubjectById(@PathVariable Integer id) {
        return ResponseEntity.ok(subjectService.getSubjectById(id));
    }

    @Operation(summary = "Tạo môn học mới")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<SubjectResponse> createSubject(@RequestBody SubjectRequest request) {
        return ResponseEntity.ok(subjectService.createSubject(request));
    }

    @Operation(summary = "Cập nhật môn học")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<SubjectResponse> updateSubject(@PathVariable Integer id, @RequestBody SubjectRequest request) {
        return ResponseEntity.ok(subjectService.updateSubject(id, request));
    }

    @Operation(summary = "Xóa môn học")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubject(@PathVariable Integer id) {
        subjectService.deleteSubject(id);
        return ResponseEntity.ok().build();
    }
}
