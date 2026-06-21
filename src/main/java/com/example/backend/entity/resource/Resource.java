package com.example.backend.entity.resource;

import com.example.backend.constant.ResourceScopeType;
import com.example.backend.constant.ResourceSource;
import com.example.backend.constant.ResourceStatus;
import com.example.backend.constant.ResourceVisibility;
import com.example.backend.entity.BaseEntity;
import com.example.backend.entity.Lesson;
import com.example.backend.entity.assignment.Assignment;
import com.example.backend.entity.assignment.Submission;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import com.example.backend.constant.ResourceType;

import java.time.LocalDateTime;

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
    private String mimeType;
    private Long fileSize;
    @Enumerated(EnumType.STRING)
    @Column(name = "filetype")
    private ResourceType type;
    private ResourceSource source;
    @Enumerated(EnumType.STRING)
    private ResourceScopeType scopeType;
    private Integer scopeId;
    @Enumerated(EnumType.STRING)
    private ResourceVisibility visibility;
    @Enumerated(EnumType.STRING)
    private ResourceStatus status;
    private Integer usageCount;
    private LocalDateTime lastUsedAt;
    @ManyToOne
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;
    @ManyToOne
    @JoinColumn(name = "assignment_id")
    private Assignment assignment;
    @ManyToOne
    @JoinColumn(name = "submission_id")
    private Submission submission;
}
