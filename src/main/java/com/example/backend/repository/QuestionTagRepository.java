package com.example.backend.repository;

import com.example.backend.entity.quiz.QuestionTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionTagRepository extends JpaRepository<QuestionTag, Integer> {
    List<QuestionTag> findByQuestionBank_IdOrderByNameAsc(Integer questionBankId);

    Optional<QuestionTag> findByQuestionBank_IdAndNameIgnoreCase(Integer questionBankId, String name);

    @Query("""
            select qt from QuestionTag qt
            where qt.questionBank.id = :questionBankId
            and lower(qt.name) like lower(concat('%', :keyword, '%'))
            order by qt.name asc
            """)
    List<QuestionTag> searchByQuestionBank(@Param("questionBankId") Integer questionBankId, @Param("keyword") String keyword);
}
