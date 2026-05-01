package com.example.backend.repository;

import com.example.backend.entity.quiz.QuestionInteractionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionInteractionItemRepository extends JpaRepository<QuestionInteractionItem, Integer> {
    List<QuestionInteractionItem> findByBankQuestion_IdOrderByOrderIndexAsc(Integer bankQuestionId);
    List<QuestionInteractionItem> findByQuizQuestion_IdOrderByOrderIndexAsc(Integer quizQuestionId);
}
