package com.example.backend.repository;

import com.example.backend.entity.template.CurriculumTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CurriculumTemplateRepository extends JpaRepository<CurriculumTemplate, Integer>, JpaSpecificationExecutor<CurriculumTemplate> {
    List<CurriculumTemplate> findBySubject_Id(Integer subjectId);
}
