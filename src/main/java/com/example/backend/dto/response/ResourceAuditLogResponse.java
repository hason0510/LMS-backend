package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResourceAuditLogResponse {
    private Integer id;
    private Integer resourceId;
    private String actionType;
    private String actorUsername;
    private String summary;
    private LocalDateTime createdDate;
}
