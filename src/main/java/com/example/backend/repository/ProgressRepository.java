package com.example.backend.repository;

import com.example.backend.entity.Progress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProgressRepository extends JpaRepository<Progress, Integer> {
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM Progress p " +
            "WHERE p.student.id = :studentId " +
            "AND p.classContentItem.classChapter.classSection.id = :classSectionId " +
            "AND p.classContentItem.classChapter.isHidden = false " +
            "AND p.classContentItem.isHidden = false " +
            "AND p.classContentItem.itemType IN (com.example.backend.constant.ContentItemType.LESSON, com.example.backend.constant.ContentItemType.QUIZ) " +
            "AND p.isCompleted = true")
    Integer countCompletedClassItemsByStudentAndClassSection(@org.springframework.data.repository.query.Param("studentId") Integer studentId,
                                                             @org.springframework.data.repository.query.Param("classSectionId") Integer classSectionId);

    Optional<Progress> findByStudent_IdAndClassContentItem_Id(Integer studentId, Integer classContentItemId);

    List<Progress> findByStudent_IdAndClassContentItem_IdIn(Integer studentId, Collection<Integer> classContentItemIds);

    @org.springframework.data.jpa.repository.Query("""
            SELECT p
            FROM Progress p
            WHERE p.classContentItem.id = :classContentItemId
              AND p.student.id IN :studentIds
              AND p.isCompleted = true
            """)
    List<Progress> findCompletedByClassContentItemAndStudentIds(
            @org.springframework.data.repository.query.Param("classContentItemId") Integer classContentItemId,
            @org.springframework.data.repository.query.Param("studentIds") Collection<Integer> studentIds
    );
}
