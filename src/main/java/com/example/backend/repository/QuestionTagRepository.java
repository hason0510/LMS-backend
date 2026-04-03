package com.example.backend.repository;

import com.example.backend.entity.QuestionTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionTagRepository extends JpaRepository<QuestionTag, Integer> {
    List<QuestionTag> findBySubject_Id(Integer subjectId);

    Optional<QuestionTag> findBySubject_IdAndNameIgnoreCase(Integer subjectId, String name);
}
