package com.example.backend.entity.old;

import com.example.backend.constant.CourseStatus;
import com.example.backend.entity.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.util.List;

/**
 * Legacy course entity kept for backward compatibility with old endpoints.
 * New learning flow should use ClassSection/CurriculumTemplate domain models.
 */
@Deprecated(since = "2026-05", forRemoval = false)
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name= "course")
@SQLDelete(sql = "UPDATE course SET is_deleted = true WHERE id = ?")
@SQLRestriction(value = "is_deleted = false")
public class Course extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String title;
    private String description;
    private Double rating;
    @Column(name = "image_url", columnDefinition = "MEDIUMTEXT")
    private String imageUrl;
    @Column(name = "cloudinary_image_id")
    private String cloudinaryImageId;
    @OneToMany(mappedBy = "course")
    private List<Chapter> chapters;
    @OneToMany(mappedBy = "course")
    private List<Enrollment> enrollment;
    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private User teacher;
    @OneToMany(mappedBy = "course")
    private List<Meeting> meetings;
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
    private String classCode;
    @Enumerated(EnumType.STRING)
    private CourseStatus status;
}

