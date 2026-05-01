package com.example.backend.repository;

import com.example.backend.entity.quiz.QuizAttemptQuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizAttemptQuestionOptionRepository extends JpaRepository<QuizAttemptQuestionOption, Integer> {
    List<QuizAttemptQuestionOption> findByAttemptQuestion_IdOrderByOrderIndexAsc(Integer attemptQuestionId);
}

