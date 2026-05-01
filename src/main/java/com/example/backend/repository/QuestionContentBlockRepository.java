package com.example.backend.repository;

import com.example.backend.entity.quiz.QuestionContentBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionContentBlockRepository extends JpaRepository<QuestionContentBlock, Integer> {
    List<QuestionContentBlock> findByBankQuestion_IdOrderByOrderIndexAsc(Integer bankQuestionId);
    List<QuestionContentBlock> findByQuizQuestion_IdOrderByOrderIndexAsc(Integer quizQuestionId);
    void deleteByBankQuestion_Id(Integer bankQuestionId);
    void deleteByQuizQuestion_Id(Integer quizQuestionId);
}
