package com.example.backend.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LessonResponse {
    private Integer id;
    private String title;
    private String content;
    private String videoUrl;
    private String notes;
    private List<ResourceResponse> resources;
}
