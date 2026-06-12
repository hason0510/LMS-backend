package com.example.backend.dto.request.classsection;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClassSectionJoinCodeRequest {
    @NotBlank(message = "Class code is required")
    private String classCode;
}
