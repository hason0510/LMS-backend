package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementResponse {
    private Integer id;
    private Integer classSectionId;
    private String classSectionTitle;
    private Integer subjectId;
    private String subjectCode;
    private String subjectTitle;
    private String title;
    private String summary;
    private Integer createdByUserId;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
