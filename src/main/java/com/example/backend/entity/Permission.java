package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "permission")
@SQLDelete(sql = "UPDATE permission SET is_deleted = true WHERE permission_id = ?")
@SQLRestriction(value = "is_deleted = false")
public class Permission extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Integer id;
    @Column(name = "permission_name")
    private String name;
    @Column(name = "api_path")
    private String apiPath;
    @Column(name = "method")
    private String method;
    @Column(name = "permission_desc")
    private String description;
    @ManyToMany(mappedBy = "permissions")
    private List<Role> roles;
}
