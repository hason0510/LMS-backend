package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "quiz_attempt_answer")
@SQLDelete(sql = "UPDATE quiz_attempt_answer SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class QuizAttemptAnswer extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private LocalDateTime completedAt;
    private String textAnswer;
    private Boolean isCorrect;

    @ManyToOne
    @JoinColumn(name = "attempt_id")
    private QuizAttempt attempt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "question_id")
    private QuizQuestion question;

//    @ManyToOne
//    @JoinColumn(name = "selected_answer_id")
//    private QuizAnswer selectedAnswer;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "quiz_attempt_selected_answers",
            joinColumns = @JoinColumn(name = "attempt_answer_id"),
            inverseJoinColumns = @JoinColumn(name = "quiz_answer_id")
    )
    private List<QuizAnswer> selectedAnswers;
}
