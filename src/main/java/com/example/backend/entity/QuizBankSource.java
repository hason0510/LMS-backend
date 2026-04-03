package com.example.backend.entity;

import com.example.backend.constant.DifficultyLevel;
import com.example.backend.constant.QuizSourceSelectionMode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "quiz_bank_sources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE quiz_bank_sources SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class QuizBankSource extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "selection_mode", nullable = false, length = 20)
    private QuizSourceSelectionMode selectionMode;

    @Column(name = "question_count")
    private Integer questionCount;

    @Column(name = "manual_question_ids", columnDefinition = "MEDIUMTEXT")
    private String manualQuestionIds;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", length = 20)
    private DifficultyLevel difficultyLevel;

    @ManyToOne
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne
    @JoinColumn(name = "question_bank_id", nullable = false)
    private QuestionBank questionBank;

    @ManyToOne
    @JoinColumn(name = "tag_id")
    private QuestionTag tag;
}
