package com.example.backend.repository;

import com.example.backend.constant.RoleType;
import com.example.backend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role,Integer> {
    Optional<Role> findFirstByRoleNameOrderByRoleIDAsc(RoleType roleName);

    boolean existsByRoleName(RoleType roleName);
}
