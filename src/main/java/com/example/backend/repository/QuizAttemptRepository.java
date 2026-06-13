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

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt,Integer>, JpaSpecificationExecutor<QuizAttempt> {
    List<QuizAttempt> findByStudent_Id(Integer studentId);

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
}


