package com.example.backend.entity;

import com.example.backend.constant.ResourceSource;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import com.example.backend.constant.ResourceType;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "resource")
@SQLDelete(sql = "UPDATE resource SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Resource extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String title;
    private String fileUrl;
    private String embedUrl;
    private String cloudinaryId;
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(name = "filetype")
    private ResourceType type;
    private ResourceSource source;
    @ManyToOne
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;
}
