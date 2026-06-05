package com.example.backend.dto.response.curriculum;

import com.example.backend.dto.response.ResourceResponse;
import lombok.Data;

import java.util.List;

@Data
public class LessonTemplateResponse {
    private Integer id;
    private String title;
    private String content;
    private String videoUrl;
    private String notes;
    private List<ResourceResponse> resources;
}
