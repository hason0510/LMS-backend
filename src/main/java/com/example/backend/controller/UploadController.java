package com.example.backend.controller;

import com.example.backend.constant.ResourceType;
import com.example.backend.dto.response.CloudinaryResponse;
import com.example.backend.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/lms")
@RequiredArgsConstructor
public class UploadController {

    private final CloudinaryService cloudinaryService;

    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @PostMapping("/upload/image")
    public ResponseEntity<CloudinaryResponse> uploadImage(
            @RequestPart MultipartFile file
    ) {
        CloudinaryResponse response =
                cloudinaryService.uploadEditorImage(file);
        return ResponseEntity.ok(response);
    }

}