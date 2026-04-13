package com.example.backend.repository;

import com.example.backend.entity.ClassContentItem;
import com.example.backend.entity.ClassSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassContentItemRepository extends JpaRepository<ClassContentItem, Integer> {
    List<ClassContentItem> findByClassChapter_IdOrderByOrderIndexAsc(Integer classChapterId);

    Optional<ClassContentItem> findByIdAndClassChapter_Id(Integer classContentItemId, Integer classChapterId);

    Optional<ClassContentItem> findByIdAndClassChapter_ClassSection_Id(Integer classContentItemId, Integer classSectionId);

    @Query("SELECT COUNT(ci) FROM ClassContentItem ci " +
            "WHERE ci.classChapter.classSection.id = :classSectionId " +
            "AND ci.isHidden = false AND ci.is_deleted = false")
    long countTotalItemsByClassSectionId(@Param("classSectionId") Integer classSectionId);

    Optional<ClassContentItem> findByQuiz_Id(Integer quizId);

    Optional<ClassContentItem> findByAssignment_Id(Integer assignmentId);

    @Query("""
            select cci from ClassContentItem cci
            where cci.classChapter.classSection.id = :classSectionId
              and cci.itemType = com.example.backend.constant.ContentItemType.ASSIGNMENT
            order by cci.classChapter.orderIndex asc, cci.orderIndex asc
            """)
    List<ClassContentItem> findAssignmentItemsByClassSectionId(@Param("classSectionId") Integer classSectionId);

    @Query("""
            select (count(cci) > 0) from ClassContentItem cci
            where cci.classChapter.classSection.id = :classSectionId
              and cci.itemType = com.example.backend.constant.ContentItemType.ASSIGNMENT
              and cci.assignment is not null
              and cci.assignment.id = :assignmentId
            """)
    boolean existsAssignmentInClassSection(
            @Param("classSectionId") Integer classSectionId,
            @Param("assignmentId") Integer assignmentId
    );

    @Query("""
            select distinct cci.classChapter.classSection from ClassContentItem cci
            where cci.itemType = com.example.backend.constant.ContentItemType.ASSIGNMENT
              and cci.assignment is not null
              and cci.assignment.id = :assignmentId
            """)
    List<ClassSection> findClassSectionsByAssignmentId(@Param("assignmentId") Integer assignmentId);
}
