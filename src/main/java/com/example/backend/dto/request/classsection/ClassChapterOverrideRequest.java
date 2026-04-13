package com.example.backend.dto.request.classsection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassChapterOverrideRequest {
    private String title;
    private String description;
    private Integer orderIndex;
    private Boolean hidden;
    private Boolean locked;
    private LocalDateTime availableFrom;
    private LocalDateTime availableTo;
}
