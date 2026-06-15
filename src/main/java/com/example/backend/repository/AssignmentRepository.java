package com.example.backend.repository;

import com.example.backend.entity.assignment.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Integer> {
    List<Assignment> findByDueAtBetween(LocalDateTime start, LocalDateTime end);

    List<Assignment> findByCloseAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT DISTINCT a FROM ClassContentItem i JOIN i.classChapter c JOIN i.assignment a WHERE c.classSection.id = :classSectionId ORDER BY a.dueAt ASC, a.createdDate ASC")
    List<Assignment> findByClassSectionId(@Param("classSectionId") Integer classSectionId);

    /**
     * Count distinct assignments belonging to a class section via ClassContentItem.
     */
    @Query("SELECT COUNT(DISTINCT a.id) FROM ClassContentItem i JOIN i.classChapter c JOIN i.assignment a WHERE c.classSection.id = :classSectionId")
    long countByClassSectionId(@Param("classSectionId") Integer classSectionId);
}
