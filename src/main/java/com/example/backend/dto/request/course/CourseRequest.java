package com.example.backend.dto.request.course;

import com.example.backend.constant.CourseStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseRequest {
    private String title;
    private String description;
    private Integer teacherId;
    private Integer categoryId;
    private String classCode;
    private Double rating;
    @Enumerated(EnumType.STRING)
    private CourseStatus status;
}
