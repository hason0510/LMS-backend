package com.example.backend.dto.request.classsection;

import com.example.backend.constant.ClassSectionStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassSectionRequest {
    @NotBlank(message = "Class section title is required")
    private String title;
    private String description;
    private String imageUrl;
    private String classCode;
    private ClassSectionStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer teacherId;
    private List<Integer> taIds;
}
