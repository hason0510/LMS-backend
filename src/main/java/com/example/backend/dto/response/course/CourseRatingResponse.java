package com.example.backend.dto.response.course;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseRatingResponse {
    private Integer id;
    private Integer courseId;
    private String courseName;
    private Integer studentId;
    private String studentFullname;
    private String studentUsername;
    private String studentCode;
    private Double ratingValue;
    private String description;
}
