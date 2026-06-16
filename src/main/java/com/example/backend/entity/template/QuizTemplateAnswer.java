package com.example.backend.entity.template;

import com.example.backend.entity.BaseEntity;
import com.example.backend.entity.resource.Resource;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "quiz_template_answers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE quiz_template_answers SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class QuizTemplateAnswer extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(columnDefinition = "LONGTEXT")
    private String explanation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_template_question_id", nullable = false)
    private QuizTemplateQuestion quizTemplateQuestion;
}

