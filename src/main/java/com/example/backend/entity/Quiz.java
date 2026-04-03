package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name= "quiz")
@SQLDelete(sql = "UPDATE quiz SET is_deleted = true WHERE id = ?")
@SQLRestriction(value = "is_deleted = false")
public class Quiz extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String title;
    private String description;
    @Column(name = "min_pass_score")
    private Integer minPassScore;
    private Integer timeLimitMinutes;
    private Integer maxAttempts;
    private LocalDateTime availableFrom;
    private LocalDateTime availableUntil;
    @OneToMany(mappedBy = "quiz")
    private List<QuizQuestion> questions;

    @OneToMany(mappedBy = "quiz")
    private List<QuizAttempt> attempts;

    @ManyToOne
    @JoinColumn(name = "class_section_id")
    private ClassSection classSection;

    @OneToMany(mappedBy = "quiz")
    @OrderBy("orderIndex ASC")
    private List<QuizBankSource> bankSources;
}

