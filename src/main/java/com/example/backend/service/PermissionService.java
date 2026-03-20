package com.example.backend.service;

import com.example.backend.dto.request.PermissionRequest;
import com.example.backend.dto.response.permission.PermissionInfoResponse;
import com.example.backend.entity.Permission;
import java.util.List;

public interface PermissionService {
    PermissionInfoResponse createPermission(PermissionRequest request);
    
    PermissionInfoResponse updatePermission(Integer id, PermissionRequest request);
    
    void deletePermissionById(Integer id);
    
    PermissionInfoResponse getPermissionById(Integer id);
    
    List<PermissionInfoResponse> getAllPermissions();
    
    PermissionInfoResponse convertPermissionToDTO(Permission permission);
    
    Permission getPermissionByName(String name);
}
