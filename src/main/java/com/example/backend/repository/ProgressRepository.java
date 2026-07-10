package com.example.backend.repository;

import com.example.backend.entity.Progress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProgressRepository extends JpaRepository<Progress, Integer> {
    @Query("SELECT COUNT(DISTINCT p.classContentItem) FROM Progress p " +
            "WHERE p.student.id = :studentId " +
            "AND p.classContentItem.classChapter.classSection.id = :classSectionId " +
            "AND p.classContentItem.classChapter.isHidden = false " +
            "AND p.classContentItem.isHidden = false AND p.classContentItem.is_deleted = false " +
            "AND p.classContentItem.isLocked = false " +
            "AND (p.classContentItem.availableFrom IS NULL OR p.classContentItem.availableFrom <= :now) " +
            "AND p.classContentItem.itemType IN (com.example.backend.constant.ContentItemType.LESSON, com.example.backend.constant.ContentItemType.QUIZ) " +
            "AND p.isCompleted = true")
    Integer countCompletedClassItemsByStudentAndClassSection(@Param("studentId") Integer studentId,
                                                             @Param("classSectionId") Integer classSectionId,
                                                             @Param("now") LocalDateTime now);

    Optional<Progress> findByStudent_IdAndClassContentItem_Id(Integer studentId, Integer classContentItemId);

    List<Progress> findByStudent_IdAndClassContentItem_IdIn(Integer studentId, Collection<Integer> classContentItemIds);

    @Modifying
    @Query(value = "INSERT INTO progress (student_id, class_content_item_id, is_completed, completed_at) " +
            "VALUES (:studentId, :classContentItemId, true, :completedAt) " +
            "ON DUPLICATE KEY UPDATE is_completed = true, completed_at = COALESCE(completed_at, :completedAt)",
            nativeQuery = true)
    void upsertCompletedProgress(@Param("studentId") Integer studentId,
                                 @Param("classContentItemId") Integer classContentItemId,
                                 @Param("completedAt") LocalDateTime completedAt);

    @Query("""
            SELECT p
            FROM Progress p
            WHERE p.classContentItem.id = :classContentItemId
              AND p.student.id IN :studentIds
              AND p.isCompleted = true
            """)
    List<Progress> findCompletedByClassContentItemAndStudentIds(
            @Param("classContentItemId") Integer classContentItemId,
            @Param("studentIds") Collection<Integer> studentIds
    );
}
