package com.example.backend.repository;

import com.example.backend.entity.ClassSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassSectionRepository extends JpaRepository<ClassSection, Integer>, JpaSpecificationExecutor<ClassSection> {
    boolean existsByClassCode(String classCode);
    Optional<ClassSection> findByClassCode(String classCode);

    List<ClassSection> findByCurriculumTemplate_Id(Integer curriculumTemplateId);

    List<ClassSection> findByTeacher_Id(Integer teacherId);

    List<ClassSection> findBySubject_Id(Integer subjectId);

    /**
     * Aggregate count by status for admin report (PUBLIC/PRIVATE/ARCHIVED).
     */
    @Query("SELECT cs.status, COUNT(cs) FROM ClassSection cs " +
           "GROUP BY cs.status")
    List<Object[]> countGroupByStatus();

    /**
     * Aggregate teacher load (classCount + studentCount) in a single query.
     * Returns Object[]: [teacherId, teacherFullName, classCount, studentCount]
     * studentCount counts approved enrollments only.
     */
    @Query("SELECT cs.teacher.id, cs.teacher.fullName, COUNT(DISTINCT cs.id), " +
           "COALESCE(SUM(CASE WHEN e.approvalStatus = com.example.backend.constant.EnrollmentStatus.APPROVED THEN 1 ELSE 0 END), 0) " +
           "FROM ClassSection cs " +
           "LEFT JOIN cs.enrollments e " +
           "WHERE cs.teacher IS NOT NULL " +
           "GROUP BY cs.teacher.id, cs.teacher.fullName " +
           "ORDER BY COUNT(DISTINCT cs.id) DESC")
    List<Object[]> findTeacherLoadProjection();

    /**
     * Aggregate subject load by subject (subjectId, subjectTitle, classCount).
     */
    @Query("SELECT cs.subject.id, cs.subject.title, COUNT(cs) " +
           "FROM ClassSection cs " +
           "WHERE cs.subject IS NOT NULL " +
           "GROUP BY cs.subject.id, cs.subject.title " +
           "ORDER BY COUNT(cs) DESC")
    List<Object[]> findSubjectLoadProjection();

    /**
     * Top classes by approved enrollment count.
     * Returns Object[]: [classSectionId, title, classCode, status, subjectTitle, teacherId, teacherName, totalEnrollments]
     */
    @Query("SELECT cs.id, cs.title, cs.classCode, cs.status, " +
           "       (CASE WHEN cs.subject IS NULL THEN '' ELSE cs.subject.title END), " +
           "       (CASE WHEN cs.teacher IS NULL THEN NULL ELSE cs.teacher.id END), " +
           "       (CASE WHEN cs.teacher IS NULL THEN '' ELSE cs.teacher.fullName END), " +
           "       COALESCE(SUM(CASE WHEN e.approvalStatus = com.example.backend.constant.EnrollmentStatus.APPROVED THEN 1 ELSE 0 END), 0) AS approvedCount " +
           "FROM ClassSection cs " +
           "LEFT JOIN cs.enrollments e " +
           "GROUP BY cs.id, cs.title, cs.classCode, cs.status, cs.subject.title, cs.teacher.id, cs.teacher.fullName " +
           "ORDER BY approvedCount DESC, cs.id DESC")
    List<Object[]> findTopClassesByApprovedEnrollment(org.springframework.data.domain.Pageable pageable);

    /**
     * Distinct teacher count (excluding null).
     */
    @Query("SELECT COUNT(DISTINCT cs.teacher.id) FROM ClassSection cs WHERE cs.teacher IS NOT NULL")
    long countDistinctTeachers();
}
