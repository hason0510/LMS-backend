package com.example.backend.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.backend.constant.ResourceType;
import com.example.backend.dto.response.CloudinaryResponse;
import com.example.backend.exception.BusinessException;
import com.example.backend.service.CloudinaryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryServiceImpl(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    @Transactional
    public CloudinaryResponse uploadFile(MultipartFile file, String fileName, String type) {
        String normalizedType = type == null ? "auto" : type.toLowerCase();
        try {
            Map<String, Object> uploadOptions = buildUploadOptions(fileName, normalizedType);
            Map result = uploadToCloudinary(file, uploadOptions);

            String hlsUrl = null;
            if ("video".equals(normalizedType)) {
                try {
                    Map eagerResult = generateVideoStreaming((String) result.get("public_id"));
                    hlsUrl = extractHlsUrl(eagerResult);
                } catch (Exception streamingError) {
                    log.warn(
                            "Cloudinary uploaded video {} but HLS eager generation failed: {}",
                            fileName,
                            streamingError.getMessage()
                    );
                }
            }

            return CloudinaryResponse.builder()
                    .publicId((String) result.get("public_id"))
                    .url((String) result.get("secure_url"))
                    .type(normalizedType)
                    .hlsUrl(hlsUrl)
                    .build();
        } catch (Exception e) {
            log.error("Cloudinary upload failed for type {} and file {}", normalizedType, fileName, e);
            throw new BusinessException("Failed to upload " + type + ": " + e.getMessage());
        }
    }

    private Map<String, Object> buildUploadOptions(String fileName, String normalizedType) {
        Map<String, Object> options = new HashMap<>();
        options.put("public_id", fileName);

        switch (normalizedType) {
            case "video" -> options.put("resource_type", "video");
            case "audio" -> options.put("resource_type", "video");
            case "raw" -> options.put("resource_type", "raw");
            case "image" -> options.put("resource_type", "image");
            default -> options.put("resource_type", "auto");
        }

        return options;
    }

    private Map uploadToCloudinary(MultipartFile file, Map<String, Object> options) throws Exception {
        return cloudinary.uploader().upload(file.getBytes(), options);
    }

    private Map generateVideoStreaming(String publicId) throws Exception {
        Map<String, Object> options = new HashMap<>();
        options.put("type", "upload");
        options.put("resource_type", "video");
        options.put("eager", Collections.singletonList(
                ObjectUtils.asMap("streaming_profile", "sp_auto", "format", "m3u8")
        ));
        options.put("eager_async", false);
        return cloudinary.uploader().explicit(publicId, options);
    }

    private String extractHlsUrl(Map result) {
        if (result == null) {
            return null;
        }
        List<Map> eager = (List<Map>) result.get("eager");
        if (eager == null || eager.isEmpty()) {
            return null;
        }
        return (String) eager.get(0).get("secure_url");
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
