package com.example.backend.service.impl;

import com.cloudinary.Cloudinary;
import com.example.backend.constant.ResourceType;
import com.example.backend.dto.response.CloudinaryResponse;
import com.example.backend.exception.BusinessException;
import com.example.backend.service.CloudinaryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
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
            if ("video".equalsIgnoreCase(type)) {
                options.put("resource_type", "video");
            } else {
                options.put("resource_type", "auto"); // image, pdf
            }
            Map result = cloudinary.uploader().upload(file.getBytes(), options);
            return CloudinaryResponse.builder()
                    .publicId((String) result.get("public_id"))
                    .url((String) result.get("secure_url"))
                    .type(type)
                    .build();
        } catch (Exception e) {
            throw new BusinessException("Failed to upload " + type);
        }
    }

    @Override
    public void deleteFile(String publicId, ResourceType type) {
        try {
            Map<String, Object> options = new HashMap<>();

            if (type == ResourceType.VIDEO) {
                options.put("resource_type", "video");
            } else if (type == ResourceType.PDF) {
                options.put("resource_type", "raw");
            } else {
                options.put("resource_type", "image");
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
