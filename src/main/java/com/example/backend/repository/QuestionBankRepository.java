package com.example.backend.repository;

import com.example.backend.entity.quiz.QuestionBank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionBankRepository extends JpaRepository<QuestionBank, Integer> {
    List<QuestionBank> findBySubject_Id(Integer subjectId);
}
