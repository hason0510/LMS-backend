package com.example.backend.entity.quiz;

import com.example.backend.constant.AttemptStatus;
import com.example.backend.constant.GradingStatus;
import com.example.backend.entity.BaseEntity;
import com.example.backend.entity.ClassContentItem;
import com.example.backend.entity.User;
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
    @Column(name = "earned_points", precision = 10, scale = 2)
    private BigDecimal earnedPoints;
    @Column(name = "total_points", precision = 10, scale = 2)
    private BigDecimal totalPoints;
    @Column(name = "instructor_feedback", columnDefinition = "LONGTEXT")
    private String instructorFeedback;
    @Enumerated(EnumType.STRING)
    @Column(name = "grading_status", length = 20)
    private GradingStatus gradingStatus;
    @Enumerated(EnumType.STRING)
    private AttemptStatus status;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private User student;

    @Column(name = "chapter_item_id")
    private Integer chapterItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_content_item_id")
    private ClassContentItem classContentItem;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    List<QuizAttemptAnswer> attemptAnswers;



}
