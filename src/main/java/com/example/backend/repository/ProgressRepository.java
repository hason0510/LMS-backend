package com.example.backend.repository;

import com.example.backend.entity.StudentChapterItemProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgressRepository extends JpaRepository<StudentChapterItemProgress, Integer> {
    @Query("SELECT COUNT(p) FROM StudentChapterItemProgress p " +
            "WHERE p.student.id = :studentId " +
            "AND p.chapterItem.chapter.course.id = :courseId " +
            "AND p.isCompleted = true")
    Integer countCompletedItemsByStudentAndCourse(@Param("studentId") Integer studentId,
                                               @Param("courseId") Integer courseId);

    @Query("SELECT p.chapterItem.id FROM StudentChapterItemProgress p " +
            "WHERE p.student.id = :studentId " +
            "AND p.chapterItem.chapter.id = :chapterId " +
            "AND p.isCompleted = true")
    List<Integer> findCompletedItemIdsByUserAndChapter(Integer studentId, Integer chapterId);

    Optional<StudentChapterItemProgress> findByStudent_IdAndChapterItem_Id(Integer studentId, Integer chapterItemId);
}
