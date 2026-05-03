package com.example.backend.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.backend.constant.ResourceType;
import com.example.backend.dto.response.CloudinaryResponse;
import com.example.backend.exception.BusinessException;
import com.example.backend.service.CloudinaryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryServiceImpl(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    @Transactional
    public CloudinaryResponse uploadFile(MultipartFile file, String fileName, String type) {
        try {
            Map<String, Object> options = new HashMap<>();
            options.put("public_id", fileName);
            String normalizedType = type == null ? "auto" : type.toLowerCase();
            switch (normalizedType) {
                case "video" -> options.put("resource_type", "video");
                case "raw" -> options.put("resource_type", "raw");
                case "image" -> options.put("resource_type", "image");
                default -> options.put("resource_type", "auto");
            }

            // Add eager HLS transformation for video
            if ("video".equals(normalizedType)) {
                options.put("eager", Collections.singletonList(
                    ObjectUtils.asMap("streaming_profile", "sp_auto", "format", "m3u8")
                ));
                options.put("eager_async", false);
            }

            Map result = cloudinary.uploader().upload(file.getBytes(), options);

            // Extract HLS URL from eager transformation results
            String hlsUrl = null;
            if ("video".equals(normalizedType)) {
                List<Map> eager = (List<Map>) result.get("eager");
                if (eager != null && !eager.isEmpty()) {
                    hlsUrl = (String) eager.get(0).get("secure_url");
                }
            }

            return CloudinaryResponse.builder()
                    .publicId((String) result.get("public_id"))
                    .url((String) result.get("secure_url"))
                    .type(normalizedType)
                    .hlsUrl(hlsUrl)
                    .build();
        } catch (Exception e) {
            throw new BusinessException("Failed to upload " + type);
        }
    }

    @Override
    public void deleteFile(String publicId, ResourceType type) {
        try {
            Map<String, Object> options = new HashMap<>();

            if (type == ResourceType.VIDEO || type == ResourceType.AUDIO) {
                options.put("resource_type", "video");
            } else if (type == ResourceType.IMAGE) {
                options.put("resource_type", "image");
            } else {
                options.put("resource_type", "raw");
            }
            cloudinary.uploader().destroy(publicId, options);
        } catch (Exception e) {
            throw new BusinessException("Failed to delete file");
        }
    }

    @Override
    public CloudinaryResponse uploadEditorImage(MultipartFile file) {
        try {
            Map<String, Object> options = new HashMap<>();
            options.put("folder", "quiz");
            options.put("resource_type", "image");

            Map result = cloudinary.uploader().upload(file.getBytes(), options);

            return CloudinaryResponse.builder()
                    .publicId((String) result.get("public_id"))
                    .url((String) result.get("secure_url"))
                    .type("image")
                    .build();
        } catch (Exception e) {
            throw new BusinessException("Failed to upload editor image");
        }
    }

}
