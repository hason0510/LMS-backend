package com.example.backend.entity.template;

import com.example.backend.constant.QuestionInteractionItemRole;
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
@Table(name = "quiz_template_question_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE quiz_template_question_items SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class QuizTemplateQuestionItem extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String content;

    @Column(name = "item_key", length = 100)
    private String itemKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionInteractionItemRole role;

    @Column(name = "correct_match_key", length = 100)
    private String correctMatchKey;

    @Column(name = "correct_order_index")
    private Integer correctOrderIndex;

    @Column(name = "blank_index")
    private Integer blankIndex;

    @Column(name = "accepted_answers", columnDefinition = "LONGTEXT")
    private String acceptedAnswers;

    @Column(name = "blank_type", length = 20)
    private String blankType = "TEXT_INPUT";

    @Column(name = "blank_options", columnDefinition = "LONGTEXT")
    private String blankOptions;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @Column(name = "order_index")
    private Integer orderIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_template_question_id", nullable = false)
    private QuizTemplateQuestion quizTemplateQuestion;
}

