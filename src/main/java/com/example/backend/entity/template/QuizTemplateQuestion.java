package com.example.backend.entity.template;

import com.example.backend.constant.QuestionType;
import com.example.backend.entity.BaseEntity;
import com.example.backend.entity.Resource;
import com.example.backend.entity.quiz.BankQuestion;
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
@Table(name = "quiz_template_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE quiz_template_questions SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class QuizTemplateQuestion extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(precision = 10, scale = 2)
    private BigDecimal points;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_bank_question_id")
    private BankQuestion sourceBankQuestion;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private QuestionType type;

    @Column(name = "order_index")
    private Integer orderIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_template_id", nullable = false)
    private QuizTemplate quizTemplate;

    @OneToMany(mappedBy = "quizTemplateQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizTemplateAnswer> answers;

    @OneToMany(mappedBy = "quizTemplateQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<QuizTemplateQuestionItem> items;
}

