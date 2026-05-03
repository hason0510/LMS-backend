package com.example.backend.entity.quiz;

import com.example.backend.entity.BaseEntity;
import com.example.backend.entity.ClassSection;
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

    @Column(name = "generate_questions_per_attempt", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean generateQuestionsPerAttempt = false;

    @Column(name = "shuffle_questions", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean shuffleQuestions = false;

    @Column(name = "shuffle_answers", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean shuffleAnswers = false;

    @Column(name = "display_mode", nullable = false, length = 20)
    private String displayMode = "PAGINATION";

    @Column(name = "show_correct_answer", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean showCorrectAnswer = false;

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

