package com.example.backend.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PermissionResponse {
    private Integer id;
    private String name;
    private String apiPath;
    private String method;
    private String description;
    private List<String> roleName;
}
