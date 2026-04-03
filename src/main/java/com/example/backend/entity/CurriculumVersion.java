package com.example.backend.entity;

import com.example.backend.constant.CurriculumStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.List;

@Entity
@Table(
        name = "curriculum_versions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"template_id", "version_no"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE curriculum_versions SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class CurriculumVersion extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CurriculumStatus status;

    @ManyToOne
    @JoinColumn(name = "template_id", nullable = false)
    private CurriculumTemplate template;

    @ManyToOne
    @JoinColumn(name = "based_on_version_id")
    private CurriculumVersion basedOnVersion;

    @OneToMany(mappedBy = "curriculumVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<ChapterTemplate> chapters;

    @OneToMany(mappedBy = "curriculumVersion")
    private List<ClassSection> classSections;

    @OneToMany(mappedBy = "curriculumVersion")
    private List<QuestionBank> questionBanks;
}
