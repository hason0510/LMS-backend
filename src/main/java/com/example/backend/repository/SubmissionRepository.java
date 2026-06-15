package com.example.backend.repository;

import com.example.backend.constant.SubmissionStatus;
import com.example.backend.entity.assignment.Submission;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Integer>, JpaSpecificationExecutor<Submission> {
    Optional<Submission> findByAssignment_IdAndClassSection_IdAndStudent_Id(
            Integer assignmentId,
            Integer classSectionId,
            Integer studentId
    );

    List<Submission> findByAssignment_IdAndClassSection_IdOrderBySubmissionTimeDesc(Integer assignmentId, Integer classSectionId);

    List<Submission> findByStudent_IdOrderBySubmissionTimeDesc(Integer studentId);

    List<Submission> findByStudent_IdAndClassSection_IdOrderBySubmissionTimeDesc(Integer studentId, Integer classSectionId);

    List<Submission> findByStudent_IdAndClassSection_IdAndStatusOrderBySubmissionTimeDesc(
            Integer studentId,
            Integer classSectionId,
            SubmissionStatus status
    );

    List<Submission> findByStudent_IdAndAssignment_IdIn(Integer studentId, Collection<Integer> assignmentIds);

    @Query("SELECT s.assignment.id, s.status, COUNT(s) FROM Submission s " +
           "WHERE s.classSection.id = :classSectionId AND s.is_deleted = false " +
           "GROUP BY s.assignment.id, s.status")
    List<Object[]> countByClassSectionGroupedByAssignmentAndStatus(@org.springframework.data.repository.query.Param("classSectionId") Integer classSectionId);

    /**
     * Count distinct students who have any submission record for an assignment in a class section.
     * Used to derive notSubmitted = totalStudents - distinctStudentsWithSubmission accurately
     * regardless of how many records exist per student.
     */
    @Query("SELECT s.assignment.id, COUNT(DISTINCT s.student.id) FROM Submission s " +
           "WHERE s.classSection.id = :classSectionId AND s.is_deleted = false " +
           "AND s.status <> com.example.backend.constant.SubmissionStatus.NOT_SUBMITTED " +
           "GROUP BY s.assignment.id")
    List<Object[]> countDistinctActualSubmittersByClassSectionGroupedByAssignment(
            @org.springframework.data.repository.query.Param("classSectionId") Integer classSectionId
    );

    /**
     * Aggregate pending submissions count across given class section IDs.
     */
    @Query("SELECT COUNT(s) FROM Submission s " +
           "WHERE s.classSection.id IN :classSectionIds " +
           "AND s.is_deleted = false " +
           "AND s.status IN (com.example.backend.constant.SubmissionStatus.SUBMITTED, com.example.backend.constant.SubmissionStatus.LATE_SUBMITTED)")
    long countPendingSubmissionsInClassSections(
            @org.springframework.data.repository.query.Param("classSectionIds") Collection<Integer> classSectionIds
    );
}
