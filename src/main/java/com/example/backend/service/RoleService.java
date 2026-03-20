package com.example.backend.service;

import com.example.backend.dto.request.RoleRequest;
import com.example.backend.dto.request.UserRoleRequest;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.RoleResponse;
import org.springframework.data.domain.Pageable;

public interface RoleService {

    RoleResponse createRole(RoleRequest request);
  
    RoleResponse updateRole(RoleRequest request, Integer roleId);
    
    void deleteRole(Integer roleId);
    
    PageResponse<RoleResponse> getPageRole(Pageable pageable);
    
    RoleResponse getRole(Integer roleId);

    void updateUserRole(UserRoleRequest request);
}
