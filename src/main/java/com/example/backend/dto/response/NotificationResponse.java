package com.example.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Integer id;
    private String title;
    private String message;
    private String description;
    private String summary;
    private String type;
    private String actionUrl;
    private Integer classSectionId;
    private String classSectionTitle;
    private String referenceType;
    private Integer referenceId;
    @JsonProperty("isRead")
    private boolean readStatus;
    @JsonProperty("time")
    private LocalDateTime createdAt;

}
