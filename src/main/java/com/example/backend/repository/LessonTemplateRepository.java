package com.example.backend.repository;

import com.example.backend.entity.template.LessonTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LessonTemplateRepository extends JpaRepository<LessonTemplate, Integer> {
}
