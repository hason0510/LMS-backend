package com.example.backend.repository;

import com.example.backend.entity.template.QuizTemplateBankSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizTemplateBankSourceRepository extends JpaRepository<QuizTemplateBankSource, Integer> {
    List<QuizTemplateBankSource> findByQuizTemplate_IdOrderByOrderIndexAsc(Integer quizTemplateId);
    long countDistinctByTags_Id(Integer tagId);
}
