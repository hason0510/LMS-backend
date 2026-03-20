package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name= "lesson")
@SQLDelete(sql = "UPDATE lesson SET is_deleted = true WHERE id = ?")
@SQLRestriction(value = "is_deleted = false")
public class Lesson extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "title")
    private String title;
    @Column(name = "content", columnDefinition = "MEDIUMTEXT")
    private String content;
    @Column(name = "video_url")
    private String videoUrl;
    @Column(name = "notes", columnDefinition = "MEDIUMTEXT")
    private String notes;
    @OneToMany(mappedBy = "lesson")
    private List<Resource> resources;
    @OneToMany(mappedBy = "lesson")
    private List<Comment> comments;
    @Column(name = "is_finished")
    private Boolean isFinished;

}
