package com.example.backend.dto.request.curriculum;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LessonTemplateRequest {
    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    private String content;

    private String videoUrl;

    private String notes;
}
