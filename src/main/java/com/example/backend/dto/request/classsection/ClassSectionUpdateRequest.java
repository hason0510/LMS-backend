package com.example.backend.dto.request.classsection;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassSectionUpdateRequest {
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
}
