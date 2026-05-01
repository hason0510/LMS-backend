package com.example.backend.repository;

import com.example.backend.entity.quiz.BankQuestionTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankQuestionTagRepository extends JpaRepository<BankQuestionTag, Integer> {
    List<BankQuestionTag> findByBankQuestion_Id(Integer bankQuestionId);

    List<BankQuestionTag> findByTag_Id(Integer tagId);

    Optional<BankQuestionTag> findByBankQuestion_IdAndTag_Id(Integer bankQuestionId, Integer tagId);

    boolean existsByTag_Id(Integer tagId);

    long countByTag_Id(Integer tagId);

    void deleteByBankQuestion_Id(Integer bankQuestionId);

    void deleteByBankQuestion_IdAndTag_Id(Integer bankQuestionId, Integer tagId);
}
