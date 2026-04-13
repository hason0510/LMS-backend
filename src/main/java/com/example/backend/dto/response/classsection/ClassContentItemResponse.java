package com.example.backend.dto.response.classsection;

import com.example.backend.constant.ContentItemType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassContentItemResponse {
    private Integer id;
    private Integer classChapterId;
    private Integer contentItemTemplateId;
    private ContentItemType itemType;
    private String title;
    private Integer orderIndex;
    private Boolean hidden;
    private Boolean locked;
    private java.time.LocalDateTime availableFrom;
    private java.time.LocalDateTime availableTo;
    private Integer lessonId;
    private Integer quizId;
    private Integer assignmentId;
}
