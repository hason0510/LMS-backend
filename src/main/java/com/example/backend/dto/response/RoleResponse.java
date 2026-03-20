package com.example.backend.dto.response;

import com.example.backend.constant.RoleType;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleResponse {
    private Integer roleID;
    private String roleName;
    private String roleDesc;
    private List<Integer> permissionIds;
}
