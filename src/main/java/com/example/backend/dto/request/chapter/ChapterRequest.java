package com.example.backend.dto.request.chapter;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class ChapterRequest {
    private String title;
    private String orderIndex;
    private Integer courseId;
    private String description;
}
