package com.example.backend.utils;

import com.example.backend.constant.ResourceType;
import com.example.backend.exception.BusinessException;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FilenameUtils;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@UtilityClass
public class FileUploadUtil {

    public static final long MAX_IMAGE_SIZE = 20 * 1024 * 1024;  // 20MB
    public static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024; // 100MB
    public static final long MAX_PDF_SIZE   = 20 * 1024 * 1024;  // 20MB
    public static final long MAX_RESOURCE_SIZE = 500L * 1024 * 1024; // 500MB

    public static final Set<String> IMAGE_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp");

    public static final Set<String> VIDEO_EXTENSIONS =
            Set.of("mp4", "mov", "avi", "mkv");

    public static final Set<String> AUDIO_EXTENSIONS =
            Set.of("mp3", "wav", "m4a", "aac", "ogg");

    public static final Set<String> PDF_EXTENSIONS =
            Set.of("pdf");

    public static final Set<String> RESOURCE_EXTENSIONS =
            Set.of(
                    "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "csv",
                    "jpg", "jpeg", "png", "gif", "bmp", "webp",
                    "mp3", "wav", "m4a", "aac", "ogg",
                    "mp4", "mov", "avi", "mkv",
                    "zip", "rar", "7z"
            );

    public static final Map<String, Long> MAX_SIZE_BY_TYPE = Map.of(
            "image", MAX_IMAGE_SIZE,
            "video", MAX_VIDEO_SIZE,
            "audio", MAX_RESOURCE_SIZE,
            "pdf", MAX_PDF_SIZE,
            "resource", MAX_RESOURCE_SIZE
    );

    public static final Map<String, List<String>> ALLOWED_EXTENSIONS_BY_TYPE = Map.of(
            "image", sortedExtensions(IMAGE_EXTENSIONS),
            "video", sortedExtensions(VIDEO_EXTENSIONS),
            "audio", sortedExtensions(AUDIO_EXTENSIONS),
            "pdf", sortedExtensions(PDF_EXTENSIONS),
            "resource", sortedExtensions(RESOURCE_EXTENSIONS)
    );

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
            case "resource", "attachment" -> {
                if (size > MAX_RESOURCE_SIZE) {
                    throw new BusinessException("Attachment size must be <= 500MB");
                }
                if (isInvalidExtension(fileName, RESOURCE_EXTENSIONS)) {
                    throw new BusinessException("Invalid attachment file");
                }
            }
            default -> throw new BusinessException("Unsupported file type");
        }
    }

    public static long getMaxSizeBytes(String type) {
        return MAX_SIZE_BY_TYPE.getOrDefault(normalizePolicyType(type), MAX_RESOURCE_SIZE);
    }

    public static List<String> getAllowedExtensions(String type) {
        return ALLOWED_EXTENSIONS_BY_TYPE.getOrDefault(
                normalizePolicyType(type),
                sortedExtensions(RESOURCE_EXTENSIONS)
        );
    }

    private static String normalizePolicyType(String type) {
        if (type == null || type.isBlank()) {
            return "resource";
        }
        return type.toLowerCase(Locale.ROOT);
    }

    private static List<String> sortedExtensions(Set<String> extensions) {
        return extensions.stream().sorted().toList();
    }

    public static ResourceType resolveResourceType(String fileName) {
        String ext = FilenameUtils.getExtension(fileName);
        if (!org.springframework.util.StringUtils.hasText(ext)) {
            return ResourceType.FILE;
        }
        String normalized = ext.toLowerCase();

        if (VIDEO_EXTENSIONS.contains(normalized)) {
            return ResourceType.VIDEO;
        }
        if (AUDIO_EXTENSIONS.contains(normalized)) {
            return ResourceType.AUDIO;
        }
        if (IMAGE_EXTENSIONS.contains(normalized)) {
            return ResourceType.IMAGE;
        }
        if ("pdf".equals(normalized)) {
            return ResourceType.PDF;
        }
        if (Set.of("doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "csv").contains(normalized)) {
            return ResourceType.DOCX;
        }
        return ResourceType.FILE;
    }

    public static String getFileName(String originalName) {
        String baseName = FilenameUtils.getBaseName(originalName);
        String timestamp = new SimpleDateFormat(DATE_FORMAT).format(System.currentTimeMillis());
        return String.format(FILE_NAME_FORMAT, baseName, timestamp);
    }
}


