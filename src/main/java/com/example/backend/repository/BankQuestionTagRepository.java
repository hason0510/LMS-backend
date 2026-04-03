package com.example.backend.repository;

import com.example.backend.entity.BankQuestionTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankQuestionTagRepository extends JpaRepository<BankQuestionTag, Integer> {
    List<BankQuestionTag> findByBankQuestion_Id(Integer bankQuestionId);

    void deleteByBankQuestion_Id(Integer bankQuestionId);
}
