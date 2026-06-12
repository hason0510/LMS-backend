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
    List<Enrollment> findByCourseIdAndStudent_IdIn(
            Integer courseId,
            List<Integer> studentIds
    );

    List<Enrollment> findByClassSection_IdAndStudent_IdIn(
            Integer classSectionId,
            List<Integer> studentIds
    );

    Page<Enrollment> findByApprovalStatus(EnrollmentStatus approvalStatus, Pageable pageable);

    List <Enrollment> findByCourseIdAndApprovalStatus(Integer courseId, EnrollmentStatus approvalStatus);

    Page<Enrollment> findByCourseIdAndApprovalStatus(Integer courseId, EnrollmentStatus enrollmentStatus, Pageable pageable);

    Enrollment findByStudent_IdAndCourseId(Integer studentId, Integer courseId);

    Enrollment findByStudent_IdAndCourseIdAndApprovalStatus(Integer studentId, Integer courseId, EnrollmentStatus approvalStatus);

    Enrollment findByStudent_IdAndClassSection_Id(Integer studentId, Integer classSectionId);

    Enrollment findByStudent_IdAndClassSection_IdAndApprovalStatus(Integer studentId, Integer classSectionId, EnrollmentStatus approvalStatus);

    boolean existsByStudent_IdAndCourseIdAndApprovalStatus(Integer studentId, Integer courseId, EnrollmentStatus approvalStatus);

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

    // ── Legacy course-based queries ──

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.courseId = :courseId AND e.approvalStatus = 'APPROVED'")
    Long countApprovedEnrollmentsByCourseId(@Param("courseId") Integer courseId);
}
