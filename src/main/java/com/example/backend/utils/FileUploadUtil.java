package com.example.backend.utils;

import com.example.backend.exception.BusinessException;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FilenameUtils;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.util.Set;

@UtilityClass
public class FileUploadUtil {

    public static final long MAX_IMAGE_SIZE = 20 * 1024 * 1024;  // 20MB
    public static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024; // 100MB
    public static final long MAX_PDF_SIZE   = 20 * 1024 * 1024;  // 20MB

    public static final Set<String> IMAGE_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp");

    public static final Set<String> VIDEO_EXTENSIONS =
            Set.of("mp4", "mov", "avi", "mkv");

    public static final Set<String> PDF_EXTENSIONS =
            Set.of("pdf");

    public static final String DATE_FORMAT = "yyyyMMddHHmmss";
    public static final String FILE_NAME_FORMAT = "%s_%s";

    //Check extension theo type
    public static boolean isInvalidExtension(String fileName, Set<String> allowedExtensions) {
        if (fileName == null || fileName.isBlank()) return true;
        String ext = FilenameUtils.getExtension(fileName);
        if (ext.isBlank()) return true;
        return !allowedExtensions.contains(ext.toLowerCase());
    }

    public static void assertAllowed(MultipartFile file, String type) {
        String fileName = file.getOriginalFilename();
        long size = file.getSize();

        switch (type.toLowerCase()) {
            case "image" -> {
                if (size > MAX_IMAGE_SIZE)
                    throw new BusinessException("Image size must be <= 20MB");
                if (isInvalidExtension(fileName, IMAGE_EXTENSIONS))
                    throw new BusinessException("Invalid image file");
            }
            case "video" -> {
                if (size > MAX_VIDEO_SIZE)
                    throw new BusinessException("Video size must be <= 100MB");
                if (isInvalidExtension(fileName, VIDEO_EXTENSIONS))
                    throw new BusinessException("Invalid video file");
            }
            case "pdf" -> {
                if (size > MAX_PDF_SIZE)
                    throw new BusinessException("PDF size must be <= 20MB");
                if (isInvalidExtension(fileName, PDF_EXTENSIONS))
                    throw new BusinessException("Invalid PDF file");
            }
            default -> throw new BusinessException("Unsupported file type");
        }
    }

    public static String getFileName(String originalName) {
        String baseName = FilenameUtils.getBaseName(originalName);
        String timestamp = new SimpleDateFormat(DATE_FORMAT).format(System.currentTimeMillis());
        return String.format(FILE_NAME_FORMAT, baseName, timestamp);
    }
}


