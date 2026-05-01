package com.example.backend.repository;

import com.example.backend.entity.quiz.QuizAttemptAnswerItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizAttemptAnswerItemRepository extends JpaRepository<QuizAttemptAnswerItem, Integer> {
    List<QuizAttemptAnswerItem> findByAttemptAnswer_IdOrderByIdAsc(Integer attemptAnswerId);
    void deleteByAttemptAnswer_Id(Integer attemptAnswerId);
}
