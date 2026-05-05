package com.example.backend.repository;

import com.example.backend.entity.template.QuizTemplateQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizTemplateQuestionRepository extends JpaRepository<QuizTemplateQuestion, Integer> {
    List<QuizTemplateQuestion> findByQuizTemplate_IdOrderByOrderIndexAscIdAsc(Integer quizTemplateId);
}

