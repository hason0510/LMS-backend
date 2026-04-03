package com.example.backend.repository;

import com.example.backend.entity.BankQuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BankQuestionOptionRepository extends JpaRepository<BankQuestionOption, Integer> {
}
