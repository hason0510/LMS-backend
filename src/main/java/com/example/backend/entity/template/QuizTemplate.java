package com.example.backend.entity.template;

import com.example.backend.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "quiz_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE quiz_templates SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class QuizTemplate extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String description;

    @Column(name = "min_pass_score")
    private Integer minPassScore;

    private Integer timeLimitMinutes;

    private Integer maxAttempts;

    @Column(name = "available_from")
    private LocalDateTime availableFrom;

    @Column(name = "available_to")
    private LocalDateTime availableTo;

    @OneToMany(mappedBy = "quizTemplate")
    private List<ContentItemTemplate> contentItemTemplates;
}
