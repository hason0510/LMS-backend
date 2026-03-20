package com.example.backend.entity;

import com.example.backend.constant.AttemptStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "quiz_attempt")
@SQLDelete(sql = "UPDATE quiz_attempt SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class QuizAttempt extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "completed_time")
    private LocalDateTime completedTime;
    private LocalDateTime startTime;
    private Integer grade;
    @Column(name = "is_passed")
    private Boolean isPassed;
    private Integer totalQuestions;
    private Integer correctAnswers;
    private Integer incorrectAnswers;
    private Integer unansweredQuestions;
    private Integer attemptNumber;
    @Enumerated(EnumType.STRING)
    private AttemptStatus status;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_item_id")
    private ChapterItem chapterItem;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    List<QuizAttemptAnswer> attemptAnswers;



}
