package com.example.backend.entity.template;

import com.example.backend.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.List;

@Entity
@Table(name = "lesson_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE lesson_templates SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class LessonTemplate extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;

    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "notes", columnDefinition = "MEDIUMTEXT")
    private String notes;

    @OneToMany(mappedBy = "lessonTemplate")
    private List<ContentItemTemplate> contentItemTemplates;
}
