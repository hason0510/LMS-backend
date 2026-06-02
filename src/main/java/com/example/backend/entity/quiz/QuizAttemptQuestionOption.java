package com.example.backend.entity.quiz;

import com.example.backend.entity.BaseEntity;
import com.example.backend.entity.Resource;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "quiz_attempt_question_option")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE quiz_attempt_question_option SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class QuizAttemptQuestionOption extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String content;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Column(columnDefinition = "LONGTEXT")
    private String explanation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_question_id", nullable = false)
    private QuizAttemptQuestion attemptQuestion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_quiz_answer_id")
    private QuizAnswer sourceQuizAnswer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_bank_question_option_id")
    private BankQuestionOption sourceBankQuestionOption;
}

