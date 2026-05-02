package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementRequest {
    @NotNull(message = "classSectionId is required")
    private Integer classSectionId;

    @NotBlank(message = "Announcement title is required")
    private String title;

    private String summary;
}
