package com.example.backend.dto.response.classsection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassChapterResponse {
    private Integer id;
    private String title;
    private String description;
    private Integer orderIndex;
    private Boolean hidden;
    private Boolean locked;
    private java.time.LocalDateTime availableFrom;
    private java.time.LocalDateTime availableTo;
    private List<ClassContentItemResponse> contentItems;
}
