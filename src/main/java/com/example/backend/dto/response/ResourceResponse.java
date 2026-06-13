package com.example.backend.dto.response;

import com.example.backend.constant.ResourceScopeType;
import com.example.backend.constant.ResourceStatus;
import com.example.backend.constant.ResourceType;
import com.example.backend.constant.ResourceSource;
import com.example.backend.constant.ResourceVisibility;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResourceResponse {
    private Integer id;
    private String title;
    private String fileUrl;
    private String embedUrl;
    private String cloudinaryId;
    private String hlsUrl;
    private String description;
    private String mimeType;
    private String fileType;
    private Long fileSize;
    private ResourceType type;
    private ResourceSource source;
    private ResourceScopeType scopeType;
    private Integer scopeId;
    private ResourceVisibility visibility;
    private ResourceStatus status;
    private Integer usageCount;
    private LocalDate createdDate;
    private LocalDateTime lastUsedAt;
    private String createdBy;
    private String scopeTargetName;
    private String lessonTitle;
    private Integer lessonId;
    private Integer assignmentId;
    private Integer submissionId;

}
