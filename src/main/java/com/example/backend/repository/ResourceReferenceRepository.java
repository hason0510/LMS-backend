package com.example.backend.repository;

import com.example.backend.entity.resource.ResourceReference;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ResourceReferenceRepository extends JpaRepository<ResourceReference, Integer> {
    List<ResourceReference> findByResource_IdOrderByIdDesc(Integer resourceId);
    List<ResourceReference> findByEntityTypeAndEntityIdAndFieldNameOrderByIdDesc(String entityType, Integer entityId, String fieldName);
    boolean existsByResource_IdAndEntityTypeAndEntityIdAndFieldName(Integer resourceId, String entityType, Integer entityId, String fieldName);

    @Modifying
    @Transactional
    @Query(value = "UPDATE resource_references SET is_deleted = true WHERE resource_id = ?1 AND is_deleted = false", nativeQuery = true)
    void softDeleteAllActiveByResourceId(Integer resourceId);

    @Modifying
    @Transactional
    @Query(
            value = "UPDATE resource_references SET is_deleted = true WHERE entity_type = ?1 AND entity_id = ?2 AND field_name = ?3 AND is_deleted = false",
            nativeQuery = true
    )
    void softDeleteByEntity(String entityType, Integer entityId, String fieldName);

    @Modifying
    @Transactional
    @Query(
            value = "UPDATE resource_references SET is_deleted = true WHERE entity_type = ?1 AND entity_id = ?2 AND field_name = ?3 AND resource_id = ?4 AND is_deleted = false",
            nativeQuery = true
    )
    void softDeleteByEntityAndResource(String entityType, Integer entityId, String fieldName, Integer resourceId);
}
