package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "bank_question_tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE bank_question_tags SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class BankQuestionTag extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "bank_question_id", nullable = false)
    private BankQuestion bankQuestion;

    @ManyToOne
    @JoinColumn(name = "tag_id", nullable = false)
    private QuestionTag tag;
}
