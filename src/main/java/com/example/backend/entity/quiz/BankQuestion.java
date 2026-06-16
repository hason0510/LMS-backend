package com.example.backend.entity.quiz;

import com.example.backend.constant.DifficultyLevel;
import com.example.backend.constant.QuestionType;
import com.example.backend.entity.BaseEntity;
import com.example.backend.entity.resource.Resource;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "bank_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE bank_questions SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class BankQuestion extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String content;

    @Column(columnDefinition = "LONGTEXT")
    private String explanation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", length = 20)
    private DifficultyLevel difficultyLevel;

    @Column(name = "default_points", precision = 10, scale = 2)
    private BigDecimal defaultPoints;

    @ManyToOne
    @JoinColumn(name = "question_bank_id", nullable = false)
    private QuestionBank questionBank;

    @OneToMany(mappedBy = "bankQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<BankQuestionOption> options;

    @OneToMany(mappedBy = "bankQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<QuestionInteractionItem> interactionItems;

    @OneToMany(mappedBy = "bankQuestion")
    private List<BankQuestionTag> tagMappings;

    @OneToMany(mappedBy = "sourceBankQuestion")
    private List<QuizQuestion> quizQuestions;
}
