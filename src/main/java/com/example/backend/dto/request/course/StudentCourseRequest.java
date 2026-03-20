package com.example.backend.dto.request.course;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentCourseRequest {
    List<Integer> studentIds;
}
