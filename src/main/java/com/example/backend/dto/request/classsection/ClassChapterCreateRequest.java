package com.example.backend.dto.request.classsection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassChapterCreateRequest {
    private String title;
    private String description;
    private Integer orderIndex;
    private Boolean hidden;
}
