package com.example.backend.repository;

import com.example.backend.constant.ClassMemberRole;
import com.example.backend.entity.ClassMember;
import com.example.backend.entity.ClassSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClassMemberRepository extends JpaRepository<ClassMember, Integer> {
    Optional<ClassMember> findByClassSection_IdAndUser_Id(Integer classSectionId, Integer userId);

    Optional<ClassMember> findByClassSection_IdAndRole(Integer classSectionId, ClassMemberRole role);

    List<ClassMember> findByClassSection_Id(Integer classSectionId);

    boolean existsByClassSection_IdAndUser_Id(Integer classSectionId, Integer userId);

    boolean existsByClassSection_IdAndUser_IdAndRole(Integer classSectionId, Integer userId, ClassMemberRole role);

    boolean existsByClassSection_IdAndUser_IdAndRoleIn(
            Integer classSectionId,
            Integer userId,
            Collection<ClassMemberRole> roles
    );

    void deleteByClassSection_IdAndUser_Id(Integer classSectionId, Integer userId);

    void deleteByClassSection_Id(Integer classSectionId);

    @Query("""
            SELECT cm.classSection
            FROM ClassMember cm
            WHERE cm.user.id = :userId
              AND cm.role = :role
            """)
    List<ClassSection> findClassSectionsByMemberRole(
            @Param("userId") Integer userId,
            @Param("role") ClassMemberRole role
    );
}
