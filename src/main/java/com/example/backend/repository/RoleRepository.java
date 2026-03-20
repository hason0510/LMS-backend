package com.example.backend.repository;

import com.example.backend.constant.RoleType;
import com.example.backend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role,Integer> {
    Role findByRoleName(RoleType roleName);

    boolean existsByRoleName(RoleType roleName);
}
