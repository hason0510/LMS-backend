package com.example.backend.repository;

import com.example.backend.entity.QuizAnswer;
import com.example.backend.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizAnswerRepository extends JpaRepository<QuizAnswer,Integer> {
    boolean existsByQuizQuestion_Id(Integer id);
}
