package com.example.backend.entity.quiz;

import com.example.backend.constant.GradingStatus;
import com.example.backend.entity.User;
import com.example.backend.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.math.BigDecimal;
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
    @Column(name = "max_points", precision = 10, scale = 2)
    private BigDecimal maxPoints;
    @Column(name = "earned_points", precision = 10, scale = 2)
    private BigDecimal earnedPoints;
    @Column(name = "manual_score", precision = 10, scale = 2)
    private BigDecimal manualScore;
    @Enumerated(EnumType.STRING)
    @Column(name = "grading_status", length = 20)
    private GradingStatus gradingStatus;
    @Column(name = "teacher_feedback", columnDefinition = "LONGTEXT")
    private String teacherFeedback;
    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @ManyToOne
    @JoinColumn(name = "attempt_id")
    private QuizAttempt attempt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "attempt_question_id")
    private QuizAttemptQuestion attemptQuestion;

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

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "quiz_attempt_selected_options",
            joinColumns = @JoinColumn(name = "attempt_answer_id"),
            inverseJoinColumns = @JoinColumn(name = "attempt_question_option_id")
    )
    private List<QuizAttemptQuestionOption> selectedOptions;

    @OneToMany(mappedBy = "attemptAnswer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizAttemptAnswerItem> answerItems;
}
