package com.example.backend.dto.request.curriculum;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChapterTemplateUpsertRequest {
    @NotBlank(message = "Chapter title is required")
    private String title;
    private String description;
    private Integer orderIndex;
}
