package com.example.backend.entity.quiz;

import com.example.backend.constant.QuestionContentBlockType;
import com.example.backend.entity.BaseEntity;
import com.example.backend.entity.Resource;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "question_content_blocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE question_content_blocks SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class QuestionContentBlock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "block_type", nullable = false, length = 20)
    private QuestionContentBlockType blockType;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(length = 50)
    private String language;

    @Column(name = "blank_key", length = 100)
    private String blankKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_question_id")
    private BankQuestion bankQuestion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_question_id")
    private QuizQuestion quizQuestion;
}
