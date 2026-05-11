package com.example.backend.dto.request.classsection;

import com.example.backend.constant.ContentItemType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassContentItemCreateRequest {
    private ContentItemType itemType;
    private Integer orderIndex;
    private Boolean hidden;
    private Boolean locked;
    private LocalDateTime availableFrom;
    private LocalDateTime availableTo;
    private Integer lessonId;
    private Integer quizId;
    private Integer assignmentId;
}
