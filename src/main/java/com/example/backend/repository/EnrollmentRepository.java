package com.example.backend.repository;

import com.example.backend.constant.EnrollmentStatus;
import com.example.backend.entity.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment,Integer> {
    List<Enrollment> findByClassSection_IdAndStudent_IdIn(
            Integer classSectionId,
            List<Integer> studentIds
    );

    Page<Enrollment> findByApprovalStatus(EnrollmentStatus approvalStatus, Pageable pageable);

    Enrollment findByStudent_IdAndClassSection_Id(Integer studentId, Integer classSectionId);

    Enrollment findByStudent_IdAndClassSection_IdAndApprovalStatus(Integer studentId, Integer classSectionId, EnrollmentStatus approvalStatus);

    boolean existsByStudent_IdAndClassSection_IdAndApprovalStatus(Integer studentId, Integer classSectionId, EnrollmentStatus approvalStatus);

    Page<Enrollment> findByStudent_IdAndApprovalStatus(Integer studentId, EnrollmentStatus approvalStatus, Pageable pageable);

    List<Enrollment> findByStudent_IdAndClassSection_IdIsNotNull(Integer studentId);

    Page<Enrollment> findByClassSection_IdAndApprovalStatus(Integer classSectionId, EnrollmentStatus approvalStatus, Pageable pageable);

    @Query("""
            SELECT e
            FROM Enrollment e
            JOIN e.student s
            WHERE e.classSection.id = :classSectionId
              AND e.approvalStatus = :approvalStatus
              AND (
                :keyword IS NULL
                OR LOWER(COALESCE(s.fullName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(s.studentNumber, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    Page<Enrollment> searchByClassSection_IdAndApprovalStatus(
            @Param("classSectionId") Integer classSectionId,
            @Param("approvalStatus") EnrollmentStatus approvalStatus,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    List<Enrollment> findByClassSection_IdAndApprovalStatus(Integer classSectionId, EnrollmentStatus approvalStatus);

    List<Enrollment> findByStudent_IdAndApprovalStatus(Integer studentId, EnrollmentStatus approvalStatus);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.classSection.id = :classSectionId AND e.approvalStatus = 'APPROVED'")
    Long countApprovedEnrollmentsByClassSectionId(@Param("classSectionId") Integer classSectionId);

    // ── Simple queries for teacher enrollments (classSectionIds resolved in service layer) ──

    Page<Enrollment> findByClassSection_IdIn(Collection<Integer> classSectionIds, Pageable pageable);

    Page<Enrollment> findByClassSection_IdInAndApprovalStatus(Collection<Integer> classSectionIds, EnrollmentStatus approvalStatus, Pageable pageable);

    Page<Enrollment> findByClassSection_Id(Integer classSectionId, Pageable pageable);

    long countByClassSection_IdAndApprovalStatus(Integer classSectionId, EnrollmentStatus status);

    @Query("SELECT AVG(e.progress) FROM Enrollment e " +
           "WHERE e.classSection.id = :classSectionId AND e.approvalStatus = 'APPROVED'")
    Double averageProgressByClassSection(@Param("classSectionId") Integer classSectionId);

    @Query("SELECT COUNT(e) FROM Enrollment e " +
           "WHERE e.classSection.id = :classSectionId AND e.approvalStatus = 'APPROVED' " +
           "AND e.progress < :threshold")
    long countAtRiskStudents(@Param("classSectionId") Integer classSectionId, 
                             @Param("threshold") int threshold);

    /**
     * Count approved students whose progress is in [lowThreshold, highThreshold) range.
     */
    @Query("SELECT COUNT(e) FROM Enrollment e " +
           "WHERE e.classSection.id = :classSectionId AND e.approvalStatus = 'APPROVED' " +
           "AND e.progress >= :lowThreshold AND e.progress < :highThreshold")
    long countStudentsInProgressRange(@Param("classSectionId") Integer classSectionId,
                                      @Param("lowThreshold") int lowThreshold,
                                      @Param("highThreshold") int highThreshold);

    /**
     * Count approved students whose progress is at or above a threshold.
     */
    @Query("SELECT COUNT(e) FROM Enrollment e " +
           "WHERE e.classSection.id = :classSectionId AND e.approvalStatus = 'APPROVED' " +
           "AND e.progress >= :threshold")
    long countEngagedStudents(@Param("classSectionId") Integer classSectionId,
                              @Param("threshold") int threshold);

    /**
     * Total approved students across all class sections (admin scope).
     */
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.approvalStatus = 'APPROVED' AND e.classSection IS NOT NULL")
    long countTotalApprovedStudents();

    /**
     * Total approved students across given class section IDs (teacher scope).
     */
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.approvalStatus = 'APPROVED' AND e.classSection.id IN :classSectionIds")
    long countTotalApprovedStudentsInClassSections(@Param("classSectionIds") Collection<Integer> classSectionIds);

    /**
     * Total pending requests across given class section IDs (teacher scope).
     */
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.approvalStatus = 'PENDING' AND e.classSection.id IN :classSectionIds")
    long countPendingRequestsInClassSections(@Param("classSectionIds") Collection<Integer> classSectionIds);

    /**
     * Total at-risk students across given class section IDs (teacher scope).
     */
    @Query("SELECT COUNT(e) FROM Enrollment e " +
           "WHERE e.approvalStatus = 'APPROVED' AND e.classSection.id IN :classSectionIds " +
           "AND e.progress < :threshold")
    long countAtRiskStudentsInClassSections(@Param("classSectionIds") Collection<Integer> classSectionIds,
                                            @Param("threshold") int threshold);
}
