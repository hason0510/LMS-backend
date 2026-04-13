package com.example.backend.repository;

import com.example.backend.constant.QuestionBankMemberRole;
import com.example.backend.entity.quiz.QuestionBankMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionBankMemberRepository extends JpaRepository<QuestionBankMember, Integer> {
    Optional<QuestionBankMember> findByQuestionBank_IdAndUser_Id(Integer questionBankId, Integer userId);

    List<QuestionBankMember> findByQuestionBank_Id(Integer questionBankId);

    List<QuestionBankMember> findByUser_Id(Integer userId);

    Optional<QuestionBankMember> findByQuestionBank_IdAndRole(Integer questionBankId, QuestionBankMemberRole role);

    boolean existsByQuestionBank_IdAndUser_Id(Integer questionBankId, Integer userId);

    void deleteByQuestionBank_IdAndUser_Id(Integer questionBankId, Integer userId);
}
