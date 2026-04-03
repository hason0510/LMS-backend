package com.example.backend.entity;

import com.example.backend.constant.DifficultyLevel;
import com.example.backend.constant.QuestionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

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

    private String fileUrl;
    private String embedUrl;
    private String cloudinaryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", length = 20)
    private DifficultyLevel difficultyLevel;

    @Column(name = "default_points")
    private Integer defaultPoints;

    @ManyToOne
    @JoinColumn(name = "question_bank_id", nullable = false)
    private QuestionBank questionBank;

    @ManyToOne
    @JoinColumn(name = "parent_question_id")
    private BankQuestion parentQuestion;

    @OneToMany(mappedBy = "bankQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<BankQuestionOption> options;

    @OneToMany(mappedBy = "bankQuestion")
    private List<BankQuestionTag> tagMappings;

    @OneToMany(mappedBy = "sourceBankQuestion")
    private List<QuizQuestion> quizQuestions;
}
