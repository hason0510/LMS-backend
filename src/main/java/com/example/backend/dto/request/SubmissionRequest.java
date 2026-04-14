package com.example.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionRequest {
    private String description;
    private String fileUrl;
    private String embedUrl;
    private String cloudinaryId;
    private List<ResourceRequest> resources;
}
