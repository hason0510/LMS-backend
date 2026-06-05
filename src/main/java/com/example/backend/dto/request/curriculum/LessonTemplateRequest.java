package com.example.backend.dto.request.curriculum;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class LessonTemplateRequest {
    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    private String content;

    private String videoUrl;

    private String notes;

    private List<Integer> resourceIds;
}
