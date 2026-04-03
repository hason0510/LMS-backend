package com.example.backend.repository;

import com.example.backend.entity.ChapterTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChapterTemplateRepository extends JpaRepository<ChapterTemplate, Integer> {
}
