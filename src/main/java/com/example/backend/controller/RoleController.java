package com.example.backend.controller;

import com.example.backend.dto.request.RoleRequest;
import com.example.backend.dto.request.UserRoleRequest;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.RoleResponse;
import com.example.backend.dto.response.user.UserInfoResponse;
import com.example.backend.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/lms/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @Operation(summary = "Tạo vai trò mới")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoleResponse> createRole(@RequestBody RoleRequest request) {
        RoleResponse createdRole = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRole);
    }

    @Operation(summary = "Cập nhật vai trò")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoleResponse> updateRole(
            @PathVariable Integer id, 
             @RequestBody RoleRequest request) {
        RoleResponse updatedRole = roleService.updateRole(request, id);
        return ResponseEntity.ok(updatedRole);
    }

    @Operation(summary = "Lấy thông tin vai trò theo ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoleResponse> getRole(@PathVariable Integer id) {
        RoleResponse role = roleService.getRole(id);
        return ResponseEntity.ok(role);
    }

    @Operation(summary = "Xóa vai trò")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteRole(@PathVariable Integer id) {
        roleService.deleteRole(id);
        Map<String, String> response = Map.of("message", "Role deleted successfully");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cập nhật vai trò cho người dùng")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/update-users")
    public ResponseEntity<String> updateUserRole(@RequestBody UserRoleRequest request) {
        roleService.updateUserRole(request);
        return ResponseEntity.ok("Role updated successfully");
    }

    @Operation(summary = "Lấy danh sách vai trò có phân trang")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<RoleResponse>> getPageRole(@RequestParam(value = "pageNumber", defaultValue = "1", required = false) Integer pageNumber,
                                                                   @RequestParam(value = "pageSize", defaultValue = "3", required = false) Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        PageResponse<RoleResponse> rolePage = roleService.getPageRole(pageable);
        return ResponseEntity.ok(rolePage);
    }
}