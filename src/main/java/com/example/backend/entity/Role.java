package com.example.backend.entity;

import com.example.backend.constant.RoleType;
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
@Table(name="role")
@SQLDelete(sql = "UPDATE role SET is_deleted = true WHERE role_id = ?")
@SQLRestriction(value = "is_deleted = false")
public class Role extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Integer roleID;
    @Column(name = "role_name", columnDefinition = "VARCHAR(20)")
    @Enumerated(EnumType.STRING) //luu data vao DB dang String
    private RoleType roleName;
    @Column(name = "role_desc")
    private String roleDesc;
    @ManyToMany
    @JoinTable(
            name = "role_permission",
            joinColumns = @JoinColumn(name ="role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private List<Permission> permissions;
    @OneToMany(mappedBy = "role")
    private List<User> users;

}
