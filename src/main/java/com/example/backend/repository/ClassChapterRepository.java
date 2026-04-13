package com.example.backend.repository;

import com.example.backend.entity.ClassChapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassChapterRepository extends JpaRepository<ClassChapter, Integer> {
    List<ClassChapter> findByClassSection_IdOrderByOrderIndexAsc(Integer classSectionId);

    Optional<ClassChapter> findByIdAndClassSection_Id(Integer classChapterId, Integer classSectionId);
}
