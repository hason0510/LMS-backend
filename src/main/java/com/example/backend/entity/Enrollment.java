package com.example.backend.entity;

import com.example.backend.constant.EnrollmentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name= "enrollment")
@SQLDelete(sql = "UPDATE enrollment SET is_deleted = true WHERE id = ?")
@SQLRestriction(value = "is_deleted = false")
public class Enrollment extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne
    @JoinColumn(name = "student_id")
    private User student;
    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;
    private Integer progress;
    @Enumerated(EnumType.STRING)
    private EnrollmentStatus approvalStatus;

}
