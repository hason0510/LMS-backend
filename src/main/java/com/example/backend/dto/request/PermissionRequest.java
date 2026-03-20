package com.example.backend.dto.request;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PermissionRequest {
    private String name;
    private String method;
    private String description;
    private String apiPath;
    private List<Integer> roleIds;
}
