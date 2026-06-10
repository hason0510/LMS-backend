package com.example.backend.entity;

import com.example.backend.constant.ContentItemType;
import com.example.backend.entity.assignment.Assignment;
import com.example.backend.entity.quiz.Quiz;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "class_content_items",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_class_content_items_lesson", columnNames = "lesson_id"),
                @UniqueConstraint(name = "uk_class_content_items_quiz", columnNames = "quiz_id"),
                @UniqueConstraint(name = "uk_class_content_items_assignment", columnNames = "assignment_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE class_content_items SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class ClassContentItem extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "class_chapter_id", nullable = false)
    private ClassChapter classChapter;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    private ContentItemType itemType;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Column(name = "is_hidden", nullable = false)
    private Boolean isHidden = false;

    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked = false;

    @Column(name = "available_from")
    private LocalDateTime availableFrom;

    @Column(name = "available_to")
    private LocalDateTime availableTo;

    @OneToOne
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @OneToOne
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @OneToOne
    @JoinColumn(name = "assignment_id")
    private Assignment assignment;

}
