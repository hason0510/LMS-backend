package com.example.backend.entity.template;

import com.example.backend.constant.ContentItemType;
import com.example.backend.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(
        name = "content_item_templates",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_content_item_templates_lesson_template", columnNames = "lesson_template_id"),
                @UniqueConstraint(name = "uk_content_item_templates_quiz_template", columnNames = "quiz_template_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE content_item_templates SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class ContentItemTemplate extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    private ContentItemType itemType;

    @Column(name = "order_index")
    private Integer orderIndex;

    @ManyToOne
    @JoinColumn(name = "chapter_template_id", nullable = false)
    private ChapterTemplate chapterTemplate;

    @OneToOne
    @JoinColumn(name = "lesson_template_id")
    private LessonTemplate lessonTemplate;

    @OneToOne
    @JoinColumn(name = "quiz_template_id")
    private QuizTemplate quizTemplate;
}
