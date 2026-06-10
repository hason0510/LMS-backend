package com.example.backend.repository;

import com.example.backend.entity.assignment.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Integer> {
    List<Assignment> findByDueAtBetween(LocalDateTime start, LocalDateTime end);

    List<Assignment> findByCloseAtBetween(LocalDateTime start, LocalDateTime end);
}
