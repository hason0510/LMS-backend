package com.example.backend.repository;

import com.example.backend.entity.assignment.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Integer> {
    List<Assignment> findByClassSection_IdOrderByDueAtAsc(Integer classSectionId);

    Optional<Assignment> findByIdAndClassSection_Id(Integer assignmentId, Integer classSectionId);
}
