package com.example.backend.dto.response;

import com.example.backend.constant.ResourceType;
import com.example.backend.constant.ResourceSource;
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
    private String description;
    private String mimeType;
    private Long fileSize;
    private ResourceType type;
    private ResourceSource source;
    private String lessonTitle;
    private Integer lessonId;
    private Integer assignmentId;
    private Integer submissionId;

}
