package com.example.backend.entity.old;

import com.example.backend.constant.ItemType;
import com.example.backend.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * Legacy chapter item entity for old course-based structure.
 * Keep only for compatibility during phased migration.
 */
@Deprecated(since = "2026-05", forRemoval = false)
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "chapter_item")
@SQLDelete(sql = "UPDATE chapter_item SET is_deleted = true WHERE id = ?")
@SQLRestriction(value = "is_deleted = false")
public class ChapterItem extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;
    @Enumerated(EnumType.STRING)
    private ItemType type;
    @Column(name = "ref_id")
    private Integer refId;
    @Column(name = "order_index")
    private Integer orderIndex;
}

