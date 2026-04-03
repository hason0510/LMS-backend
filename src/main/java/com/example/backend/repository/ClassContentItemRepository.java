package com.example.backend.repository;

import com.example.backend.entity.ClassContentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassContentItemRepository extends JpaRepository<ClassContentItem, Integer> {
    List<ClassContentItem> findByClassChapter_IdOrderByOrderIndexAsc(Integer classChapterId);

    @Query("SELECT COUNT(ci) FROM ClassContentItem ci " +
            "WHERE ci.classChapter.classSection.id = :classSectionId " +
            "AND ci.isHidden = false AND ci.is_deleted = false")
    long countTotalItemsByClassSectionId(@Param("classSectionId") Integer classSectionId);

    Optional<ClassContentItem> findByOverrideQuiz_Id(Integer quizId);
}
