package com.example.backend.entity;

import com.example.backend.constant.ContentItemType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "class_content_items")
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

    @ManyToOne
    @JoinColumn(name = "content_item_template_id")
    private ContentItemTemplate contentItemTemplate;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    private ContentItemType itemType;

    @Column(name = "title_override")
    private String titleOverride;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Column(name = "is_hidden", nullable = false)
    private Boolean isHidden = false;

    @ManyToOne
    @JoinColumn(name = "override_lesson_id")
    private Lesson overrideLesson;

    @ManyToOne
    @JoinColumn(name = "override_quiz_id")
    private Quiz overrideQuiz;

    @ManyToOne
    @JoinColumn(name = "override_assignment_id")
    private Assignment overrideAssignment;
}
