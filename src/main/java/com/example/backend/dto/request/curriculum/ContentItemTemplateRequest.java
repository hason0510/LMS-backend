package com.example.backend.dto.request.curriculum;

import com.example.backend.constant.ContentItemType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContentItemTemplateRequest {
    @NotNull(message = "Item type is required")
    private ContentItemType itemType;

    private Integer orderIndex;
    private Integer lessonId;
    private Integer quizId;
    private Integer assignmentId;
}
