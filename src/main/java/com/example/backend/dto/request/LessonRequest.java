package com.example.backend.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LessonRequest {
    private String title;
    private String content;
    private String videoUrl;
    private String notes;
}
