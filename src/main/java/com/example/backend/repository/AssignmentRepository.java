package com.example.backend.repository;

import com.example.backend.entity.assignment.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Integer>, JpaSpecificationExecutor<Assignment> {
    List<Assignment> findByClassSection_IdOrderByDueAtAsc(Integer classSectionId);

    Optional<Assignment> findByIdAndClassSection_Id(Integer assignmentId, Integer classSectionId);

    List<Assignment> findByDueAtBetween(LocalDateTime start, LocalDateTime end);

    List<Assignment> findByCloseAtBetween(LocalDateTime start, LocalDateTime end);
}
