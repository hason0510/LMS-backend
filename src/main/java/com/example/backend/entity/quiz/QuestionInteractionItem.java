package com.example.backend.entity.quiz;

import com.example.backend.constant.QuestionInteractionItemRole;
import com.example.backend.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "question_interaction_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE question_interaction_items SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class QuestionInteractionItem extends BaseEntity {
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

    private String fileUrl;
    private String embedUrl;
    private String cloudinaryId;

    @Column(name = "order_index")
    private Integer orderIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_question_id")
    private BankQuestion bankQuestion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_question_id")
    private QuizQuestion quizQuestion;
}
