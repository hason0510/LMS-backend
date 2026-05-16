package com.example.backend.dto.request;
import com.example.backend.constant.ResourceScopeType;
import com.example.backend.constant.ResourceSource;
import com.example.backend.constant.ResourceStatus;
import com.example.backend.constant.ResourceType;
import com.example.backend.constant.ResourceVisibility;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResourceRequest {
    private String title;
    private String description;
    private String fileUrl;
    private String embedUrl;
    private Integer lessonId;
    private Integer assignmentId;
    private Integer submissionId;
    private ResourceType type;
    private ResourceSource source;
    private ResourceScopeType scopeType;
    private Integer scopeId;
    private ResourceVisibility visibility;
    private ResourceStatus status;
    private String cloudinaryId;
    private String mimeType;
    private Long fileSize;

}
