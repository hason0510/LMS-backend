package com.example.backend.dto.request.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchPermissionRequest {
    private String name;
    private String apiPath;
    private String method;
    private String roleName;
    private String description;
    private String module;
    private Boolean active;
    private String methodType;
    private Boolean isPublic;
    private String sortBy;
    private String sortDirection;
}
