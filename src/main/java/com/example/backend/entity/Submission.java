package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "submission")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE submission SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Submission extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "assignment_id")
    private Assignment assignment;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private User student;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String description;

    @Column(name = "")
    private String instruction;

    @Column(name = "max_score")
    private Integer maxScore;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "allow_late_submission", nullable = false)
    private boolean allowLateSubmission = false;
}
