package com.example.backend.repository;

import com.example.backend.entity.template.QuizTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizTemplateRepository extends JpaRepository<QuizTemplate, Integer> {
}
