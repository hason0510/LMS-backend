package com.example.backend.entity;

import com.example.backend.constant.QuestionBankScope;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.List;

@Entity
@Table(name = "question_banks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE question_banks SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class QuestionBank extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 20)
    private QuestionBankScope scopeType;

    @ManyToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne
    @JoinColumn(name = "curriculum_version_id")
    private CurriculumVersion curriculumVersion;

    @ManyToOne
    @JoinColumn(name = "class_section_id")
    private ClassSection classSection;

    @OneToMany(mappedBy = "questionBank")
    private List<BankQuestion> questions;

    @OneToMany(mappedBy = "questionBank")
    private List<QuizBankSource> quizSources;
}
