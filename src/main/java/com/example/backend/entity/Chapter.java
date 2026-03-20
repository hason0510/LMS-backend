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
@Table(name= "chapter")
@SQLDelete(sql = "UPDATE chapter SET is_deleted = true WHERE id = ?")
@SQLRestriction(value = "is_deleted = false")
public class Chapter extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String title;
    @Column(name = "order_index")
    private Integer orderIndex;
    @Column(name = "description")
    private String description;
    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;
    @OneToMany(mappedBy = "chapter", cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE
    })
    @OrderBy("orderIndex ASC")
    private List<ChapterItem> chapterItems;

/*    @OneToMany(mappedBy = "chapter")
    private List<Lesson> lessons;
    @OneToMany(mappedBy = "chapter")
    private List<Quiz> quizzes;*/
}
