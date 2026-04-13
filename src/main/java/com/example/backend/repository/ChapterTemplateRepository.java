package com.example.backend.repository;

import com.example.backend.entity.template.ChapterTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterTemplateRepository extends JpaRepository<ChapterTemplate, Integer> {
    List<ChapterTemplate> findByCurriculumTemplate_IdOrderByOrderIndexAsc(Integer curriculumTemplateId);

    Optional<ChapterTemplate> findByIdAndCurriculumTemplate_Id(Integer chapterId, Integer curriculumTemplateId);
}
