package com.example.backend.dto.response.course;


import com.example.backend.constant.CourseStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponse {
    private Integer id;
    private String title;
    private String description;
    private Integer teacherId;
    private String teacherName;
    private Integer categoryId;
    private String categoryName;
    private String imageUrl;
    private String cloudinaryImageId;
    private String classCode;
    private Double rating;
    private Long reviewCounts;
    @Enumerated(EnumType.STRING)
    private CourseStatus status;
    private Long totalEnrollments;
    private Integer progress; // Learning progress percentage (0-100), only populated in student context
}
