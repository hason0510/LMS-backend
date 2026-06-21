package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Progress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    private User student;

    @ManyToOne
    @JoinColumn(name = "class_content_item_id")
    private ClassContentItem classContentItem;

    @Column(name = "is_completed")
    private Boolean isCompleted;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
