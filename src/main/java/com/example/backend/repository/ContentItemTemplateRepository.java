package com.example.backend.repository;

import com.example.backend.entity.template.ContentItemTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContentItemTemplateRepository extends JpaRepository<ContentItemTemplate, Integer> {
    List<ContentItemTemplate> findByChapterTemplate_IdOrderByOrderIndexAsc(Integer chapterTemplateId);

    Optional<ContentItemTemplate> findByIdAndChapterTemplate_Id(Integer contentItemId, Integer chapterTemplateId);
}
