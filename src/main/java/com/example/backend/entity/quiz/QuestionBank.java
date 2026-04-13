package com.example.backend.entity.quiz;

import com.example.backend.entity.BaseEntity;
import com.example.backend.entity.Subject;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.List;

@Entity
@Table(name = "question_banks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE question_banks SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class QuestionBank extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String description;

    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @OneToMany(mappedBy = "questionBank")
    private List<BankQuestion> questions;

    @OneToMany(mappedBy = "questionBank")
    private List<QuizBankSource> quizSources;

    @OneToMany(mappedBy = "questionBank", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestionBankMember> members;
}
