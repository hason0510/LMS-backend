package com.example.backend.controller;

import com.example.backend.dto.request.MeetingRequest;
import com.example.backend.dto.response.MeetingResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.service.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/lms/meetings")
public class MeetingController {
    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService){
        this.meetingService = meetingService;
    }

    @Operation(summary = "Tạo cuộc họp mới")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<MeetingResponse> createMeeting(@RequestBody MeetingRequest request) {
        return ResponseEntity.ok(meetingService.createMeeting(request));
    }

    @Operation(summary = "Lấy thông tin cuộc họp theo ID")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'TEACHER')")
    @GetMapping("/{id}")
    public ResponseEntity<MeetingResponse> getMeetingById(@PathVariable Integer id) {
        return ResponseEntity.ok(meetingService.getMeetingById(id));
    }

    @Operation(summary = "Lấy danh sách cuộc họp có phân trang")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'TEACHER')")
    @GetMapping
    public ResponseEntity<PageResponse<MeetingResponse>> getAllMeetings(
            @RequestParam(value = "pageNumber", defaultValue = "1", required = false) Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "5", required = false) Integer pageSize
    ) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        PageResponse<MeetingResponse> meetingPage = meetingService.getMeetingPage(pageable);
        return ResponseEntity.ok(meetingPage);
    }

    @Operation(summary = "Cập nhật cuộc họp")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<MeetingResponse> updateMeeting(
            @PathVariable Integer id,
            @RequestBody MeetingRequest request
    ) {
        return ResponseEntity.ok(meetingService.updateMeeting(id, request));
    }

    @Operation(summary = "Xóa cuộc họp")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMeeting(@PathVariable Integer id) {
        meetingService.deleteMeeting(id);
        return ResponseEntity.noContent().build();
    }
}