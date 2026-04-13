package com.example.backend.repository;

import com.example.backend.entity.template.AssignmentTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssignmentTemplateRepository extends JpaRepository<AssignmentTemplate, Integer> {
}
