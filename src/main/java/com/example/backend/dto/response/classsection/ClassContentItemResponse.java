package com.example.backend.dto.response.classsection;

import com.example.backend.constant.ContentItemType;
import com.example.backend.constant.ClassContentAvailabilityStatus;
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
    private String displayTitle;
    private ClassContentAvailabilityStatus availabilityStatus;
    private Boolean accessible;
    private String accessMessageKey;
    private String accessMessage;
}
