package com.example.backend.dto.request;
import java.util.List;

import com.example.backend.constant.RoleType;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleRequest {
    private String roleName;
    private String roleDesc;
    private List<Integer> permissionIds;
}
