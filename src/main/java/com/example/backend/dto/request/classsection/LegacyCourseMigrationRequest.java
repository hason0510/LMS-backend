package com.example.backend.dto.request.classsection;

import com.example.backend.constant.CourseStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LegacyCourseMigrationRequest {
    @NotNull(message = "Subject id is required for legacy course migration")
    private Integer subjectId;

    private String title;
    private String description;
    private String classCode;
    private CourseStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer teacherId;
    private Integer managerId;
}
