package com.example.backend.repository;

import com.example.backend.entity.quiz.QuizAttemptQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizAttemptQuestionRepository extends JpaRepository<QuizAttemptQuestion, Integer> {
    List<QuizAttemptQuestion> findByAttempt_IdOrderByOrderIndexAsc(Integer attemptId);
}

