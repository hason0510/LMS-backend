package com.example.backend.specification;

import com.example.backend.constant.ResourceScopeType;
import com.example.backend.constant.ResourceSource;
import com.example.backend.constant.ResourceStatus;
import com.example.backend.constant.ResourceType;
import com.example.backend.constant.ResourceVisibility;
import com.example.backend.entity.resource.Resource;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.Locale;

public class ResourceSpecification {
    private ResourceSpecification() {
    }

    public static Specification<Resource> visibilityForBrowse(
            boolean isAdmin,
            String currentUsername,
            boolean ownerLibrary,
            boolean includeCurrentScope,
            ResourceScopeType requestedScopeType,
            Integer requestedScopeId,
            boolean canBrowseRequestedScope
    ) {
        if (isAdmin) {
            return alwaysTrue();
        }
        if (ownerLibrary) {
            return (root, query, cb) -> {
                var ownResource = cb.equal(root.get("createdBy"), currentUsername);
                if (includeCurrentScope && canBrowseRequestedScope) {
                    var sharedInRequestedScope = cb.and(
                            cb.equal(root.get("scopeType"), requestedScopeType),
                            cb.equal(root.get("scopeId"), requestedScopeId),
                            cb.equal(root.get("visibility"), ResourceVisibility.SHARED)
                    );
                    return cb.or(ownResource, sharedInRequestedScope);
                }
                return ownResource;
            };
        }
        return (root, query, cb) -> {
            var legacyResource = cb.isNull(root.get("createdBy"));
            var ownResource = cb.equal(root.get("createdBy"), currentUsername);
            var institutionResource = cb.equal(root.get("visibility"), ResourceVisibility.INSTITUTION);
            if (canBrowseRequestedScope) {
                var sharedInRequestedScope = cb.and(
                        cb.equal(root.get("scopeType"), requestedScopeType),
                        cb.equal(root.get("scopeId"), requestedScopeId),
                        cb.equal(root.get("visibility"), ResourceVisibility.SHARED)
                );
                return cb.or(legacyResource, ownResource, institutionResource, sharedInRequestedScope);
            }
            return cb.or(legacyResource, ownResource, institutionResource);
        };
    }

    public static Specification<Resource> createdBy(String username) {
        if (!StringUtils.hasText(username)) {
            return alwaysTrue();
        }
        return (root, query, cb) -> cb.equal(root.get("createdBy"), username);
    }

    public static Specification<Resource> ownerContains(String ownerKeyword) {
        if (!StringUtils.hasText(ownerKeyword)) {
            return alwaysTrue();
        }
        String like = "%" + ownerKeyword.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("createdBy")), like);
    }

    public static Specification<Resource> hasScopeType(ResourceScopeType scopeType) {
        if (scopeType == null) {
            return alwaysTrue();
        }
        return (root, query, cb) -> cb.equal(root.get("scopeType"), scopeType);
    }

    public static Specification<Resource> hasScopeId(Integer scopeId) {
        if (scopeId == null) {
            return alwaysTrue();
        }
        return (root, query, cb) -> cb.equal(root.get("scopeId"), scopeId);
    }

    public static Specification<Resource> hasType(ResourceType type) {
        if (type == null) {
            return alwaysTrue();
        }
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<Resource> hasSource(ResourceSource source) {
        if (source == null) {
            return alwaysTrue();
        }
        return (root, query, cb) -> cb.equal(root.get("source"), source);
    }

    public static Specification<Resource> hasStatus(ResourceStatus status) {
        if (status == null) {
            return alwaysTrue();
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Resource> activeByDefault() {
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get("status")),
                cb.equal(root.get("status"), ResourceStatus.ACTIVE)
        );
    }

    public static Specification<Resource> searchContains(String search) {
        if (!StringUtils.hasText(search)) {
            return alwaysTrue();
        }
        String keyword = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), keyword),
                cb.like(cb.lower(root.get("description")), keyword),
                cb.like(cb.lower(root.get("mimeType")), keyword)
        );
    }

    public static Specification<Resource> alwaysTrue() {
        return (root, query, cb) -> cb.conjunction();
    }
}
