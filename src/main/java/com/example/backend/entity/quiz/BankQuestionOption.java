package com.example.backend.entity.quiz;

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
@Table(name = "bank_question_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE bank_question_options SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class BankQuestionOption extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String content;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Column(columnDefinition = "LONGTEXT")
    private String explanation;

    @ManyToOne
    @JoinColumn(name = "bank_question_id", nullable = false)
    private BankQuestion bankQuestion;
}
