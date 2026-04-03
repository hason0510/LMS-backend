package com.example.backend.repository;

import com.example.backend.entity.QuizBankSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizBankSourceRepository extends JpaRepository<QuizBankSource, Integer> {
    List<QuizBankSource> findByQuiz_IdOrderByOrderIndexAsc(Integer quizId);
}
