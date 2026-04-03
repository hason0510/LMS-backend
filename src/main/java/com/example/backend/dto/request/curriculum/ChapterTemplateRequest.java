package com.example.backend.dto.request.curriculum;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChapterTemplateRequest {
    @NotBlank(message = "Chapter title is required")
    private String title;

    private String description;
    private Integer orderIndex;

    @Valid
    private List<ContentItemTemplateRequest> contentItems;
}
