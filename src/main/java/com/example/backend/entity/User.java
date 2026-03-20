package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name= "users")
@SQLDelete(sql = "UPDATE users SET is_deleted = true WHERE user_id = ?")
@SQLRestriction(value = "is_deleted = false") //mac dinh chi lay nhung ban ghi ko bi soft delete
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer id;
    @Column(name = "user_name", unique = true)
    private String userName;
    @Column(name = "pass_word")
    private String password;
    @Column(name = "full_name")
    private String fullName;
    @Column(name = "phone_number")
    private String phoneNumber;
    @Column(name = "birthday")
    private String birthday;
    @Column(name = "student_number")
    private String studentNumber;
    @Column(name = "address")
    private String address;
    @Column(name = "refresh_token", columnDefinition = "MEDIUMTEXT")
    private String refreshToken;
    @Column(name = "gmail")
    private String gmail;
    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;
    @Column(name = "image_url", columnDefinition = "MEDIUMTEXT")
    private String imageUrl;
    @Column(name = "cloudinary_image_id")
    private String cloudinaryImageId;
    @Column(name = "work_place")
    private String workPlace;
    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;
    @Column(name = "field_of_expertise")
    private String fieldOfExpertise;
    @Column(name = "bio", columnDefinition = "LONGTEXT")
    private String bio;
    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;
    @OneToMany(mappedBy = "student")
    private List<Enrollment> enrollment;
    @OneToMany(mappedBy = "teacher")
    private List<Course> taughtCourses;
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Otp> otps = new ArrayList<>();
    @OneToMany(mappedBy = "recipient")
    private List<Notification> notifications = new ArrayList<>();


}
