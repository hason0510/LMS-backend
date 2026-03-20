package com.example.backend.service.impl;

import com.example.backend.dto.request.PermissionRequest;
import com.example.backend.dto.response.permission.PermissionInfoResponse;
import com.example.backend.entity.Permission;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.PermissionRepository;
import com.example.backend.service.PermissionService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

/*
@Service
public class PermissionServiceImpl implements PermissionService {
    private final PermissionRepository permissionRepository;

    public PermissionServiceImpl(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Override
    public PermissionInfoResponse createPermission(PermissionRequest request) {
        Permission permission = new Permission();
        permission.setName(request.getName());
        permission.setApiPath(request.getDescription());
        permission.setMethod(request.getMethod());
        permission.setDescription(request.getDescription());
        
        Permission savedPermission = permissionRepository.save(permission);
        return convertPermissionToDTO(savedPermission);
    }
    

    @Override
    public PermissionInfoResponse updatePermission(Integer id, PermissionRequest request) {
        Permission permission = permissionRepository.findById(id).orElse(null);
        
        if (permission == null) {
            throw new ResourceNotFoundException("Permission not found");
        }
      
        if (request.getName() != null) {
            permission.setName(request.getName());
        }
        if (request.getApiPath() != null) {
            permission.setApiPath(request.getApiPath());
        }
        if (request.getMethod() != null) {
            permission.setMethod(request.getMethod());
        }
        if (request.getDescription() != null) {
            permission.setDescription(request.getDescription());
        }
        
        Permission updatedPermission = permissionRepository.save(permission);
        return convertPermissionToDTO(updatedPermission);
    }

    @Override
    public void deletePermissionById(Integer id) {
        Permission permission = permissionRepository.findById(id).orElse(null);
        if (permission == null) {
            throw new ResourceNotFoundException("Permission not found");
        }
        permissionRepository.deleteById(id);
    }

    @Override
    public PermissionInfoResponse getPermissionById(Integer id) {
        Permission permission = permissionRepository.findById(id).orElse(null);
        if (permission == null) {
            throw new ResourceNotFoundException("Permission not found");
        }
        return convertPermissionToDTO(permission);
    }

    @Override
    public Object getAllPermissions() {
        List<PermissionInfoResponse> permissions = permissionRepository.findAll()
                .stream()
                .map(this::convertPermissionToDTO)
                .collect(Collectors.toList());
        return permissions;
    }

    @Override
    public PermissionInfoResponse convertPermissionToDTO(Permission permission) {
        PermissionInfoResponse response = new PermissionInfoResponse();
        response.setId(permission.getId());
        response.setName(permission.getName());
        response.setApiPath(permission.getApiPath());
        response.setMethod(permission.getMethod());
        response.setDescription(permission.getDescription());
        response.setCreatedDate(permission.getCreatedDate());
        response.setLastModifiedDate(permission.getLastModifiedDate());
        return response;
    }

    @Override
    public Permission getPermissionByName(String name) {
        Permission permission = permissionRepository.findByName(name);
        if (permission == null) {
            throw new ResourceNotFoundException("Permission not found with name: " + name);
        }
        return permission;
    }
}

test

 */
