package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class ResourceUploadPolicyResponse {
    private long maxSizeBytes;
    private List<String> allowedExtensions;
    private Map<String, Long> maxSizeBytesByType;
    private Map<String, List<String>> allowedExtensionsByType;
}
