package com.example.backend.dto.request.classsection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassContentItemOverrideRequest {
    private Integer orderIndex;
    private Boolean hidden;
    private Boolean locked;
    private java.time.LocalDateTime availableFrom;
    private java.time.LocalDateTime availableTo;
    private Integer lessonId;
    private Integer quizId;
    private Integer assignmentId;
}
