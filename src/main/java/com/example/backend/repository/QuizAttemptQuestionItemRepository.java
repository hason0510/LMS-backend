package com.example.backend.repository;

import com.example.backend.entity.quiz.QuizAttemptQuestionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizAttemptQuestionItemRepository extends JpaRepository<QuizAttemptQuestionItem, Integer> {
    List<QuizAttemptQuestionItem> findByAttemptQuestion_IdOrderByOrderIndexAsc(Integer attemptQuestionId);
}
