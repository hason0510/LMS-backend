package com.example.backend.dto.response;

import com.example.backend.constant.ResourceType;
import lombok.*;

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
    private ResourceType type;
    private String lessonTitle;
    private Integer lessonId;

}
