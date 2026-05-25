package com.example.backend.repository;

import com.example.backend.constant.SubmissionStatus;
import com.example.backend.entity.assignment.Submission;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
