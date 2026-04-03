package com.example.backend.dto.response.curriculum;

import com.example.backend.constant.ContentItemType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContentItemTemplateResponse {
    private Integer id;
    private ContentItemType itemType;
    private Integer orderIndex;
    private Integer lessonId;
    private String lessonTitle;
    private Integer quizId;
    private String quizTitle;
    private Integer assignmentId;
    private String assignmentTitle;
}
