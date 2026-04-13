package com.example.backend.repository;

import com.example.backend.entity.quiz.QuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizAnswerRepository extends JpaRepository<QuizAnswer,Integer> {
    boolean existsByQuizQuestion_Id(Integer id);
}
