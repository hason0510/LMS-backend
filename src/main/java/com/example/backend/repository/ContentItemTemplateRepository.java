package com.example.backend.repository;

import com.example.backend.entity.ContentItemTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContentItemTemplateRepository extends JpaRepository<ContentItemTemplate, Integer> {
}
