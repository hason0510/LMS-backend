package com.example.backend.repository;

import com.example.backend.entity.ClassChapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassChapterRepository extends JpaRepository<ClassChapter, Integer> {
    List<ClassChapter> findByClassSection_IdOrderByOrderIndexAsc(Integer classSectionId);
}
