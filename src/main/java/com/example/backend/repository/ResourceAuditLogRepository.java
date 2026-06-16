package com.example.backend.repository;

import com.example.backend.entity.resource.ResourceAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceAuditLogRepository extends JpaRepository<ResourceAuditLog, Integer> {
    List<ResourceAuditLog> findTop20ByResource_IdOrderByIdDesc(Integer resourceId);
}
