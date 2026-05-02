package com.example.backend.repository;

import com.example.backend.entity.quiz.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QuizRepository extends JpaRepository<Quiz,Integer> {
    List<Quiz> findByAvailableUntilBetween(LocalDateTime start, LocalDateTime end);
}
