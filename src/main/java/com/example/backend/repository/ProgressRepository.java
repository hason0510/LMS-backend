package com.example.backend.repository;

import com.example.backend.entity.Progress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProgressRepository extends JpaRepository<Progress, Integer> {
    @Query("SELECT COUNT(p) FROM Progress p " +
            "WHERE p.student.id = :studentId " +
            "AND p.chapterItem.chapter.course.id = :courseId " +
            "AND p.isCompleted = true")
    Integer countCompletedItemsByStudentAndCourse(@Param("studentId") Integer studentId,
                                               @Param("courseId") Integer courseId);

    @Query("SELECT p.chapterItem.id FROM Progress p " +
            "WHERE p.student.id = :studentId " +
            "AND p.chapterItem.chapter.id = :chapterId " +
            "AND p.isCompleted = true")
    List<Integer> findCompletedItemIdsByUserAndChapter(Integer studentId, Integer chapterId);

    Optional<Progress> findByStudent_IdAndChapterItem_Id(Integer studentId, Integer chapterItemId);

    @Query("SELECT COUNT(p) FROM Progress p " +
            "WHERE p.student.id = :studentId " +
            "AND p.classContentItem.classChapter.classSection.id = :classSectionId " +
            "AND p.classContentItem.classChapter.isHidden = false " +
            "AND p.classContentItem.isHidden = false " +
            "AND p.classContentItem.itemType IN (com.example.backend.constant.ContentItemType.LESSON, com.example.backend.constant.ContentItemType.QUIZ) " +
            "AND p.isCompleted = true")
    Integer countCompletedClassItemsByStudentAndClassSection(@Param("studentId") Integer studentId,
                                                             @Param("classSectionId") Integer classSectionId);

    Optional<Progress> findByStudent_IdAndClassContentItem_Id(Integer studentId, Integer classContentItemId);

    List<Progress> findByStudent_IdAndClassContentItem_IdIn(Integer studentId, Collection<Integer> classContentItemIds);

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
