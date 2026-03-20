package com.example.backend.dto.request;
import com.example.backend.constant.ResourceSource;
import com.example.backend.constant.ResourceType;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResourceRequest {
    private String title;
    private String description;
    private String embedUrl;
    private Integer lessonId;
    private ResourceType type;
    private ResourceSource source;
    private String cloudinaryId;

}
