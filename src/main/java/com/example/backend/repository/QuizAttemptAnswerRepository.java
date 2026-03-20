package com.example.backend.repository;

import com.example.backend.entity.Quiz;
import com.example.backend.entity.QuizAttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizAttemptAnswerRepository extends JpaRepository<QuizAttemptAnswer,Integer> {
    Optional<QuizAttemptAnswer> findByAttempt_IdAndQuestion_Id(
            Integer attemptId, Integer questionId);

    @Query("SELECT qaa FROM QuizAttemptAnswer qaa " +
           "LEFT JOIN FETCH qaa.selectedAnswers " +
           "WHERE qaa.attempt.id = :attemptId")
    List<QuizAttemptAnswer> findByAttempt_Id(Integer attemptId);
}