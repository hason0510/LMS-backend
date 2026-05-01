package com.example.backend.entity.quiz;

import com.example.backend.constant.QuestionType;
import com.example.backend.entity.BaseEntity;
import com.example.backend.entity.Resource;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "quiz_question")
@SQLDelete(sql = "UPDATE quiz_question SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class QuizQuestion extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String content;
    private Integer points;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @ManyToOne
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @ManyToOne
    @JoinColumn(name = "source_bank_question_id")
    private BankQuestion sourceBankQuestion;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private QuestionType type;

    @OneToMany(mappedBy = "quizQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizAnswer> answers;

    @OneToMany(mappedBy = "quizQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<QuestionInteractionItem> interactionItems;

    @OneToMany(mappedBy = "quizQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<QuestionContentBlock> contentBlocks;
}
