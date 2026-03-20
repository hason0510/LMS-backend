package com.example.backend.dto.request.course;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseRatingRequest {
    private Double ratingValue;
    private String description;
}
