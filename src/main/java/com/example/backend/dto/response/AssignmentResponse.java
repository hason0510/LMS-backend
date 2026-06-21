package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentResponse {
    private Integer id;
    private String title;
    private String description;
    private String instruction;
    private Integer maxScore;
    private LocalDateTime dueAt;
    private LocalDateTime closeAt;
    private Integer classSectionId;
    private String classSectionStatus;
    private List<ResourceResponse> resources;
}
