package com.example.backend.dto.request;

import com.example.backend.constant.ResourceScopeType;
import com.example.backend.constant.ResourceSource;
import com.example.backend.constant.ResourceStatus;
import com.example.backend.constant.ResourceType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResourceSearchRequest {
    private ResourceScopeType scopeType;
    private Integer scopeId;
    private ResourceType type;
    private ResourceSource source;
    private ResourceStatus status;
    private String search;
    private String owner;
    private Boolean createdByMe;
    private Boolean ownerLibrary;
    private Boolean includeCurrentScope;
    private Boolean recent;
    private String sortBy;
}
