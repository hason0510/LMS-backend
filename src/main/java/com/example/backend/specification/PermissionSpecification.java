package com.example.backend.specification;

import com.example.backend.constant.RoleType;
import com.example.backend.entity.Permission;
import com.example.backend.entity.Role;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public class PermissionSpecification {

    public static Specification<Permission> likeName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }

    public static Specification<Permission> hasMethod(String method) {
        return (root, query, cb) -> {
            if (method == null || method.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("method"), method.toUpperCase());
        };
    }

    public static Specification<Permission> likeDescription(String description) {
        return (root, query, cb) -> {
            if (description == null || description.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("description")), "%" + description.toLowerCase() + "%");
        };
    }

    public static Specification<Permission> likeApiPath(String apiPath) {
        return (root, query, cb) -> {
            if (apiPath == null || apiPath.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("apiPath")), "%" + apiPath.toLowerCase() + "%");
        };
    }

    public static Specification<Permission> hasRole(String roleName) {
        return (root, query, cb) -> {
            if (roleName == null || roleName.trim().isEmpty()) {
                return cb.conjunction();
            }
            Join<Permission, Role> roleJoin = root.join("roles", JoinType.INNER);
            return cb.equal(cb.lower(roleJoin.get("name")), roleName.toLowerCase());
        };
    }

    public static Specification<Permission> hasRoleType(RoleType roleType) {
        return (root, query, cb) -> {
            if (roleType == null) {
                return cb.conjunction();
            }
            Join<Permission, Role> roleJoin = root.join("roles", JoinType.INNER);
            return cb.equal(roleJoin.get("roleType"), roleType);
        };
    }

    public static Specification<Permission> isActive(Boolean active) {
        return (root, query, cb) -> {
            if (active == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("active"), active);
        };
    }

    public static Specification<Permission> hasModule(String module) {
        return (root, query, cb) -> {
            if (module == null || module.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(cb.lower(root.get("module")), module.toLowerCase());
        };
    }

    public static Specification<Permission> isPublicPermission() {
        return (root, query, cb) -> cb.isTrue(root.get("isPublic"));
    }

    public static Specification<Permission> isProtectedPermission() {
        return (root, query, cb) -> cb.isFalse(root.get("isPublic"));
    }

    public static Specification<Permission> forMethodType(String methodType) {
        return (root, query, cb) -> {
            if (methodType == null || methodType.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("methodType"), methodType);
        };
    }
}
