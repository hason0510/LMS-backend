package com.example.backend.dto.response.course;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentCourseResponse {
    private Integer courseId;
    private List<Integer> userIds;
}
