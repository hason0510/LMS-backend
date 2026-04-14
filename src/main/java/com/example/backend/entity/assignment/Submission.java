package com.example.backend.entity.assignment;

import com.example.backend.constant.SubmissionStatus;
import com.example.backend.entity.BaseEntity;
import com.example.backend.entity.ClassSection;
import com.example.backend.entity.Resource;
import com.example.backend.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "submission")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE submission SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Submission extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_section_id")
    private ClassSection classSection;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private User student;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String description;

    private String fileUrl;

    private String embedUrl;

    private String cloudinaryId;

    @Column(name = "submission_time")
    private LocalDateTime submissionTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubmissionStatus status = SubmissionStatus.NOT_SUBMITTED;

    @Column(name = "submission_count")
    private Integer submissionCount = 0;

    @Column(name = "grade")
    private Integer grade;

    @Column(name = "feedback", columnDefinition = "LONGTEXT")
    private String feedback;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Resource> resources = new ArrayList<>();
}
