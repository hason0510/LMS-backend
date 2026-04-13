package com.example.backend.dto.response.curriculum;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CurriculumTemplateResponse {
    private Integer id;
    private String name;
    private String description;
    private Boolean isDefault;
    private Integer subjectId;
    private String subjectTitle;
    private Integer categoryId;
    private String categoryTitle;
    private List<ChapterTemplateResponse> chapters;
}
