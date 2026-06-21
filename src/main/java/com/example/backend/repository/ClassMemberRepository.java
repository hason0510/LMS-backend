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

    /**
     * Aggregate count distinct users across all class sections by role.
     * Used by admin report to count active TAs across the system.
     */
    @Query("SELECT COUNT(DISTINCT u.id) FROM ClassMember cm JOIN cm.user u WHERE cm.role = :role")
    long countDistinctUsersByRole(@Param("role") ClassMemberRole role);

    /**
     * Count members of a class section by role (used to count TA per class).
     */
    @Query("SELECT cm.classSection.id, COUNT(cm) FROM ClassMember cm " +
            "WHERE cm.role = :role AND cm.classSection.id IN :classSectionIds " +
            "GROUP BY cm.classSection.id")
    List<Object[]> countByRoleGroupedByClassSection(
            @Param("role") ClassMemberRole role,
            @Param("classSectionIds") Collection<Integer> classSectionIds
    );

    /**
     * Aggregate of TA users across the system with the classes they assist.
     * Each row: [userId, fullName, gmail, imageUrl, classSectionId, classSectionTitle, classCode]
     */
    @Query("SELECT cm.user.id, cm.user.fullName, cm.user.gmail, cm.user.imageUrl, " +
            "cm.classSection.id, cm.classSection.title, cm.classSection.classCode " +
            "FROM ClassMember cm " +
            "WHERE cm.role = :role " +
            "ORDER BY cm.user.fullName ASC, cm.classSection.id ASC")
    List<Object[]> findAssistantsWithClasses(@Param("role") ClassMemberRole role);

    /**
     * Lớp đang trợ giảng của một tập user (dùng cho danh sách TA phân trang).
     * Mỗi row: [userId, classSectionId, classSectionTitle, classCode]
     */
    @Query("SELECT cm.user.id, cm.classSection.id, cm.classSection.title, cm.classSection.classCode " +
            "FROM ClassMember cm " +
            "WHERE cm.role = :role AND cm.user.id IN :userIds " +
            "ORDER BY cm.classSection.title ASC")
    List<Object[]> findAssistantClassesByUserIds(
            @Param("role") ClassMemberRole role,
            @Param("userIds") Collection<Integer> userIds
    );
}
