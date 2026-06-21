package com.example.backend.repository;

import com.example.backend.constant.AttemptStatus;
import com.example.backend.dto.response.quiz.ClassSectionQuizGradeResponse;
import com.example.backend.dto.response.quiz.ClassSectionStudentQuizResultResponse;
import com.example.backend.entity.quiz.QuizAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt,Integer>, JpaSpecificationExecutor<QuizAttempt> {
    List<QuizAttempt> findByStudent_Id(Integer studentId);

    /** Tất cả lần làm của một học viên trên nhiều content item (dùng cho feed quiz). */
    List<QuizAttempt> findByStudent_IdAndClassContentItem_IdIn(Integer studentId, Collection<Integer> classContentItemIds);

    Optional<QuizAttempt> findByClassContentItem_IdAndStudent_IdAndStatus(Integer classContentItemId, Integer studentId, AttemptStatus status);

    Optional<QuizAttempt> findTopByClassContentItem_IdAndStudent_IdAndStatusOrderByIdDesc(Integer classContentItemId, Integer studentId, AttemptStatus status);

    int countByClassContentItem_IdAndStudent_Id(Integer classContentItemId, Integer studentId);

    boolean existsByQuiz_Id(Integer quizId);

    Page<QuizAttempt> findByClassContentItem_IdAndStatusIn(
            Integer classContentItemId,
            List<AttemptStatus> statuses,
            Pageable pageable
    );

    @Query("SELECT MAX(qa.grade) FROM QuizAttempt qa " +
            "WHERE qa.classContentItem.id = :classContentItemId " +
            "AND qa.student.id = :studentId")
    Integer findMaxGradeByClassContentItemAndStudent(@Param("classContentItemId") Integer classContentItemId,
                                                     @Param("studentId") Integer studentId);

    List<QuizAttempt> findByClassContentItem_IdAndStudent_Id(Integer classContentItemId, Integer studentId);

    @Query("SELECT new com.example.backend.dto.response.quiz.ClassSectionStudentQuizResultResponse(" +
            "qa.quiz.id, qa.quiz.title, qa.classContentItem.id, MAX(qa.grade), " +
            "(MAX(CASE WHEN qa.isPassed = true THEN 1 ELSE 0 END) = 1)) " +
            "FROM QuizAttempt qa " +
            "JOIN qa.classContentItem cci " +
            "JOIN cci.classChapter cc " +
            "WHERE qa.student.id = :studentId " +
            "AND cc.classSection.id = :classSectionId " +
            "AND (qa.status = 'COMPLETED' OR qa.status = 'EXPIRED') " +
            "GROUP BY qa.quiz.id, qa.quiz.title, qa.classContentItem.id " +
            "ORDER BY cc.orderIndex ASC, cci.orderIndex ASC")
    List<ClassSectionStudentQuizResultResponse> findMaxGradesByStudentAndClassSection(
            @Param("studentId") Integer studentId,
            @Param("classSectionId") Integer classSectionId
    );

    @Query("SELECT new com.example.backend.dto.response.quiz.ClassSectionQuizGradeResponse(" +
            "s.id, s.fullName, s.studentNumber, " +
            "qa.quiz.id, qa.quiz.title, qa.classContentItem.id, MAX(qa.grade)) " +
            "FROM QuizAttempt qa " +
            "JOIN qa.student s " +
            "JOIN qa.classContentItem cci " +
            "JOIN cci.classChapter cc " +
            "WHERE cc.classSection.id = :classSectionId " +
            "AND (qa.status = 'COMPLETED' OR qa.status = 'EXPIRED') " +
            "GROUP BY s.id, s.fullName, s.studentNumber, qa.quiz.id, qa.quiz.title, qa.classContentItem.id " +
            "ORDER BY s.studentNumber ASC, cc.orderIndex ASC, cci.orderIndex ASC")
    List<ClassSectionQuizGradeResponse> findMaxGradesByClassSection(@Param("classSectionId") Integer classSectionId);

    /**
     * Aggregate quiz statistics across a class section.
     * Returns a single Object[]: [totalAttempts, averageScore, topScore, distinctStudents]
     * over completed or expired attempts only.
     */
    @Query("SELECT COUNT(qa), COALESCE(AVG(qa.grade), 0), COALESCE(MAX(qa.grade), 0), COUNT(DISTINCT qa.student.id) " +
            "FROM QuizAttempt qa " +
            "JOIN qa.classContentItem cci " +
            "JOIN cci.classChapter cc " +
            "WHERE cc.classSection.id = :classSectionId " +
            "AND (qa.status = 'COMPLETED' OR qa.status = 'EXPIRED')")
    Object[] aggregateClassSectionQuizStats(@Param("classSectionId") Integer classSectionId);

    /**
     * Quiz-level aggregate (per quizId) for a class section. Each row:
     * [quizId, classContentItemId, quizTitle, totalAttempts, distinctStudents, avgGrade, maxGrade,
     *  passedCount, notPassedCount, waitingReviewCount, minPassScore]
     *
     * passedCount counts attempts where isPassed=true and gradingStatus<>NEEDS_REVIEW.
     * waitingReviewCount counts attempts where gradingStatus=NEEDS_REVIEW.
     */
    @Query("SELECT qa.quiz.id, qa.classContentItem.id, qa.quiz.title, " +
            "COUNT(qa), COUNT(DISTINCT qa.student.id), " +
            "COALESCE(AVG(qa.grade), 0), COALESCE(MAX(qa.grade), 0), " +
            "SUM(CASE WHEN qa.isPassed = true AND qa.gradingStatus <> com.example.backend.constant.GradingStatus.NEEDS_REVIEW THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN qa.isPassed = false AND qa.gradingStatus <> com.example.backend.constant.GradingStatus.NEEDS_REVIEW THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN qa.gradingStatus = com.example.backend.constant.GradingStatus.NEEDS_REVIEW THEN 1 ELSE 0 END), " +
            "qa.quiz.minPassScore " +
            "FROM QuizAttempt qa " +
            "JOIN qa.classContentItem cci " +
            "JOIN cci.classChapter cc " +
            "WHERE cc.classSection.id = :classSectionId " +
            "AND (qa.status = 'COMPLETED' OR qa.status = 'EXPIRED') " +
            "GROUP BY qa.quiz.id, qa.classContentItem.id, qa.quiz.title, qa.quiz.minPassScore " +
            "ORDER BY qa.classContentItem.id ASC")
    List<Object[]> aggregateQuizSummariesByClassSection(@Param("classSectionId") Integer classSectionId);

    /**
     * Total attempts pending teacher review across given class section IDs (teacher scope).
     */
    @Query("SELECT COUNT(qa) FROM QuizAttempt qa " +
            "JOIN qa.classContentItem cci " +
            "JOIN cci.classChapter cc " +
            "WHERE cc.classSection.id IN :classSectionIds " +
            "AND qa.gradingStatus = com.example.backend.constant.GradingStatus.NEEDS_REVIEW " +
            "AND (qa.status = 'COMPLETED' OR qa.status = 'EXPIRED')")
    long countPendingQuizReviewsInClassSections(@Param("classSectionIds") java.util.Collection<Integer> classSectionIds);

    @Query("SELECT qa.quiz.id, qa.grade FROM QuizAttempt qa " +
            "JOIN qa.classContentItem cci " +
            "JOIN cci.classChapter cc " +
            "WHERE cc.classSection.id = :classSectionId " +
            "AND (qa.status = 'COMPLETED' OR qa.status = 'EXPIRED') " +
            "AND qa.grade IS NOT NULL")
    List<Object[]> findAllGradesByClassSectionId(@Param("classSectionId") Integer classSectionId);
}

