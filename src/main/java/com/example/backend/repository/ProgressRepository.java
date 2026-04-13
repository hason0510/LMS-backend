package com.example.backend.repository;

import com.example.backend.entity.Progress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
            "AND p.isCompleted = true")
    Integer countCompletedClassItemsByStudentAndClassSection(@Param("studentId") Integer studentId,
                                                             @Param("classSectionId") Integer classSectionId);

    Optional<Progress> findByStudent_IdAndClassContentItem_Id(Integer studentId, Integer classContentItemId);
}
