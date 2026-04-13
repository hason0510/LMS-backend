package com.example.backend.entity.template;

import com.example.backend.constant.ContentItemType;
import com.example.backend.entity.BaseEntity;
import com.example.backend.entity.ClassContentItem;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.List;

@Entity
@Table(name = "content_item_templates")
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

    @ManyToOne
    @JoinColumn(name = "lesson_template_id")
    private LessonTemplate lessonTemplate;

    @ManyToOne
    @JoinColumn(name = "quiz_template_id")
    private QuizTemplate quizTemplate;

    @ManyToOne
    @JoinColumn(name = "assignment_template_id")
    private AssignmentTemplate assignmentTemplate;

}
