package com.example.backend.repository;

import com.example.backend.constant.DifficultyLevel;
import com.example.backend.entity.BankQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankQuestionRepository extends JpaRepository<BankQuestion, Integer> {
    List<BankQuestion> findByQuestionBank_Id(Integer questionBankId);

    @Query("""
            select distinct bq from BankQuestion bq
            left join bq.tagMappings bqt
            where bq.questionBank.id = :questionBankId
            and (:difficultyLevel is null or bq.difficultyLevel = :difficultyLevel)
            and (:tagId is null or bqt.tag.id = :tagId)
            order by bq.id asc
            """)
    List<BankQuestion> findSelectableQuestions(
            @Param("questionBankId") Integer questionBankId,
            @Param("difficultyLevel") DifficultyLevel difficultyLevel,
            @Param("tagId") Integer tagId
    );
}
