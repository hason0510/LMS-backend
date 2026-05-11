package com.example.backend.entity.quiz;

import com.example.backend.constant.DifficultyLevel;
import com.example.backend.constant.QuizSourceSelectionMode;
import com.example.backend.constant.QuizTagMatchMode;
import com.example.backend.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.List;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", length = 20)
    private DifficultyLevel difficultyLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "tag_match_mode", nullable = false, length = 10)
    private QuizTagMatchMode tagMatchMode = QuizTagMatchMode.ANY;

    @ManyToOne
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne
    @JoinColumn(name = "question_bank_id", nullable = false)
    private QuestionBank questionBank;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "quiz_bank_source_tags",
            joinColumns = @JoinColumn(name = "source_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<QuestionTag> tags;

}
