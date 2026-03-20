package com.example.backend.dto.response.chapter;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChapterResponse {
    private Integer id;
    private String title;
    private Integer orderIndex;
    private String courseTitle;
    private String description;
    private Integer courseId;
}
