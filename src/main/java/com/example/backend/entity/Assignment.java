package com.example.backend.entity;

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
@Table(name = "assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE assignments SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Assignment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String description;

    @Column(columnDefinition = "LONGTEXT")
    private String instruction;

    @Column(name = "max_score")
    private Integer maxScore;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "allow_late_submission", nullable = false)
    private boolean allowLateSubmission = false;

}
