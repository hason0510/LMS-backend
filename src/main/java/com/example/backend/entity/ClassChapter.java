package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.List;

@Entity
@Table(name = "class_chapters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE class_chapters SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class ClassChapter extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "class_section_id", nullable = false)
    private ClassSection classSection;

    @ManyToOne
    @JoinColumn(name = "chapter_template_id")
    private ChapterTemplate chapterTemplate;

    @Column(name = "title_override")
    private String titleOverride;

    @Column(name = "description_override", columnDefinition = "MEDIUMTEXT")
    private String descriptionOverride;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Column(name = "is_hidden", nullable = false)
    private Boolean isHidden = false;

    @OneToMany(mappedBy = "classChapter", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<ClassContentItem> contentItems;
}
