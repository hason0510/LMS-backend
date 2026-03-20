package com.example.backend.service;

import com.example.backend.constant.ResourceType;
import com.example.backend.dto.response.CloudinaryResponse;
import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {
    CloudinaryResponse uploadFile(final MultipartFile file, final String fileName, String type);

    void deleteFile(String publicId, ResourceType type);

    CloudinaryResponse uploadEditorImage(MultipartFile file);
}
