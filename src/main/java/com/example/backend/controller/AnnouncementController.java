package com.example.backend.controller;

import com.example.backend.dto.request.AnnouncementRequest;
import com.example.backend.dto.response.AnnouncementResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/lms/announcements")
@RequiredArgsConstructor
public class AnnouncementController {
    private final AnnouncementService announcementService;

    @Operation(summary = "Create announcement")
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<AnnouncementResponse> createAnnouncement(@Valid @RequestBody AnnouncementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(announcementService.createAnnouncement(request));
    }

    @Operation(summary = "Update announcement")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}")
    public ResponseEntity<AnnouncementResponse> updateAnnouncement(
            @PathVariable Integer id,
            @Valid @RequestBody AnnouncementRequest request
    ) {
        return ResponseEntity.ok(announcementService.updateAnnouncement(id, request));
    }

    @Operation(summary = "Get announcement detail")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<AnnouncementResponse> getAnnouncementById(@PathVariable Integer id) {
        return ResponseEntity.ok(announcementService.getAnnouncementById(id));
    }

    @Operation(summary = "Get announcements")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<PageResponse<AnnouncementResponse>> getAnnouncements(
            @RequestParam(value = "classSectionId", required = false) Integer classSectionId,
            @RequestParam(value = "subjectId", required = false) Integer subjectId,
            @RequestParam(value = "subjectKeyword", required = false) String subjectKeyword,
            @RequestParam(value = "classTitle", required = false) String classTitle,
            @RequestParam(value = "contentKeyword", required = false) String contentKeyword,
            @RequestParam(value = "sort", defaultValue = "DESC", required = false) String sort,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(value = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "pageNumber", defaultValue = "1", required = false) Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "10", required = false) Integer pageSize
    ) {
        return ResponseEntity.ok(announcementService.getAnnouncements(
                classSectionId,
                subjectId,
                subjectKeyword,
                classTitle,
                contentKeyword,
                sort,
                date,
                dateFrom,
                dateTo,
                search,
                pageNumber,
                pageSize
        ));
    }

    @Operation(summary = "Delete announcement")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAnnouncement(@PathVariable Integer id) {
        announcementService.deleteAnnouncement(id);
        return ResponseEntity.noContent().build();
    }
}
