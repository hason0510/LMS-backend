package com.example.backend.dto.response.curriculum;

import lombok.Data;

@Data
public class LessonTemplateResponse {
    private Integer id;
    private String title;
    private String content;
    private String videoUrl;
    private String notes;
}
