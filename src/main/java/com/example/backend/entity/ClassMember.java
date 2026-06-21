package com.example.backend.entity;

import com.example.backend.constant.ClassMemberRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(
        name = "class_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"class_id", "user_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassMember extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private ClassSection classSection;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClassMemberRole role;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "class_member_permissions",
            joinColumns = @JoinColumn(name = "class_member_id")
    )
    @Column(name = "permission_name", length = 50)
    private Set<String> permissions = new LinkedHashSet<>();
}
