package com.example.backend.service.impl;

import com.example.backend.constant.RoleType;
import com.example.backend.dto.request.RoleRequest;
import com.example.backend.dto.request.UserRoleRequest;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.RoleResponse;
import com.example.backend.entity.Permission;
import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.PermissionRepository;
import com.example.backend.repository.RoleRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
public class RoleServiceImpl implements RoleService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    public RoleServiceImpl(RoleRepository roleRepository, 
                          PermissionRepository permissionRepository,
                          UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
    }

    public RoleResponse createRole(RoleRequest request) {
        Role role = new Role();
        if (roleRepository.existsByRoleName(RoleType.valueOf(request.getRoleName()))) {
            throw new DataIntegrityViolationException(
                    "Role name already exists: " + request.getRoleName()
            );
        }
        role.setRoleName(RoleType.valueOf(request.getRoleName()));
        role.setRoleDesc(request.getRoleDesc());
        
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            List<Permission> permissions = request.getPermissionIds().stream()
                    .map(id -> permissionRepository.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id: " + id)))
                    .toList();
            log.info("Adding {} permissions to role", permissions.size());
            role.setPermissions(permissions);
        }
        Role savedRole = roleRepository.save(role);
        log.info("Role created successfully with id: {}", savedRole.getRoleID());
        return convertToDTO(savedRole);
    }

    @Override
    public RoleResponse updateRole(RoleRequest request, Integer roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> {
                    log.error("Role with id {} not found", roleId);
                    return new ResourceNotFoundException("Role not found with id: " + roleId);
                });
        role.setRoleDesc(request.getRoleDesc());
        if (request.getPermissionIds() != null) {
            List<Permission> permissions = request.getPermissionIds().stream()
                    .map(id -> permissionRepository.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id: " + id)))
                    .toList();
            role.setPermissions(permissions);
        }
        roleRepository.save(role);
        return convertToDTO(role);
    }

    @Override
    public void deleteRole(Integer roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));
        role.getPermissions().forEach(permission -> permission.getRoles().remove(role));
        role.getUsers().forEach(user -> {
            Role defaultRole = roleRepository.findByRoleName(RoleType.STUDENT);
            if(defaultRole == null) {
                throw new ResourceNotFoundException("Role with id " + defaultRole.getRoleID() + " not found");
            }
            user.setRole(defaultRole);
            userRepository.save(user);
        });
        roleRepository.delete(role);
    }

    @Override
    public PageResponse<RoleResponse> getPageRole(Pageable pageable) {
        Page<Role> roles = roleRepository.findAll(pageable);
        Page<RoleResponse> rolePage = roles.map(this::convertToDTO);
        return new PageResponse<>(
                rolePage.getNumber() + 1,
                rolePage.getNumberOfElements(),
                rolePage.getTotalPages(),
                rolePage.getContent()
        );
    }

    @Override
    public RoleResponse getRole(Integer roleId) {
        log.info("Get role with id {}", roleId);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));
        return convertToDTO(role);
    }

    @Override
    public void updateUserRole(UserRoleRequest request){
        if(roleRepository.existsByRoleName(RoleType.valueOf(request.getRoleName()))){
            Role roleUpdated = roleRepository.findByRoleName(RoleType.valueOf(request.getRoleName()));
            request.getUserIds().stream()
                    .map(userId -> userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId)))
                    .forEach(user -> {
                        user.setRole(roleUpdated);
                        userRepository.save(user);
                    });
        }
        else throw new ResourceNotFoundException("Role not found with name: " + request.getRoleName());
    }

    public RoleResponse convertToDTO(Role role) {
        RoleResponse response = new RoleResponse();
        response.setRoleName(role.getRoleName().name());
        response.setRoleID(role.getRoleID());
        response.setRoleDesc(role.getRoleDesc());
        List<Integer> permissionIds = role.getPermissions()
                .stream()
                .map(Permission::getId)
                .toList();
        response.setPermissionIds(permissionIds);
        return response;
    }
}
