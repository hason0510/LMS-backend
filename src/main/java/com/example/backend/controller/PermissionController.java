package com.example.backend.controller;

import com.example.backend.entity.Permission;
import com.example.backend.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
/*
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {
    private final PermissionService permissionService;
    
    @Operation(summary = "Tạo quyền mới")  // ← THÊM VÀO ĐÂY
    @PostMapping
    public ResponseEntity<Permission> create(@RequestBody Permission permission) {
        Permission createdPermission = permissionService.create(permission);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPermission);
    }
    
    @Operation(summary = "Lấy tất cả quyền")  // ← THÊM VÀO ĐÂY
    @GetMapping
    public ResponseEntity<List<Permission>> getAll() {
        List<Permission> permissions = permissionService.getAll();
        return ResponseEntity.ok(permissions);
    }
    
    @Operation(summary = "Lấy quyền theo ID")  // ← THÊM VÀO ĐÂY
    @GetMapping("/{id}")
    public ResponseEntity<Permission> getById(@PathVariable Integer id) {
        Permission permission = permissionService.getById(id);
        return ResponseEntity.ok(permission);
    }

    @Operation(summary = "Cập nhật quyền")  // ← THÊM VÀO ĐÂY
    @PutMapping("/{id}")
    public ResponseEntity<Permission> update(
            @PathVariable Integer id,
            @RequestBody Permission permissionDetails) {
        Permission updatedPermission = permissionService.update(id, permissionDetails);
        return ResponseEntity.ok(updatedPermission);
    }

    @Operation(summary = "Xóa quyền")  // ← THÊM VÀO ĐÂY
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        permissionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

 */